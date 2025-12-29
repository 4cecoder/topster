package com.topster.tv.ui.screens

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for utility functions
 */
class ComposePlayerUtilsTest {

    @Test
    fun `formatTime should format zero correctly`() {
        assertEquals("0:00", formatTime(0))
        assertEquals("0:00", formatTime(-1))
    }

    @Test
    fun `formatTime should format minutes only correctly`() {
        assertEquals("00:30", formatTime(30000L))
        assertEquals("01:00", formatTime(60000L))
        assertEquals("05:45", formatTime(345000L))
        assertEquals("15:30", formatTime(930000L))
    }

    @Test
    fun `formatTime should format hours correctly`() {
        assertEquals("1:00:00", formatTime(3600000L))
        assertEquals("1:30:00", formatTime(5400000L))
        assertEquals("2:15:45", formatTime(8145000L))
    }

    @Test
    fun `formatTime should pad single digits`() {
        assertEquals("0:01", formatTime(1000L))
        assertEquals("0:05", formatTime(5000L))
        assertEquals("0:09", formatTime(9000L))
    }

    @Test
    fun `formatTime should handle large values`() {
        assertEquals("10:00:00", formatTime(36000000L))
        assertEquals("99:59:59", formatTime(359999000L))
    }

    @Test
    fun `formatTime should format correctly for various edge cases`() {
        // Just under 1 minute
        assertEquals("0:00:59", formatTime(59000L))

        // Exactly 1 minute
        assertEquals("0:01:00", formatTime(60000L))

        // Just under 1 hour
        assertEquals("0:59:59", formatTime(3599000L))

        // Exactly 1 hour
        assertEquals("1:00:00", formatTime(3600000L))
    }
}
