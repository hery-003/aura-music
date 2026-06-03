package com.auramusic.data.serialization

import com.auramusic.domain.model.Playlist
import com.auramusic.domain.model.Song
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaylistSerializerTest {

    private val sampleSongs = listOf(
        Song(id = 1, title = "Song A", artist = "Artist X", album = "Album 1", duration = 200000),
        Song(id = 2, title = "Song B", artist = "Artist Y", album = "Album 2", duration = 180000)
    )

    private val playlist = Playlist(
        id = 1,
        name = "My Favorites",
        description = "My favorite songs",
        songIds = listOf(1L, 2L)
    )

    @Test
    fun `exportToJson produces valid JSON`() {
        val json = PlaylistSerializer.exportToJson(playlist, sampleSongs)
        assertTrue(json.contains("My Favorites"))
        assertTrue(json.contains("Song A"))
        assertTrue(json.contains("Artist X"))
    }

    @Test
    fun `roundtrip export then import preserves data`() {
        val json = PlaylistSerializer.exportToJson(playlist, sampleSongs)
        val imported = PlaylistSerializer.importFromJson(json)
        assertNotNull(imported)
        assertEquals(playlist.name, imported.name)
        assertEquals(playlist.description, imported.description)
        assertEquals(2, imported.songs.size)
        assertEquals("Song A", imported.songs[0].title)
        assertEquals("Artist X", imported.songs[0].artist)
        assertEquals("Album 1", imported.songs[0].album)
    }

    @Test
    fun `importFromJson handles invalid JSON`() {
        val result = PlaylistSerializer.importFromJson("not valid json")
        assertNull(result)
    }

    @Test
    fun `importFromJson handles empty JSON object`() {
        val result = PlaylistSerializer.importFromJson("{}")
        assertNull(result)
    }

    @Test
    fun `export with empty song list`() {
        val json = PlaylistSerializer.exportToJson(playlist, emptyList())
        val imported = PlaylistSerializer.importFromJson(json)
        assertNotNull(imported)
        assertTrue(imported.songs.isEmpty())
    }

    @Test
    fun `export persists song metadata correctly`() {
        val json = PlaylistSerializer.exportToJson(playlist, sampleSongs)
        val imported = PlaylistSerializer.importFromJson(json)
        assertNotNull(imported)
        val secondSong = imported.songs[1]
        assertEquals("Song B", secondSong.title)
        assertEquals("Artist Y", secondSong.artist)
        assertEquals("Album 2", secondSong.album)
        assertEquals(180000, secondSong.duration)
    }
}
