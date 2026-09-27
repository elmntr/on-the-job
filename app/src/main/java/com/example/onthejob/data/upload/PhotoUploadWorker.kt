package com.example.onthejob.data.upload

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.onthejob.data.entry.EntryRepository
import java.io.File

/**
 * Runs in the background (survives app kill / device reboot, thanks to
 * WorkManager's own persistent queue) to finish uploading a photo that
 * failed or was offline at the time the entry was saved. Only runs when
 * NetworkType.CONNECTED is satisfied (set at enqueue time in
 * NewEntryViewModel), so it naturally waits for connectivity to return
 * rather than needing any manual "retry" tap from the student.
 *
 * Capped at MAX_ATTEMPTS so a persistently broken upload (e.g. a bad preset,
 * not just "currently offline") eventually gives up instead of retrying
 * forever under WorkManager's default backoff.
 */
class PhotoUploadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString(KEY_USER_ID) ?: return Result.failure()
        val entryId = inputData.getString(KEY_ENTRY_ID) ?: return Result.failure()
        val filePath = inputData.getString(KEY_FILE_PATH) ?: return Result.failure()

        if (runAttemptCount >= MAX_ATTEMPTS) {
            return Result.failure()
        }

        val file = File(filePath)
        if (!file.exists()) {
            // Nothing left to upload (e.g. cache was cleared) — give up, not retryable.
            return Result.failure()
        }

        val uploader = CloudinaryUploader()
        val uploadResult = uploader.upload(file, userId) { /* no progress UI in background */ }

        return uploadResult.fold(
            onSuccess = { url ->
                val appendResult = EntryRepository().appendImageUrl(userId, entryId, url)
                if (appendResult.isSuccess) {
                    file.delete()
                    Result.success()
                } else {
                    // Uploaded fine but couldn't record the URL yet (e.g. dropped
                    // connection right after) — retry the whole thing later.
                    Result.retry()
                }
            },
            onFailure = { Result.retry() },
        )
    }

    companion object {
        const val KEY_USER_ID = "userId"
        const val KEY_ENTRY_ID = "entryId"
        const val KEY_FILE_PATH = "filePath"
        private const val MAX_ATTEMPTS = 5
    }
}