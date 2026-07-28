package com.example.onthejob.ui.photoviewer

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Downloads the full-resolution image at [imageUrl] (the Cloudinary URL — not
 * whatever Coil has cached/downsampled for display) and inserts it into the
 * device's MediaStore under Pictures/OnTheJob, so it shows up in the system
 * gallery app.
 *
 * API 29+ only: uses scoped storage (RELATIVE_PATH), no WRITE_EXTERNAL_STORAGE
 * permission needed. Below API 29 this requires a legacy permission + insertImage
 * path not implemented here — callers should hide the save action on API < 29
 * rather than call this and surface the resulting error.
 *
 * NOTE: uses its own OkHttpClient instance. If the project already has a shared
 * client (e.g. used by CloudinaryUploader), swap this to reuse that instead —
 * flagging since I don't have visibility into that file.
 */
object PhotoSaver {

    private val client = OkHttpClient()

    sealed interface Result {
        data object Success : Result
        data class Error(val message: String) : Result
    }

    suspend fun saveToGallery(context: Context, imageUrl: String): Result = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return@withContext Result.Error("Saving photos isn't supported on this Android version.")
        }
        try {
            val request = Request.Builder().url(imageUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.Error("Couldn't download the photo.")
                }
                val body = response.body ?: return@withContext Result.Error("Couldn't download the photo.")

                val fileName = "onthejob_${System.currentTimeMillis()}.jpg"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OnTheJob")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext Result.Error("Couldn't save to gallery.")

                resolver.openOutputStream(uri)?.use { out ->
                    body.byteStream().copyTo(out)
                } ?: return@withContext Result.Error("Couldn't save to gallery.")

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)

                Result.Success
            }
        } catch (e: IOException) {
            Result.Error("Couldn't save — check your connection.")
        } catch (e: Exception) {
            Result.Error("Couldn't save the photo.")
        }
    }
}
