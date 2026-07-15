package com.auramusic.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import com.auramusic.data.preferences.AppPreferences
import com.auramusic.domain.model.Song
import com.auramusic.domain.repository.MusicRepository
import com.auramusic.domain.usecase.ToggleFavoriteUseCase
import com.auramusic.player.MusicPlayer
import com.auramusic.service.MusicPlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val musicPlayer: MusicPlayer,
    private val preferences: AppPreferences,
    private val repository: MusicRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val totalListeningTime: StateFlow<Long> = preferences.totalListeningTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    init {
        musicPlayer.onSongStartedPlaying = { song ->
            viewModelScope.launch {
                try { repository.incrementPlayCount(song.id) } catch (e: Exception) { Timber.e(e, "Failed to increment play count") }
            }
        }

        viewModelScope.launch {
            try {
                val songList = withTimeoutOrNull(5000) { repository.getAllSongs().first() } ?: emptyList()
                if (songList.isNotEmpty()) {
                    val lastId = preferences.lastPlayedSongId.first()
                    val lastPos = preferences.lastPlayedPosition.first()
                    if (lastId > 0L && songList.any { it.id == lastId }) {
                        musicPlayer.restoreState(songList, lastId, lastPos)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to restore state")
            }
        }
    }

    fun playSong(song: Song) {
        try {
            musicPlayer.playSong(song)
            startPlaybackService()
        } catch (e: Exception) {
            Timber.e(e, "playSong failed")
        }
    }

    fun playSongNext(song: Song) {
        try {
            musicPlayer.playSongNext(song)
            startPlaybackService()
        } catch (e: Exception) {
            Timber.e(e, "playSongNext failed")
        }
    }

    fun addToQueue(song: Song) {
        try {
            musicPlayer.addToQueue(song)
        } catch (e: Exception) {
            Timber.e(e, "addToQueue failed")
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun startPlaybackService() {
        try {
            val intent = Intent(context, MusicPlaybackService::class.java)
            context.startForegroundService(intent)
        } catch (e: Exception) {
            Timber.e(e, "startPlaybackService failed")
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            try {
                toggleFavoriteUseCase(song.id, !song.isFavorite)
            } catch (e: Exception) {
                Timber.e(e, "toggleFavorite failed")
            }
        }
    }

    fun setCrossfade(enabled: Boolean, durationSec: Int) {
        musicPlayer.setCrossfade(enabled, durationSec)
        viewModelScope.launch {
            try {
                preferences.setCrossfadeEnabled(enabled)
                preferences.setCrossfadeDuration(durationSec)
            } catch (e: Exception) { Timber.e(e, "Failed to save crossfade prefs") }
        }
    }

    fun startSleepTimer(minutes: Int) {
        try {
            musicPlayer.sleepTimerManager.start(minutes)
        } catch (e: Exception) { Timber.e(e, "startSleepTimer failed") }
    }

    fun stopSleepTimer() {
        try {
            musicPlayer.sleepTimerManager.stop()
        } catch (e: Exception) { Timber.e(e, "stopSleepTimer failed") }
    }

    fun addTimeToSleepTimer(minutes: Int) {
        try {
            musicPlayer.sleepTimerManager.addTime(minutes)
        } catch (e: Exception) { Timber.e(e, "addTimeToSleepTimer failed") }
    }

    fun setAudioQuality(quality: Int) {
        viewModelScope.launch {
            try {
                preferences.setAudioQuality(quality)
                musicPlayer.setAudioQuality(quality == AppPreferences.AUDIO_QUALITY_HIGH)
            } catch (e: Exception) { Timber.e(e, "setAudioQuality failed") }
        }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try { preferences.setAnimationsEnabled(enabled) } catch (e: Exception) { Timber.e(e, "setAnimationsEnabled failed") }
        }
    }
}
