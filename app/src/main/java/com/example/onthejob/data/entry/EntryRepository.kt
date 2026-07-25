package com.example.onthejob.data.entry

import com.example.onthejob.util.awaitTask
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class EntryRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    /**
     * Saves the entry immediately, regardless of formattingStatus — the entry
     * is never blocked or lost due to AI formatting failure (per CLAUDE.md's
     * fallback design). Returns the new document's id on success.
     *
     * Important Firestore nuance: DocumentReference.set()'s Task only completes
     * once the write is acknowledged by the SERVER — not once it's queued in
     * local cache. With offline persistence (on by default), the write IS
     * durably saved locally the moment set() is called and will sync
     * automatically once connectivity returns — but the Task itself stays
     * pending indefinitely while offline. We bound the wait and treat a
     * timeout as success, since the local write has already happened by that
     * point. A genuine failure (e.g. PERMISSION_DENIED) still throws
     * immediately and is NOT swallowed by the timeout.
     */
    suspend fun saveEntry(entry: Entry): Result<String> = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("users")
                .document(entry.userId)
                .collection("entries")
                .document()

            val task = docRef.set(entry.copy(id = docRef.id))
            withTimeoutOrNull(5_000L) { task.awaitTask() }

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Appends a single photo URL to an already-saved entry's imageUrls array.
     * Used by PhotoUploadWorker once a queued offline photo finishes
     * uploading later. Uses FieldValue.arrayUnion so multiple photos
     * finishing independently (possibly from separate WorkManager runs)
     * can't overwrite each other.
     *
     * Deliberately does NOT apply the save-time timeout trick — if this call
     * fails (still offline, etc.), we let it throw so the caller (the Worker)
     * can return Result.retry() and let WorkManager reschedule automatically,
     * rather than silently treating an unconfirmed append as done.
     */
    suspend fun appendImageUrl(userId: String, entryId: String, url: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("entries")
                    .document(entryId)
                    .update("imageUrls", FieldValue.arrayUnion(url))
                    .awaitTask()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}