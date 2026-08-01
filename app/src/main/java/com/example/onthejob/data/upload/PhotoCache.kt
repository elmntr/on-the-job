package com.example.onthejob.data.upload

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Copies a picked photo's bytes into app-internal storage (filesDir), giving
 * a durable local file that survives app restarts/reboots — unlike the
 * original content:// Uri from the picker, which isn't guaranteed to remain
 * readable indefinitely. This is what makes offline-first photo upload
 * possible: WorkManager can retry from this file later even if the app was
 * killed or the device rebooted in between.
 */
object PhotoCache {
    fun cache(context: Context, uri: Uri): File? {
        return try {
            val dir = File(context.filesDir, "pending_uploads").apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            file
        } catch (e: Exception) {
            null
        }
    }
}