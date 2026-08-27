package com.example.onthejob.data.ojt

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents a single OJT placement — e.g. one company or one academic
 * term. Stored at users/{uid}/ojtInstances/{instanceId}.
 *
 * Data-model design choice (see DECISIONS.md):
 *   Option (b) was chosen — instances live at users/{uid}/ojtInstances/{id},
 *   and entries remain at users/{uid}/entries/{entryId} with an `ojtInstanceId`
 *   foreign-key field, rather than moving entries under each instance
 *   sub-collection. This avoids a destructive migration and keeps all
 *   Firestore security rules unchanged (entries collection is already allowed).
 *
 * Migration: on first app open after this update, OjtInstanceRepository
 * auto-creates a default "OJT 1" instance and backfills existing entries
 * with its ID. Idempotent — safe to run multiple times or after interruption.
 */
data class OjtInstance(
    @DocumentId val id: String = "",
    val name: String = "",
    val hoursRequired: Double = 486.0,
    @ServerTimestamp val createdAt: Date? = null,
)
