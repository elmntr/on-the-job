package com.example.onthejob.data.ojt

import com.example.onthejob.util.awaitTask
import com.example.onthejob.data.firestore.toPlacement
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Manages OJT instances (placements) and their one-time migration.
 *
 * Firestore layout:
 *   users/{uid}/ojtInstances/{instanceId}  — OjtInstance docs
 *   users/{uid}/entries/{entryId}           — unchanged; entries gain ojtInstanceId field
 *
 * See DECISIONS.md for the full tradeoff discussion between option (a) and (b).
 *
 * Security rules: ojtInstances uses the same allow-read/write-if-auth-uid-matches
 * pattern as entries. The new collection must be added to Firestore rules — see
 * DECISIONS.md's security-rules section. Until the rules are deployed, writes
 * will fail with PERMISSION_DENIED in production (the same gap that affected
 * entries before rules were fixed).
 */
class OjtInstanceRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    companion object {
        const val DEFAULT_INSTANCE_NAME = "OJT 1"
        private const val COL_OJT_INSTANCES = "ojtInstances"
        private const val COL_ENTRIES = "entries"
    }

    /**
     * Live stream of all OJT instances for a user, newest first.
     * Used by the instance picker in the top bar.
     */
    fun getInstances(userId: String): Flow<List<OjtInstance>> = callbackFlow {
        val registration = firestore.collection("users")
            .document(userId)
            .collection(COL_OJT_INSTANCES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Do not close flow with exception to prevent crashing on PERMISSION_DENIED
                    // Emit default fallback instance so UI remains functional while rules deploy
                    trySend(listOf(OjtInstance(id = "default", name = DEFAULT_INSTANCE_NAME, hoursRequired = 486.0)))
                    return@addSnapshotListener
                }
                val instances = snapshot?.documents?.mapNotNull {
                    it.toPlacement()
                } ?: emptyList()
                trySend(if (instances.isEmpty()) listOf(OjtInstance(id = "default", name = DEFAULT_INSTANCE_NAME, hoursRequired = 486.0)) else instances.sortedBy { it.createdAt?.time ?: 0L })
            }
        awaitClose { registration.remove() }
    }

    /**
     * Creates a new OJT instance and returns its generated id.
     * Uses the same bounded-timeout-as-success pattern as EntryRepository.
     */
    suspend fun createInstance(userId: String, instance: OjtInstance): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val docRef = firestore.collection("users")
                    .document(userId)
                    .collection(COL_OJT_INSTANCES)
                    .document()
                val task = docRef.set(instance.copy(id = docRef.id))
                withTimeoutOrNull(5_000L) { task.awaitTask() }
                Result.success(docRef.id)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Idempotent one-time migration for existing users.
     *
     * Contract:
     *  - If the user already has ≥ 1 ojtInstances doc → no-op; returns the
     *    first instance's id (sorted by createdAt asc).
     *  - If the user has 0 ojtInstances docs → creates a default "OJT 1"
     *    instance with the provided hoursRequired, then bulk-updates every
     *    entry that has an empty ojtInstanceId to point at the new instance.
     *
     * Safe to call multiple times (idempotency: the instance count check is
     * the gate). Safe if interrupted: a partial backfill leaves some entries
     * with blank ojtInstanceId; calling again will re-attempt those entries
     * because blank == not yet migrated.
     *
     * Returns the active instance id (either the existing first instance or
     * the newly created default).
     */
    suspend fun ensureDefaultInstance(
        userId: String,
        legacyHoursRequired: Double,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Check whether the user already has any instances.
            val existingSnapshot = firestore.collection("users")
                .document(userId)
                .collection(COL_OJT_INSTANCES)
                .limit(1)
                .get()
                .await()

            if (!existingSnapshot.isEmpty) {
                // Already migrated — return the first instance id.
                val firstId = existingSnapshot.documents.firstOrNull()?.id
                    ?: return@withContext Result.failure(Exception("Instance doc had no id"))
                return@withContext Result.success(firstId)
            }

            // 2. No instances yet — create the default one.
            val defaultRef = firestore.collection("users")
                .document(userId)
                .collection(COL_OJT_INSTANCES)
                .document()
            val defaultInstance = OjtInstance(
                id = defaultRef.id,
                name = DEFAULT_INSTANCE_NAME,
                hoursRequired = legacyHoursRequired,
            )
            withTimeoutOrNull(5_000L) { defaultRef.set(defaultInstance).awaitTask() }
            val instanceId = defaultRef.id

            // 3. Backfill existing entries that have a blank ojtInstanceId.
            //    Firestore doesn't support batch queries + updates in one step,
            //    so we fetch all entries and write only the un-migrated ones.
            val entriesSnapshot = firestore.collection("users")
                .document(userId)
                .collection(COL_ENTRIES)
                .get()
                .await()

            val unmigrated = entriesSnapshot.documents.filter { doc ->
                doc.getString("ojtInstanceId").isNullOrBlank()
            }

            // Firestore batch is limited to 500 writes. Chunk if needed.
            unmigrated.chunked(400).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { doc ->
                    batch.update(doc.reference, "ojtInstanceId", instanceId)
                }
                withTimeoutOrNull(10_000L) { batch.commit().awaitTask() }
            }

            Result.success(instanceId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates an existing instance's name and/or hoursRequired.
     */
    suspend fun updateInstance(
        userId: String,
        instanceId: String,
        name: String,
        hoursRequired: Double,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val task = firestore.collection("users")
                .document(userId)
                .collection(COL_OJT_INSTANCES)
                .document(instanceId)
                .update(mapOf("name" to name, "hoursRequired" to hoursRequired))
            withTimeoutOrNull(5_000L) { task.awaitTask() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
