package com.example.onthejob.data.entry

enum class DayStatus { DONE, PENDING, NONE }

/**
 * A day counts as PENDING if any entry that day isn't yet "done" — the
 * student should still see "this day needs attention" even if one of
 * several entries that day already polished successfully.
 */
fun dayStatus(entriesForDay: List<Entry>?): DayStatus = when {
    entriesForDay.isNullOrEmpty() -> DayStatus.NONE
    entriesForDay.any { it.formattingStatus != "done" } -> DayStatus.PENDING
    else -> DayStatus.DONE
}