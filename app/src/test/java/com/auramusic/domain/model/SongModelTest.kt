package com.auramusic.domain.model

import org.junit.Test
import kotlin.test.assertEquals

class SongModelTest {

    @Test
    fun `formattedDuration formats seconds correctly`() {
        val song = Song(id = 1, title = "Test", duration = 200000)
        assertEquals("3:20", song.formattedDuration)
    }

    @Test
    fun `formattedDuration handles zero`() {
        val song = Song(id = 1, title = "Test", duration = 0)
        assertEquals("0:00", song.formattedDuration)
    }

    @Test
    fun `formattedDuration handles one minute`() {
        val song = Song(id = 1, title = "Test", duration = 60000)
        assertEquals("1:00", song.formattedDuration)
    }

    @Test
    fun `formattedDuration handles long duration`() {
        val song = Song(id = 1, title = "Test", duration = 3665000)
        assertEquals("61:05", song.formattedDuration)
    }

    @Test
    fun `artistDisplay replaces unknown`() {
        val song = Song(id = 1, title = "Test", artist = "<unknown>")
        assertEquals("Unknown Artist", song.artistDisplay)
    }

    @Test
    fun `artistDisplay keeps normal name`() {
        val song = Song(id = 1, title = "Test", artist = "John Doe")
        assertEquals("John Doe", song.artistDisplay)
    }

    @Test
    fun `artistDisplay handles blank`() {
        val song = Song(id = 1, title = "Test", artist = "")
        assertEquals("Unknown Artist", song.artistDisplay)
    }

    @Test
    fun `artistDisplay handles whitespace`() {
        val song = Song(id = 1, title = "Test", artist = "   ")
        assertEquals("Unknown Artist", song.artistDisplay)
    }

    @Test
    fun `playlist has default color`() {
        val pl = Playlist(name = "Test", description = "Desc")
        assertEquals(0xFF8B5CF6L, pl.color)
    }

    @Test
    fun `playlist has default song count`() {
        val pl = Playlist(name = "Empty")
        assertEquals(0, pl.songCount)
    }

    @Test
    fun `album defaults`() {
        val album = Album(id = 1, title = "Title")
        assertEquals("Unknown Artist", album.artist)
        assertEquals(0, album.songCount)
    }

    @Test
    fun `genre model`() {
        val genre = Genre(id = 1, name = "Rock", songCount = 10)
        assertEquals("Rock", genre.name)
        assertEquals(10, genre.songCount)
    }

    @Test
    fun `folder model`() {
        val folder = Folder(id = 1, name = "Music", path = "/storage/music", songCount = 5)
        assertEquals("/storage/music", folder.path)
        assertEquals(5, folder.songCount)
    }

    @Test
    fun `artist model`() {
        val artist = Artist(id = 1, name = "Artist", songCount = 10, albumCount = 2)
        assertEquals("Artist", artist.name)
        assertEquals(2, artist.albumCount)
    }
}
