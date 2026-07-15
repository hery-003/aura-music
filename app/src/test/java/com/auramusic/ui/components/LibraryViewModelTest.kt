package com.auramusic.ui.components

import com.auramusic.data.preferences.AppPreferences
import com.auramusic.domain.model.Song
import com.auramusic.domain.repository.MusicRepository
import com.auramusic.domain.usecase.GetSongsByAlbumUseCase
import com.auramusic.domain.usecase.GetSongsByArtistUseCase
import com.auramusic.domain.usecase.ScanMusicUseCase
import com.auramusic.domain.usecase.ToggleFavoriteUseCase
import com.auramusic.util.MusicScanner
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryViewModelTest {

    private val repository: MusicRepository = mockk()
    private val scanner: MusicScanner = mockk()
    private val getSongsByAlbumUseCase: GetSongsByAlbumUseCase = mockk()
    private val getSongsByArtistUseCase: GetSongsByArtistUseCase = mockk()
    private val scanMusicUseCase: ScanMusicUseCase = mockk()
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase = mockk()
    private val preferences: AppPreferences = mockk()
    private val context: android.content.Context = mockk()

    private lateinit var viewModel: LibraryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        every { repository.getAllSongs() } returns MutableStateFlow(emptyList())
        every { repository.getFavoriteSongs() } returns MutableStateFlow(emptyList())
        every { repository.getRecentlyPlayed(20) } returns MutableStateFlow(emptyList())
        every { repository.getMostPlayed(20) } returns MutableStateFlow(emptyList())
        every { repository.getRecentlyAdded(20) } returns MutableStateFlow(emptyList())
        every { repository.getAllPlaylists() } returns MutableStateFlow(emptyList())
        every { repository.getAllArtists() } returns MutableStateFlow(emptyList())
        every { repository.getAllAlbums() } returns MutableStateFlow(emptyList())
        every { repository.getAllGenres() } returns MutableStateFlow(emptyList())
        every { repository.getAllFolders() } returns MutableStateFlow(emptyList())
        coEvery { repository.getAllSongs().first() } returns emptyList()
        coEvery { repository.scanAndInsertSongs(any()) } returns Unit
        coEvery { repository.createPlaylist(any(), any()) } returns 1L
        coEvery { repository.deletePlaylist(any()) } returns Unit
        coEvery { repository.updatePlaylistName(any(), any(), any()) } returns Unit
        coEvery { repository.addSongToPlaylist(any(), any()) } returns Unit
        coEvery { repository.removeSongFromPlaylist(any(), any()) } returns Unit
        coEvery { repository.deleteSong(any()) } returns Unit
        coEvery { repository.reorderPlaylistSongs(any(), any()) } returns Unit

        every { scanner.scanAudioFiles() } returns emptyList()
        coEvery { scanMusicUseCase(any()) } returns emptyList()

        every { preferences.searchHistory } returns MutableStateFlow(emptyList())
        coEvery { preferences.addSearchQuery(any()) } returns Unit
        coEvery { preferences.clearSearchHistory() } returns Unit

        every { context.contentResolver } returns mockk()

        viewModel = LibraryViewModel(
            repository = repository,
            scanner = scanner,
            getSongsByAlbumUseCase = getSongsByAlbumUseCase,
            getSongsByArtistUseCase = getSongsByArtistUseCase,
            scanMusicUseCase = scanMusicUseCase,
            toggleFavoriteUseCase = toggleFavoriteUseCase,
            preferences = preferences,
            context = context
        )
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is not scanning`() {
        assertFalse(viewModel.isScanning.value)
    }

    @Test
    fun `songs starts empty`() {
        assertTrue(viewModel.songs.value.isEmpty())
    }

    @Test
    fun `scanMusic triggers scanning`() = runTest {
        coEvery { repository.getAllSongs().first() } returns emptyList()
        every { scanner.scanAudioFiles() } returns emptyList()
        coEvery { scanMusicUseCase(any()) } returns emptyList()

        viewModel.scanMusic()

        assertFalse(viewModel.isScanning.value)
    }

    @Test
    fun `createPlaylist delegates to repository`() = runTest {
        viewModel.createPlaylist("My Playlist", "Description")
        coVerify { repository.createPlaylist("My Playlist", "Description") }
    }

    @Test
    fun `deletePlaylist delegates to repository`() = runTest {
        viewModel.deletePlaylist(1L)
        coVerify { repository.deletePlaylist(1L) }
    }

    @Test
    fun `addSongToPlaylist delegates to repository`() = runTest {
        viewModel.addSongToPlaylist(1L, 100L)
        coVerify { repository.addSongToPlaylist(1L, 100L) }
    }

    @Test
    fun `removeSongFromPlaylist delegates to repository`() = runTest {
        viewModel.removeSongFromPlaylist(1L, 100L)
        coVerify { repository.removeSongFromPlaylist(1L, 100L) }
    }

    @Test
    fun `searchSongs saves query to history`() = runTest {
        every { repository.searchSongs(any()) } returns flowOf(emptyList())
        viewModel.searchSongs("test query").first()
        coVerify { preferences.addSearchQuery("test query") }
    }

    @Test
    fun `clearSearchHistory delegates to preferences`() = runTest {
        viewModel.clearSearchHistory()
        coVerify { preferences.clearSearchHistory() }
    }

    @Test
    fun `deleteSongFromDb deletes from repository`() = runTest {
        coEvery { repository.deleteSong(1L) } returns Unit
        viewModel.deleteSongFromDb(1L)
        coVerify { repository.deleteSong(1L) }
    }

    @Test
    fun `reorderPlaylistSongs delegates to repository`() = runTest {
        viewModel.reorderPlaylistSongs(1L, listOf(1L, 2L, 3L))
        coVerify { repository.reorderPlaylistSongs(1L, listOf(1L, 2L, 3L)) }
    }

    @Test
    fun `showAddToPlaylistDialog sets song id`() {
        viewModel.showAddToPlaylistDialog(100L)
        assertEquals(100L, viewModel.addToPlaylistSongId.value)
    }

    @Test
    fun `dismissAddToPlaylistDialog clears song id`() {
        viewModel.showAddToPlaylistDialog(100L)
        viewModel.dismissAddToPlaylistDialog()
        assertEquals(null, viewModel.addToPlaylistSongId.value)
    }

    @Test
    fun `getSongsByAlbum returns flow`() = runTest {
        every { getSongsByAlbumUseCase(1L) } returns flowOf(emptyList())
        val result = viewModel.getSongsByAlbum(1L).first()
        assertEquals(0, result.size)
    }

    @Test
    fun `getSongsByArtist returns flow`() = runTest {
        every { getSongsByArtistUseCase("Artist") } returns flowOf(emptyList())
        val result = viewModel.getSongsByArtist("Artist").first()
        assertEquals(0, result.size)
    }
}
