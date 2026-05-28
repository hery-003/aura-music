package com.auramusic.util

import org.junit.Test
import kotlin.test.assertEquals

class PlayerExtensionsTest {

    @Test
    fun `formatDuration formats zero`() {
        assertEquals("0:00", 0L.formatDuration())
    }

    @Test
    fun `formatDuration formats seconds`() {
        assertEquals("0:30", 30000L.formatDuration())
    }

    @Test
    fun `formatDuration formats minutes`() {
        assertEquals("1:00", 60000L.formatDuration())
    }

    @Test
    fun `formatDuration formats minutes and seconds`() {
        assertEquals("3:45", 225000L.formatDuration())
    }

    @Test
    fun `formatDuration formats hours`() {
        assertEquals("90:00", 5400000L.formatDuration())
    }

    @Test
    fun `formatDuration rounds down seconds`() {
        assertEquals("1:01", 61000L.formatDuration())
    }
}
