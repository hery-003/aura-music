package com.auramusic.util

import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MusicScannerTest {

    private val context: Context = mockk()
    private val contentResolver: ContentResolver = mockk()

    @Before
    fun setup() {
        every { context.contentResolver } returns contentResolver
        every { context.packageName } returns "com.auramusic"

        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk()
        every { MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) } returns mockk()
        every { MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_INTERNAL) } returns mockk()
    }

    @Test
    fun `scanner is created with context`() {
        val scanner = MusicScanner(context)
        assertTrue(scanner is MusicScanner)
    }

    @Test
    fun `scanAudioFiles handles null cursor`() {
        val externalUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        every {
            contentResolver.query(
                externalUri,
                any(),
                any(),
                any<String?>(),
                any()
            )
        } returns null

        val scanner = MusicScanner(context)
        val results = scanner.scanAudioFiles()
        assertTrue(results.isEmpty())
    }

    @Test
    fun `scanAudioFiles handles empty cursor`() {
        val cursor = MatrixCursor(
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.GENRE,
                MediaStore.Audio.Media.DATA
            )
        )

        val externalUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        every {
            contentResolver.query(
                externalUri,
                any(),
                any(),
                any<String?>(),
                any()
            )
        } returns cursor

        val scanner = MusicScanner(context)
        val results = scanner.scanAudioFiles()
        assertTrue(results.isEmpty())
    }

    @Test
    fun `scanAudioFiles parses valid song row`() {
        val cursor = MatrixCursor(
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.GENRE,
                MediaStore.Audio.Media.DATA
            )
        )
        cursor.addRow(
            arrayOf(
                1L, "Test Song", "Test Artist", "Test Album", 1L,
                200000L, 5000L, 1000L, 1001L, 1, "Rock", "/music/test.mp3"
            )
        )

        val externalUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        every {
            contentResolver.query(
                externalUri,
                any(),
                any(),
                any<String?>(),
                any()
            )
        } returns cursor

        val scanner = MusicScanner(context)
        val results = scanner.scanAudioFiles()
        assertEquals(1, results.size)
        val song = results[0]
        assertEquals("Test Song", song.title)
        assertEquals("Test Artist", song.artist)
        assertEquals("Test Album", song.album)
        assertEquals(1L, song.albumId)
        assertEquals(200000L, song.duration)
        assertEquals("Rock", song.genre)
        assertEquals("/music/test.mp3", song.path)
    }

    @Test
    fun `scanAudioFiles filters songs with zero duration`() {
        val cursor = MatrixCursor(
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.GENRE,
                MediaStore.Audio.Media.DATA
            )
        )
        cursor.addRow(arrayOf(1L, "Valid", "A", "B", 1L, 200000L, 100L, 1L, 1L, 1, "Pop", "/a.mp3"))
        cursor.addRow(arrayOf(2L, "No Duration", "A", "B", 1L, 0L, 100L, 1L, 1L, 2, "Pop", "/b.mp3"))

        val externalUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        every {
            contentResolver.query(
                externalUri,
                any(),
                any(),
                any<String?>(),
                any()
            )
        } returns cursor

        val scanner = MusicScanner(context)
        val results = scanner.scanAudioFiles()
        assertEquals(1, results.size)
        assertEquals("Valid", results[0].title)
    }
}
