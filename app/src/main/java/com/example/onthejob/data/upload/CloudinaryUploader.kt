package com.example.onthejob.data.upload

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import org.json.JSONObject
import java.io.IOException

class CloudinaryUploader(
    private val client: OkHttpClient = OkHttpClient(),
) {
    suspend fun upload(
        context: Context,
        uri: Uri,
        cloudName: String,
        uploadPreset: String,
        onProgress: (Float) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(IOException("Could not read photo from device"))

            val mediaType = (context.contentResolver.getType(uri) ?: "image/jpeg").toMediaTypeOrNull()

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