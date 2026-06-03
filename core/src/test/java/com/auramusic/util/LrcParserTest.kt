package com.auramusic.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun `parse valid LRC lines`() {
        val content = """
[00:12.34]First line
[01:23.45]Second line
[02:34.56]Third line
        """.trimIndent()

        val lyricData = LrcParser.parse(content)
        assertNotNull(lyricData)
        val lines = lyricData!!.lines
        assertEquals(3, lines.size)
        assertEquals("First line", lines[0].text)
        assertEquals(12340L, lines[0].timestampMs)
        assertEquals("Second line", lines[1].text)
        assertEquals(83450L, lines[1].timestampMs)
        assertEquals("Third line", lines[2].text)
        assertEquals(154560L, lines[2].timestampMs)
    }

    @Test
    fun `parse empty content`() {
        val lyricData = LrcParser.parse("")
        assertNotNull(lyricData)
        assertTrue(lyricData!!.lines.isEmpty())
    }

    @Test
    fun `parse content without timestamps returns empty lines`() {
        val content = """
Just some text
without any timestamps
        """.trimIndent()
        val lyricData = LrcParser.parse(content)
        assertNotNull(lyricData)
        assertTrue(lyricData!!.lines.isEmpty())
    }

    @Test
    fun `parse ignores untimed lines and only includes timed`() {
        val content = """
[00:05.00]Timed line
Untimed line will be ignored
        """.trimIndent()
        val lyricData = LrcParser.parse(content)
        assertNotNull(lyricData)
        val lines = lyricData!!.lines
        assertEquals(1, lines.size)
        assertEquals("Timed line", lines[0].text)
        assertEquals(5000L, lines[0].timestampMs)
    }
}
