package com.auramusic.ui.components

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import com.auramusic.data.preferences.AppPreferences
import com.auramusic.domain.model.Playlist
import com.auramusic.domain.model.Song
import com.auramusic.domain.repository.MusicRepository
import com.auramusic.domain.usecase.*
import com.auramusic.player.EqualizerManager
import com.auramusic.player.MusicPlayer
import com.auramusic.service.MusicPlaybackService
import com.auramusic.util.MusicScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    val repository: MusicRepository,
    val musicPlayer: MusicPlayer,
    val preferences: AppPreferences,
    private val scanner: MusicScanner,
    private val getSongsByAlbumUseCase: GetSongsByAlbumUseCase,
    private val getSongsByArtistUseCase: GetSongsByArtistUseCase,
    private val scanMusicUseCase: ScanMusicUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    val songs: StateFlow<List<Song>> = repository.getAllSongs()
        .catch { e -> e.printStackTrace(); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = safeFlow { repository.getFavoriteSongs() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<Song>> = safeFlow { repository.getRecentlyPlayed(20) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayed: StateFlow<List<Song>> = safeFlow { repository.getMostPlayed(20) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAdded: StateFlow<List<Song>> = safeFlow { repository.getRecentlyAdded(20) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<Playlist>> = safeFlow { repository.getAllPlaylists() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<String>> = safeFlow { repository.getAllArtists() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums = safeFlow { repository.getAllAlbums() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val genres = safeFlow { repository.getAllGenres() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders = safeFlow { repository.getAllFolders() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalListeningTime: StateFlow<Long> = safeFlow { preferences.totalListeningTime }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val searchHistory: StateFlow<List<String>> = safeFlow { preferences.searchHistory }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _addToPlaylistSongId = MutableStateFlow<Long?>(null)
    val addToPlaylistSongId: StateFlow<Long?> = _addToPlaylistSongId.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val songList = repository.getAllSongs().first()
                if (songList.isNotEmpty()) {
                    val lastId = preferences.lastPlayedSongId.first()
                    val lastPos = preferences.lastPlayedPosition.first()
                    if (lastId > 0L && songList.any { it.id == lastId }) {
                        musicPlayer.restoreState(songList, lastId, lastPos)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun <T> safeFlow(block: () -> Flow<T>): Flow<T> {
        return try {
            block()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyFlow()
        }.catch { e ->
            e.printStackTrace()
        }
    }

    fun scanMusic() {
        if (_isScanning.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val existingSongs = if (songs.value.isEmpty()) {
                    repository.getAllSongs().first()
                } else {
                    songs.value
                }
                val updatedSongs = scanMusicUseCase(existingSongs)
                repository.scanAndInsertSongs(updatedSongs)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun playSong(song: Song) {
        try {
            musicPlayer.playSong(song)
            startPlaybackService()
            viewModelScope.launch {
                try { repository.incrementPlayCount(song.id) } catch (e: Exception) { e.printStackTrace() }
            }
        } catch (e: Exception) {
            android.util.Log.e("SharedViewModel", "playSong failed", e)
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun startPlaybackService() {
        try {
            val intent = Intent(context, MusicPlaybackService::class.java)
            context.startForegroundService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            try {
                toggleFavoriteUseCase(song.id, !song.isFavorite)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getSongsByAlbum(albumId: Long): Flow<List<Song>> {
        return getSongsByAlbumUseCase(albumId)
    }

    fun getSongsByArtist(artist: String): Flow<List<Song>> {
        return getSongsByArtistUseCase(artist)
    }

    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            try {
                repository.createPlaylist(name, description)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            try {
                repository.deletePlaylist(playlistId)
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = context.getString(com.auramusic.R.string.error_delete_playlist)
            }
        }
    }

    fun updatePlaylistName(playlistId: Long, name: String, description: String) {
        viewModelScope.launch {
            try {
                repository.updatePlaylistName(playlistId, name, description)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            try {
                repository.addSongToPlaylist(playlistId, songId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            try {
                repository.removeSongFromPlaylist(playlistId, songId)
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = context.getString(com.auramusic.R.string.error_remove_song)
            }
        }
    }

    fun deleteSongFromDb(songId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteSong(songId)
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = context.getString(com.auramusic.R.string.error_delete_song)
            }
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = java.io.File(song.path)
                var fileDeleted = false
                if (file.exists()) {
                    fileDeleted = file.delete()
                }
                if (!fileDeleted) {
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        song.id
                    )
                    context.contentResolver.delete(contentUri, null, null)
                }
                repository.deleteSong(song.id)
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = context.getString(com.auramusic.R.string.error_delete_song)
            }
        }
    }

    fun reorderPlaylistSongs(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch {
            try {
                repository.reorderPlaylistSongs(playlistId, songIds)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun searchSongs(query: String): Flow<List<Song>> {
        viewModelScope.launch {
            try { preferences.addSearchQuery(query) } catch (e: Exception) { e.printStackTrace() }
        }
        return try {
            repository.searchSongs(query).catch { e ->
                e.printStackTrace()
                emit(emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyFlow()
        }
    }

    fun searchPlaylists(query: String): Flow<List<Playlist>> {
        viewModelScope.launch {
            try { preferences.addSearchQuery(query) } catch (e: Exception) { e.printStackTrace() }
        }
        return try {
            repository.searchPlaylists(query).catch { e ->
                e.printStackTrace()
                emit(emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyFlow()
        }
    }

    fun searchFolders(query: String): Flow<List<String>> {
        viewModelScope.launch {
            try { preferences.addSearchQuery(query) } catch (e: Exception) { e.printStackTrace() }
        }
        return try {
            repository.searchFolders(query).catch { e ->
                e.printStackTrace()
                emit(emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyFlow()
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            try { preferences.clearSearchHistory() } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun setEqualizerPreset(preset: Int) {
        try {
            musicPlayer.equalizerManager.setPreset(preset)
        } catch (e: Exception) { e.printStackTrace() }
        viewModelScope.launch {
            try {
                preferences.setEqualizerPreset(preset)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun setCustomEqualizerBand(bandIndex: Int, levelMillibels: Short) {
        try {
            musicPlayer.equalizerManager.setBandLevel(bandIndex, levelMillibels)
        } catch (e: Exception) { e.printStackTrace() }
        viewModelScope.launch {
            try {
                preferences.setEqualizerPreset(EqualizerManager.PRESET_CUSTOM)
                val bands = musicPlayer.equalizerManager.exportCustomBands()
                preferences.setCustomEqBands(bands)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun loadCustomEqualizerBands() {
        viewModelScope.launch {
            try {
                preferences.customEqBands.first().let { csv ->
                    if (csv.isNotBlank()) {
                        musicPlayer.equalizerManager.loadCustomBands(csv)
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun setCrossfade(enabled: Boolean, durationSec: Int) {
        musicPlayer.setCrossfade(enabled, durationSec)
        viewModelScope.launch {
            try {
                preferences.setCrossfadeEnabled(enabled)
                preferences.setCrossfadeDuration(durationSec)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun startSleepTimer(minutes: Int) {
        try {
            musicPlayer.sleepTimerManager.start(minutes)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun stopSleepTimer() {
        try {
            musicPlayer.sleepTimerManager.stop()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun addTimeToSleepTimer(minutes: Int) {
        try {
            musicPlayer.sleepTimerManager.addTime(minutes)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun setAudioQuality(quality: Int) {
        viewModelScope.launch {
            try { preferences.setAudioQuality(quality) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try { preferences.setAnimationsEnabled(enabled) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun showAddToPlaylistDialog(songId: Long) {
        _addToPlaylistSongId.value = songId
    }

    fun dismissAddToPlaylistDialog() {
        _addToPlaylistSongId.value = null
    }

    fun confirmAddToPlaylist(playlistId: Long) {
        val songId = _addToPlaylistSongId.value ?: return
        _addToPlaylistSongId.value = null
        viewModelScope.launch {
            try {
                repository.addSongToPlaylist(playlistId, songId)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}
