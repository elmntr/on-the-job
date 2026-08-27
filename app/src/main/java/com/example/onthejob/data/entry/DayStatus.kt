package com.example.onthejob.data.entry

enum class DayStatus { DONE, PENDING, NONE }

/**
 * A day counts as PENDING if any entry that day isn't yet "done" or "skipped" —
 * "skipped" means the user intentionally chose not to use AI formatting, which
 * is a terminal state (not awaiting formatting), so it does not count as pending.
 */
fun dayStatus(entriesForDay: List<Entry>?): DayStatus = when {
    entriesForDay.isNullOrEmpty() -> DayStatus.NONE
    entriesForDay.any { it.formattingStatus != "done" && it.formattingStatus != "skipped" } -> DayStatus.PENDING
    else -> DayStatus.DONE
}