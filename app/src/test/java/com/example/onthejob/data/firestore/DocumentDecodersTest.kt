package com.example.onthejob.data.firestore

import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class DocumentDecodersTest {
    @Test fun webEntryWithStoredIdLoadsWithoutLosingFields() {
        val date = Date(1_700_000_000_000L)
        val entry = entryFromFields("path-id", mapOf(
            "id" to "conflicting-stored-id", "userId" to "student", "rawText" to "Original notes",
            "text" to "Formatted notes", "imageUrls" to listOf("https://example.com/image.jpg"),
            "hours" to 7.5, "entryDate" to "2026-09-09", "ojtInstanceId" to "placement",
            "formattingStatus" to "done", "createdAt" to Timestamp(date),
        ))
        assertEquals("path-id", entry.id)
        assertEquals("student", entry.userId)
        assertEquals("Original notes", entry.rawText)
        assertEquals("Formatted notes", entry.text)
        assertEquals(listOf("https://example.com/image.jpg"), entry.imageUrls)
        assertEquals(7.5, entry.hours, 0.0)
        assertEquals("2026-09-09", entry.entryDate)
        assertEquals("placement", entry.ojtInstanceId)
        assertEquals("done", entry.formattingStatus)
        assertEquals(date, entry.createdAt)
    }
    @Test fun nativeEntryWithoutStoredIdAndIntegerHoursLoads() {
        val entry = entryFromFields("native-id", mapOf("hours" to 8L))
        assertEquals("native-id", entry.id)
        assertEquals(8.0, entry.hours, 0.0)
        assertEquals("", entry.ojtInstanceId)
        assertNull(entry.createdAt)
    }
    @Test fun webPlacementUsesPathIdEvenWhenStoredIdIsMalformed() {
        val placement = placementFromFields("actual-id", mapOf("id" to 42, "name" to "OJT 1", "hoursRequired" to 500L))
        assertEquals("actual-id", placement.id)
        assertEquals("OJT 1", placement.name)
        assertEquals(500.0, placement.hoursRequired, 0.0)
    }
    @Test fun nativePlacementRetainsDefaultsWhenFieldsAreAbsent() {
        val placement = placementFromFields("native-id", emptyMap())
        assertEquals("native-id", placement.id)
        assertEquals(486.0, placement.hoursRequired, 0.0)
    }
}
