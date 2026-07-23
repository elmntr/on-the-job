package com.example.onthejob.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable data object LogFeed : Route
    @Serializable data object Calendar : Route
    @Serializable data object TimeTracking : Route
    @Serializable data object NewEntry : Route
    @Serializable data class EntryDetail(val entryId: String) : Route
    @Serializable data object PdfExport : Route

    companion object {
        val bottomNavItems: List<Route> = listOf(LogFeed, Calendar, TimeTracking)
    }
}