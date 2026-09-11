package com.example.onthejob.data.firestore

import com.example.onthejob.data.entry.Entry
import com.example.onthejob.data.ojt.OjtInstance
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

// The document path is authoritative. Ignore any legacy `id` field written by web clients.
fun entryFromFields(documentId: String, fields: Map<String, Any?>): Entry = Entry(
    id = documentId,
    userId = fields["userId"] as? String ?: "",
    rawText = fields["rawText"] as? String ?: "",
    text = fields["text"] as? String ?: "",
    imageUrls = (fields["imageUrls"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
    hours = (fields["hours"] as? Number)?.toDouble() ?: 0.0,
    formattingStatus = fields["formattingStatus"] as? String ?: "failed_other",
    entryDate = fields["entryDate"] as? String ?: "",
    ojtInstanceId = fields["ojtInstanceId"] as? String ?: "",
    createdAt = fields["createdAt"].asDate(),
)

fun placementFromFields(documentId: String, fields: Map<String, Any?>): OjtInstance = OjtInstance(
    id = documentId,
    name = fields["name"] as? String ?: "",
    hoursRequired = (fields["hoursRequired"] as? Number)?.toDouble() ?: 486.0,
    createdAt = fields["createdAt"].asDate(),
)

private fun Any?.asDate(): Date? = when (this) {
    is Timestamp -> toDate()
    is Date -> this
    else -> null
}

fun DocumentSnapshot.toEntry(): Entry? =
    getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.let { entryFromFields(id, it) }

fun DocumentSnapshot.toPlacement(): OjtInstance? =
    getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.let { placementFromFields(id, it) }
