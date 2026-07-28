package com.example.onthejob.data.entry

import com.example.onthejob.util.awaitTask
import com.google.firebase.firestore.DocumentSnapshot
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
     * uploading later — both from New Entry's initial save AND from Entry
     * Detail's "add photo" edit flow, since both hand off pending uploads to
     * the same WorkManager job keyed by entryId. Uses FieldValue.arrayUnion
     * so multiple photos finishing independently can't overwrite each other.
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
                    it.toObject(Entry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                    ?: emptyList()
                trySend(entries)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Live stream of a single entry, for Entry Detail. A listener (not a
     * one-shot get()) for the same reason as getEntries — formattingStatus
     * can flip while the screen is open (e.g. this screen's own Regenerate
     * action, or a future Cron retry). Emits null if the document doesn't
     * exist (e.g. deleted elsewhere) rather than throwing, so the screen can
     * show a clean "not found" state instead of crashing.
     */
    fun getEntry(userId: String, entryId: String): Flow<Entry?> = callbackFlow {
        val registration = firestore.collection("users")
            .document(userId)
            .collection("entries")
            .document(entryId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(Entry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Persists edits from Entry Detail: polished text, hours, the final
     * photo URL list (after any removals + newly-uploaded additions), and
     * formattingStatus (e.g. flips to "done" if Regenerate succeeded before
     * Save was tapped). Uses the same bounded-timeout-as-success pattern as
     * saveEntry, since this is just as valid to happen offline — the local
     * write is durable immediately, only the server ack is what's timed out.
     */
    suspend fun updateEntry(
        userId: String,
        entryId: String,
        text: String,
        hours: Double,
        imageUrls: List<String>,
        formattingStatus: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val task = firestore.collection("users")
                .document(userId)
                .collection("entries")
                .document(entryId)
                .update(
                    mapOf(
                        "text" to text,
                        "hours" to hours,
                        "imageUrls" to imageUrls,
                        "formattingStatus" to formattingStatus,
                    ),
                )
            withTimeoutOrNull(5_000L) { task.awaitTask() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}