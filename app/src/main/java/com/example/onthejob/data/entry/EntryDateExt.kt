package com.example.onthejob.data.entry

import java.time.LocalDate
import java.time.ZoneId

/**
 * Local calendar date for this entry, derived from createdAt. Kept for
 * audit/debug purposes only — NOT used for Calendar/Log Feed grouping
 * anymore, since createdAt is "when recorded," not "which day this
 * belongs to." Use effectiveLocalDate for anything user-facing.
 */
val Entry.localDate: LocalDate?
    get() = createdAt?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()

/**
 * The day this entry actually belongs to, for display/grouping. Prefers
 * the student-set entryDate; falls back to createdAt's local date for
 * entries saved before entryDate existed (entryDate == "").
 */
val Entry.effectiveLocalDate: LocalDate?
    get() = entryDate.takeIf { it.isNotBlank() }
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: localDate