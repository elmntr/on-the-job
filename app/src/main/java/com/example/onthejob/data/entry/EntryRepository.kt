package com.example.onthejob.data.entry

import com.example.onthejob.util.awaitTask
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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

    /**
     * Live stream of a user's entries, newest first. Uses a snapshot listener
     * (not a one-shot get()) so the feed reflects async formattingStatus
     * changes automatically — e.g. AI formatting finishing after the entry
     * was already saved, or the future Cloudflare Cron retry flipping a
     * failed_quota entry to done once quota resets. Both can happen while
     * this screen is already open.
     *
     * Firestore's offline cache means this also emits immediately from local
     * data when offline, then again when the server confirms/reconciles —
     * consistent with the offline-first behavior elsewhere in the app.
     */
    fun getEntries(userId: String): Flow<List<Entry>> = callbackFlow {
        val registration = firestore.collection("users")
            .document(userId)
            .collection("entries")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val entries = snapshot?.documents?.mapNotNull {
                    it.toObject(Entry::class.java, com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                    ?: emptyList()
                trySend(entries)
            }
        awaitClose { registration.remove() }
    }
}