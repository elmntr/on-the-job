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
     * from. Validate actual raster contents before sending any data.
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
            if (file.length() !in 1..10L * 1024 * 1024) {
                return@withContext Result.failure(IOException("Choose photos up to 10 MB"))
            }
            val bytes = file.readBytes()
            val format = photoFormat(bytes)
                ?: return@withContext Result.failure(IOException("Only JPEG, PNG, and WebP photos are allowed"))
            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            if (options.outWidth <= 0 || options.outHeight <= 0 || options.outWidth.toLong() * options.outHeight > 40_000_000) {
                return@withContext Result.failure(IOException("Photo is damaged or larger than 40 megapixels"))
            }
            val mediaType = "image/${if (format == "jpg") "jpeg" else format}".toMediaTypeOrNull()
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
                .addFormDataPart("file", "photo.$format", progressBody)
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