package com.auramusic.data.repository

import com.auramusic.data.local.dao.AlbumInfo
import com.auramusic.data.local.dao.PlaylistDao
import com.auramusic.data.local.dao.PlaylistWithCount
import com.auramusic.data.local.dao.SongDao
import com.auramusic.data.local.entity.PlaylistEntity
import com.auramusic.data.local.entity.PlaylistSongEntity
import com.auramusic.data.local.entity.SongEntity
import com.auramusic.domain.model.Album
import com.auramusic.domain.model.Playlist
import com.auramusic.domain.model.Song
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MusicRepositoryImplTest {

    private val songDao: SongDao = mockk()
    private val playlistDao: PlaylistDao = mockk()
    private lateinit var repository: MusicRepositoryImpl

    private val testSong = Song(id = 1, title = "Test Song", artist = "Test Artist", album = "Test Album", duration = 200000)
    private val testSong2 = Song(id = 2, title = "Another Song", artist = "Test Artist", album = "Best Of", duration = 180000)
    private val testEntity = SongEntity(id = 1, title = "Test Song", artist = "Test Artist", album = "Test Album", duration = 200000)
    private val testEntity2 = SongEntity(id = 2, title = "Another Song", artist = "Test Artist", album = "Best Of", duration = 180000)

    @Before
    fun setup() {
        repository = MusicRepositoryImpl(songDao, playlistDao)
    }

    @Test
    fun `getAllSongs returns songs from dao`() = runTest {
        every { songDao.getAllSongs() } returns flowOf(listOf(testEntity, testEntity2))
        val result = repository.getAllSongs().first()
        assertEquals(2, result.size)
        assertEquals("Test Song", result[0].title)
    }

    @Test
    fun `getSongById returns song when found`() = runTest {
        every { songDao.getSongById(1) } returns flowOf(testEntity)
        val result = repository.getSongById(1).first()
        assertNotNull(result)
        assertEquals("Test Song", result!!.title)
    }

    @Test
    fun `getSongById returns null when not found`() = runTest {
        every { songDao.getSongById(999) } returns flowOf(null)
        val result = repository.getSongById(999).first()
        assertEquals(null, result)
    }

    @Test
    fun `getSongByIdOnce returns song`() = runTest {
        coEvery { songDao.getSongByIdOnce(1) } returns testEntity
        val result = repository.getSongByIdOnce(1)
        assertNotNull(result)
        assertEquals("Test Song", result!!.title)
    }

    @Test
    fun `getFavoriteSongs returns only favorites`() = runTest {
        val favEntity = testEntity.copy(isFavorite = true)
        every { songDao.getFavoriteSongs() } returns flowOf(listOf(favEntity))
        val result = repository.getFavoriteSongs().first()
        assertEquals(1, result.size)
        assertTrue(result[0].isFavorite)
    }

    @Test
    fun `searchSongs returns matching results`() = runTest {
        every { songDao.searchSongs("Test") } returns flowOf(listOf(testEntity))
        val result = repository.searchSongs("Test").first()
        assertEquals(1, result.size)
        assertEquals("Test Song", result[0].title)
    }

    @Test
    fun `searchSongs returns empty when no match`() = runTest {
        every { songDao.searchSongs("XYZ") } returns flowOf(emptyList())
        val result = repository.searchSongs("XYZ").first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAllArtists returns distinct artists`() = runTest {
        every { songDao.getAllArtists() } returns flowOf(listOf("Artist A", "Artist B"))
        val result = repository.getAllArtists().first()
        assertEquals(2, result.size)
        assertEquals("Artist A", result[0])
    }

    @Test
    fun `getSongsByArtist returns songs`() = runTest {
        every { songDao.getSongsByArtist("Test Artist") } returns flowOf(listOf(testEntity, testEntity2))
        val result = repository.getSongsByArtist("Test Artist").first()
        assertEquals(2, result.size)
    }

    @Test
    fun `getAllAlbums returns albums`() = runTest {
        val albumInfo = AlbumInfo(album = "Test Album", artist = "Test Artist", album_id = 1)
        every { songDao.getAllAlbums() } returns flowOf(listOf(albumInfo))
        val result = repository.getAllAlbums().first()
        assertEquals(1, result.size)
        assertEquals("Test Album", result[0].title)
    }

    @Test
    fun `getSongsByAlbum returns songs`() = runTest {
        every { songDao.getSongsByAlbum(1) } returns flowOf(listOf(testEntity))
        val result = repository.getSongsByAlbum(1).first()
        assertEquals(1, result.size)
    }

    @Test
    fun `getRecentlyPlayed returns limited songs`() = runTest {
        every { songDao.getRecentlyPlayed(5) } returns flowOf(listOf(testEntity, testEntity2))
        val result = repository.getRecentlyPlayed(5).first()
        assertEquals(2, result.size)
    }

    @Test
    fun `getMostPlayed returns songs ordered by play count`() = runTest {
        every { songDao.getMostPlayed(10) } returns flowOf(listOf(testEntity.copy(playCount = 10), testEntity2.copy(playCount = 5)))
        val result = repository.getMostPlayed(10).first()
        assertEquals(2, result.size)
        assertEquals(10, result[0].playCount)
    }

    @Test
    fun `getRecentlyAdded returns recent songs`() = runTest {
        every { songDao.getRecentlyAdded(10) } returns flowOf(listOf(testEntity2, testEntity))
        val result = repository.getRecentlyAdded(10).first()
        assertEquals(2, result.size)
    }

    @Test
    fun `toggleFavorite calls dao`() = runTest {
        coEvery { songDao.updateFavorite(1, true) } returns Unit
        repository.toggleFavorite(1, true)
        coVerify { songDao.updateFavorite(1, true) }
    }

    @Test
    fun `incrementPlayCount calls dao`() = runTest {
        coEvery { songDao.incrementPlayCount(1, any()) } returns Unit
        repository.incrementPlayCount(1)
        coVerify { songDao.incrementPlayCount(1, any()) }
    }

    @Test
    fun `scanAndInsertSongs maps and inserts`() = runTest {
        coEvery { songDao.insertSongs(any()) } returns Unit
        repository.scanAndInsertSongs(listOf(testSong))
        coVerify { songDao.insertSongs(any()) }
    }

    @Test
    fun `scanAndInsertSongs skips empty list`() = runTest {
        repository.scanAndInsertSongs(emptyList())
        coVerify(inverse = true) { songDao.insertSongs(any()) }
    }

    @Test
    fun `deleteSong calls dao`() = runTest {
        coEvery { songDao.deleteSongById(1) } returns Unit
        repository.deleteSong(1)
        coVerify { songDao.deleteSongById(1) }
    }

    @Test
    fun `clearAllSongs calls dao`() = runTest {
        coEvery { songDao.clearAll() } returns Unit
        repository.clearAllSongs()
        coVerify { songDao.clearAll() }
    }

    @Test
    fun `createPlaylist returns new id`() = runTest {
        coEvery { playlistDao.createPlaylist(any()) } returns 42L
        val id = repository.createPlaylist("My Playlist", "Description")
        assertEquals(42L, id)
    }

    @Test
    fun `deletePlaylist calls dao`() = runTest {
        coEvery { playlistDao.deletePlaylistById(1) } returns Unit
        coEvery { playlistDao.clearPlaylist(1) } returns Unit
        repository.deletePlaylist(1)
        coVerify { playlistDao.deletePlaylistById(1) }
        coVerify { playlistDao.clearPlaylist(1) }
    }

    @Test
    fun `addSongToPlaylist creates entity with position`() = runTest {
        coEvery { playlistDao.getPlaylistSongCount(1) } returns 0
        coEvery { playlistDao.addSongToPlaylist(any()) } returns Unit
        repository.addSongToPlaylist(1, 100)
        coVerify { playlistDao.addSongToPlaylist(match { it.playlistId == 1L && it.songId == 100L && it.position == 0 }) }
    }

    @Test
    fun `removeSongFromPlaylist calls dao`() = runTest {
        coEvery { playlistDao.removeSongFromPlaylistById(1, 100) } returns Unit
        repository.removeSongFromPlaylist(1, 100)
        coVerify { playlistDao.removeSongFromPlaylistById(1, 100) }
    }

    @Test
    fun `isSongInPlaylist returns true when found`() = runTest {
        coEvery { playlistDao.isSongInPlaylist(1, 100) } returns true
        val result = repository.isSongInPlaylist(1, 100)
        assertTrue(result)
    }

    @Test
    fun `getPlaylistSongCount returns count`() = runTest {
        coEvery { playlistDao.getPlaylistSongCount(1) } returns 5
        val result = repository.getPlaylistSongCount(1)
        assertEquals(5, result)
    }

    @Test
    fun `getAllPlaylists returns playlists`() = runTest {
        val pw = PlaylistWithCount(id = 1, name = "Test", description = "", songCount = 3, createdAt = 0L, color = 0xFF8B5CF6L)
        every { playlistDao.getAllPlaylistsWithCount() } returns flowOf(listOf(pw))
        val result = repository.getAllPlaylists().first()
        assertEquals(1, result.size)
        assertEquals("Test", result[0].name)
    }

    @Test
    fun `getAllGenres returns genres`() = runTest {
        every { songDao.getAllGenres() } returns flowOf(listOf("Rock", "Pop"))
        val result = repository.getAllGenres().first()
        assertEquals(2, result.size)
    }

    @Test
    fun `getSongsByGenre returns songs`() = runTest {
        every { songDao.getSongsByGenre("Rock") } returns flowOf(listOf(testEntity))
        val result = repository.getSongsByGenre("Rock").first()
        assertEquals(1, result.size)
    }

    @Test
    fun `searchPlaylists returns matching playlists`() = runTest {
        val pw = PlaylistWithCount(id = 1, name = "My List", description = "", songCount = 2, createdAt = 0L, color = 0xFF8B5CF6L)
        every { playlistDao.searchPlaylistsWithCount("My") } returns flowOf(listOf(pw))
        val result = repository.searchPlaylists("My").first()
        assertEquals(1, result.size)
    }
}
