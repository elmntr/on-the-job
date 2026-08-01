package com.example.onthejob.data.entry

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * A single OJT log entry. Stored at users/{uid}/entries/{entryId} in Firestore.
 *
 * `formattingStatus` mirrors the states defined in CLAUDE.md:
 *   "pending"      — not yet attempted (not currently used on write; reserved
 *                    for a future queued-retry flow after quota reset)
 *   "done"         — AI formatting succeeded, `text` holds the polished version
 *   "failed_quota" — daily Gemini quota exhausted; `text` holds the raw
 *                    description as a fallback; eligible for later re-processing
 *                    once quota resets (via the Cloudflare Cron Trigger, not yet built)
 *   "failed_other" — any other failure (rate limit exhausted after retry,
 *                    network error, unexpected error); `text` holds the raw
 *                    description as a fallback
 */
data class Entry(
    @DocumentId var id: String = "",
    val userId: String = "",
    val rawText: String = "",
    val text: String = "",
    val imageUrls: List<String> = emptyList(),
    val hours: Double = 0.0,
    val formattingStatus: String = "failed_other",
    val entryDate: String = "",
    @ServerTimestamp val createdAt: Date? = null,
)