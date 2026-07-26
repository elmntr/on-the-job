package com.example.onthejob.data.entry

import java.time.LocalDate
import java.time.ZoneId

/**
 * Local calendar date for this entry, derived from `createdAt` in the
 * device's default time zone (matches how the student experiences "today"
 * for clock in/out and the stamp badge — not UTC).
 */
val Entry.localDate: LocalDate?
    get() = createdAt?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()