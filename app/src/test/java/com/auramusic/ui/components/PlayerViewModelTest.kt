package com.auramusic.ui.components

import android.content.Intent
import com.auramusic.data.preferences.AppPreferences
import com.auramusic.domain.model.Song
import com.auramusic.domain.repository.MusicRepository
import com.auramusic.domain.usecase.ToggleFavoriteUseCase
import com.auramusic.player.MusicPlayer
import com.auramusic.player.SleepTimerManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse

class PlayerViewModelTest {

    private val repository: MusicRepository = mockk()
    private val musicPlayer: MusicPlayer = mockk()
    private val preferences: AppPreferences = mockk()
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase = mockk()
    private val context: android.content.Context = mockk()
    private val sleepTimerManager: SleepTimerManager = mockk()

    private lateinit var viewModel: PlayerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        coEvery { repository.getAllSongs().first() } returns emptyList()
        coEvery { repository.incrementPlayCount(any()) } returns Unit
        coEvery { toggleFavoriteUseCase(any(), any()) } returns Unit

        every { musicPlayer.currentSong } returns MutableStateFlow(null)
        every { musicPlayer.isPlaying } returns MutableStateFlow(false)
        every { musicPlayer.sleepTimerManager } returns sleepTimerManager
        every { musicPlayer.playSong(any()) } just runs
        every { musicPlayer.playSongNext(any()) } just runs
        every { musicPlayer.addToQueue(any()) } just runs
        every { musicPlayer.setCrossfade(any(), any()) } just runs
        every { musicPlayer.setAudioQuality(any()) } just runs

        every { sleepTimerManager.start(any()) } just runs
        every { sleepTimerManager.stop() } just runs

        every { preferences.totalListeningTime } returns MutableStateFlow(0L)
        coEvery { preferences.lastPlayedSongId.first() } returns 0L
        coEvery { preferences.lastPlayedPosition.first() } returns 0L
        coEvery { preferences.setCrossfadeEnabled(any()) } returns Unit
        coEvery { preferences.setCrossfadeDuration(any()) } returns Unit
        coEvery { preferences.setAudioQuality(any()) } returns Unit
        coEvery { preferences.setAnimationsEnabled(any()) } returns Unit

        every { context.contentResolver } returns mockk()
        every { context.startForegroundService(any()) } returns mockk()

        viewModel = PlayerViewModel(
            musicPlayer = musicPlayer,
            preferences = preferences,
            repository = repository,
            toggleFavoriteUseCase = toggleFavoriteUseCase,
            context = context
        )
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleFavorite calls use case`() = runTest {
        val song = Song(id = 1, title = "Test", isFavorite = false)
        viewModel.toggleFavorite(song)
        coVerify { toggleFavoriteUseCase(1L, true) }
    }

    @Test
    fun `toggleFavorite toggles off when already favorite`() = runTest {
        val song = Song(id = 1, title = "Test", isFavorite = true)
        viewModel.toggleFavorite(song)
        coVerify { toggleFavoriteUseCase(1L, false) }
    }

    @Test
    fun `toggleFavorite handles use case exception gracefully`() = runTest {
        coEvery { toggleFavoriteUseCase(any(), any()) } throws RuntimeException("Use case error")
        val song = Song(id = 1, title = "Test", isFavorite = false)
        viewModel.toggleFavorite(song)
        coVerify { toggleFavoriteUseCase(1L, true) }
    }

    @Test
    fun `playSong calls musicPlayer and starts service`() {
        val song = Song(id = 5, title = "Test Song")
        viewModel.playSong(song)
        verify { musicPlayer.playSong(song) }
        verify { context.startForegroundService(any()) }
    }

    @Test
    fun `playSongNext calls musicPlayer and starts service`() {
        val song = Song(id = 5, title = "Test Song")
        viewModel.playSongNext(song)
        verify { musicPlayer.playSongNext(song) }
        verify { context.startForegroundService(any()) }
    }

    @Test
    fun `addToQueue calls musicPlayer`() {
        val song = Song(id = 5, title = "Test Song")
        viewModel.addToQueue(song)
        verify { musicPlayer.addToQueue(song) }
    }

    @Test
    fun `setCrossfade saves preferences`() = runTest {
        viewModel.setCrossfade(true, 5)
        verify { musicPlayer.setCrossfade(true, 5) }
        coVerify { preferences.setCrossfadeEnabled(true) }
        coVerify { preferences.setCrossfadeDuration(5) }
    }

    @Test
    fun `startSleepTimer delegates to sleepTimerManager`() {
        viewModel.startSleepTimer(30)
        verify { sleepTimerManager.start(30) }
    }

    @Test
    fun `stopSleepTimer delegates to sleepTimerManager`() {
        viewModel.stopSleepTimer()
        verify { sleepTimerManager.stop() }
    }

    @Test
    fun `setAudioQuality saves preference and updates music player`() = runTest {
        viewModel.setAudioQuality(AppPreferences.AUDIO_QUALITY_HIGH)
        coVerify { preferences.setAudioQuality(AppPreferences.AUDIO_QUALITY_HIGH) }
        verify { musicPlayer.setAudioQuality(true) }
    }

    @Test
    fun `setAnimationsEnabled saves preference`() = runTest {
        viewModel.setAnimationsEnabled(false)
        coVerify { preferences.setAnimationsEnabled(false) }
    }

    @Test
    fun `init restores state when last song exists`() = runTest {
        val songs = listOf(
            Song(id = 1, title = "Song 1"),
            Song(id = 2, title = "Song 2")
        )
        coEvery { repository.getAllSongs().first() } returns songs
        coEvery { preferences.lastPlayedSongId.first() } returns 2L
        coEvery { preferences.lastPlayedPosition.first() } returns 15000L

        every { musicPlayer.restoreState(any(), any(), any()) } just runs

        viewModel = PlayerViewModel(
            musicPlayer = musicPlayer,
            preferences = preferences,
            repository = repository,
            toggleFavoriteUseCase = toggleFavoriteUseCase,
            context = context
        )

        verify { musicPlayer.restoreState(songs, 2L, 15000L) }
    }

    @Test
    fun `init does not restore when lastPlayedId is zero`() = runTest {
        every { musicPlayer.restoreState(any(), any(), any()) } just runs

        viewModel = PlayerViewModel(
            musicPlayer = musicPlayer,
            preferences = preferences,
            repository = repository,
            toggleFavoriteUseCase = toggleFavoriteUseCase,
            context = context
        )

        verify(exactly = 0) { musicPlayer.restoreState(any(), any(), any()) }
    }

    @Test
    fun `init handles repository exception gracefully`() = runTest {
        coEvery { repository.getAllSongs().first() } throws RuntimeException("DB error")
        every { musicPlayer.restoreState(any(), any(), any()) } just runs

        viewModel = PlayerViewModel(
            musicPlayer = musicPlayer,
            preferences = preferences,
            repository = repository,
            toggleFavoriteUseCase = toggleFavoriteUseCase,
            context = context
        )

        verify(exactly = 0) { musicPlayer.restoreState(any(), any(), any()) }
    }
}
