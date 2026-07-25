package com.example.onthejob.data.upload

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class CloudinaryUploader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS) // hard ceiling on the whole upload, belt-and-suspenders
        .build(),
) {
    /**
     * Uploads from a durable local File (rather than a content:// Uri) so this
     * same method works both for the immediate in-app attempt and for a later
     * WorkManager-driven retry, which only has a stable file path to work
     * from. Content type is treated as image/jpeg (photos are always cached
     * under a .jpg name) — Cloudinary detects actual format from file
     * content regardless of the extension, so this doesn't affect correctness.
     */
    suspend fun upload(
        file: File,
        cloudName: String,
        uploadPreset: String,
        onProgress: (Float) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) {
                return@withContext Result.failure(IOException("Cached photo file no longer exists"))
            }
            val bytes = file.readBytes()
            val mediaType = "image/jpeg".toMediaTypeOrNull()
            val progressBody = object : RequestBody() {
                override fun contentType() = mediaType
                override fun contentLength() = bytes.size.toLong()
                override fun writeTo(sink: BufferedSink) {
                    val chunkSize = 8192
                    var offset = 0
                    while (offset < bytes.size) {
                        val end = minOf(offset + chunkSize, bytes.size)
                        sink.write(bytes, offset, end - offset)
                        offset = end
                        onProgress(offset.toFloat() / bytes.size)
                    }
                }
            }
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart("file", "photo.jpg", progressBody)
                .build()
            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                .post(multipart)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Upload failed: HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(IOException("Empty response"))
                val url = JSONObject(body).getString("secure_url")
                Result.success(url)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}