package com.auramusic.ui.components

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.content.ContentUris
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import com.auramusic.data.preferences.AppPreferences
import com.auramusic.data.serialization.PlaylistSerializer
import com.auramusic.data.serialization.ExportedPlaylist
import com.auramusic.domain.model.Playlist
import com.auramusic.domain.model.Song
import com.auramusic.domain.repository.MusicRepository
import com.auramusic.domain.usecase.*
import com.auramusic.util.MusicScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    val repository: MusicRepository,
    private val scanner: MusicScanner,
    private val getSongsByAlbumUseCase: GetSongsByAlbumUseCase,
    private val getSongsByArtistUseCase: GetSongsByArtistUseCase,
    private val scanMusicUseCase: ScanMusicUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val preferences: AppPreferences,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    val songs: StateFlow<List<Song>> = repository.getAllSongs()
        .catch { e -> Timber.e(e, "Failed to get songs"); emit(emptyList()) }
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

    val songCount: StateFlow<Int> = songs
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val artistCount: StateFlow<Int> = artists
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val albumCount: StateFlow<Int> = albums
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val searchHistory: StateFlow<List<String>> = safeFlow { preferences.searchHistory }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _addToPlaylistSongId = MutableStateFlow<Long?>(null)
    val addToPlaylistSongId: StateFlow<Long?> = _addToPlaylistSongId.asStateFlow()

    private fun <T> safeFlow(block: () -> Flow<T>): Flow<T> {
        return try {
            block()
        } catch (e: Exception) {
            Timber.e(e, "safeFlow block failed")
            emptyFlow()
        }.catch { e ->
            Timber.e(e, "safeFlow catch failed")
        }
    }

    fun scanMusic() {
        if (_isScanning.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val existingSongs = if (songs.value.isEmpty()) {
                    withTimeoutOrNull(3000) { repository.getAllSongs().first() } ?: emptyList()
                } else {
                    songs.value
                }
                val updatedSongs = scanMusicUseCase(existingSongs)
                repository.scanAndInsertSongs(updatedSongs)
            } catch (e: Exception) {
                Timber.e(e, "scanMusic failed")
            } finally {
                _isScanning.value = false
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
                Timber.e(e, "createPlaylist failed")
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            try {
                repository.deletePlaylist(playlistId)
            } catch (e: Exception) {
                Timber.e(e, "deletePlaylist failed")
                _errorMessage.value = context.getString(com.auramusic.R.string.error_delete_playlist)
            }
        }
    }

    fun updatePlaylistName(playlistId: Long, name: String, description: String) {
        viewModelScope.launch {
            try {
                repository.updatePlaylistName(playlistId, name, description)
            } catch (e: Exception) {
                Timber.e(e, "updatePlaylistName failed")
            }
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            try {
                repository.addSongToPlaylist(playlistId, songId)
            } catch (e: Exception) {
                Timber.e(e, "addSongToPlaylist failed")
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            try {
                repository.removeSongFromPlaylist(playlistId, songId)
            } catch (e: Exception) {
                Timber.e(e, "removeSongFromPlaylist failed")
                _errorMessage.value = context.getString(com.auramusic.R.string.error_remove_song)
            }
        }
    }

    fun deleteSongFromDb(songId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteSong(songId)
            } catch (e: Exception) {
                Timber.e(e, "deleteSongFromDb failed")
                _errorMessage.value = context.getString(com.auramusic.R.string.error_delete_song)
            }
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var deleted = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        } else {
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        }
                        val uri = ContentUris.withAppendedId(collection, song.id)
                        deleted = context.contentResolver.delete(uri, null, null) > 0
                    } catch (e: Exception) {
                        Timber.e(e, "MediaStore delete failed")
                    }
                }
                if (!deleted) {
                    val file = java.io.File(song.path)
                    if (file.exists()) {
                        deleted = file.delete()
                    }
                }
                if (deleted) {
                    repository.deleteSong(song.id)
                } else {
                    Timber.w("Could not delete file for song: ${song.id}, removing from DB anyway")
                    repository.deleteSong(song.id)
                }
            } catch (e: Exception) {
                Timber.e(e, "deleteSong failed")
                _errorMessage.value = context.getString(com.auramusic.R.string.error_delete_song)
            }
        }
    }

    fun reorderPlaylistSongs(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch {
            try {
                repository.reorderPlaylistSongs(playlistId, songIds)
            } catch (e: Exception) {
                Timber.e(e, "reorderPlaylistSongs failed")
            }
        }
    }

    fun searchSongs(query: String): Flow<List<Song>> {
        viewModelScope.launch {
            try { preferences.addSearchQuery(query) } catch (e: Exception) { Timber.e(e, "addSearchQuery failed") }
        }
        return try {
            repository.searchSongs(query).catch { e ->
                Timber.e(e, "searchSongs flow failed")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Timber.e(e, "searchSongs failed")
            emptyFlow()
        }
    }

    fun searchPlaylists(query: String): Flow<List<Playlist>> {
        viewModelScope.launch {
            try { preferences.addSearchQuery(query) } catch (e: Exception) { Timber.e(e, "addSearchQuery failed") }
        }
        return try {
            repository.searchPlaylists(query).catch { e ->
                Timber.e(e, "searchPlaylists flow failed")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Timber.e(e, "searchPlaylists failed")
            emptyFlow()
        }
    }

    fun searchFolders(query: String): Flow<List<String>> {
        viewModelScope.launch {
            try { preferences.addSearchQuery(query) } catch (e: Exception) { Timber.e(e, "addSearchQuery failed") }
        }
        return try {
            repository.searchFolders(query).catch { e ->
                Timber.e(e, "searchFolders flow failed")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Timber.e(e, "searchFolders failed")
            emptyFlow()
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            try { preferences.clearSearchHistory() } catch (e: Exception) { Timber.e(e, "clearSearchHistory failed") }
        }
    }

    fun exportPlaylist(context: Context, playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val playlist = repository.getPlaylistByIdOnce(playlistId) ?: return@launch
                val songs = repository.getPlaylistSongs(playlistId).first()
                val json = PlaylistSerializer.exportToJson(playlist, songs)
                val file = java.io.File(
                    context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS),
                    "${playlist.name}.json"
                )
                file.writeText(json)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    ))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, context.getString(com.auramusic.R.string.export_playlist)))
            } catch (e: Exception) {
                Timber.e(e, "exportPlaylist failed")
                _errorMessage.value = context.getString(com.auramusic.R.string.error_export_playlist)
            }
        }
    }

    fun importPlaylist(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().readText()
                } ?: return@launch
                val exported = PlaylistSerializer.importFromJson(json) ?: run {
                    _errorMessage.value = context.getString(com.auramusic.R.string.error_import_playlist)
                    return@launch
                }
                val playlistId = repository.createPlaylist(exported.name, exported.description)
                val allSongs = repository.getAllSongs().first()
                exported.songs.forEach { info ->
                    val match = allSongs.find { song ->
                        song.title.equals(info.title, ignoreCase = true) &&
                        song.artist.equals(info.artist, ignoreCase = true) &&
                        song.album.equals(info.album, ignoreCase = true)
                    }
                    if (match != null) {
                        repository.addSongToPlaylist(playlistId, match.id)
                    }
                }
                _errorMessage.value = context.getString(com.auramusic.R.string.playlist_imported)
            } catch (e: Exception) {
                Timber.e(e, "importPlaylist failed")
                _errorMessage.value = context.getString(com.auramusic.R.string.error_import_playlist)
            }
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
            } catch (e: Exception) { Timber.e(e, "confirmAddToPlaylist failed") }
        }
    }
}
