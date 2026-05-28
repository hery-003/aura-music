package com.auramusic.domain.repository

import com.auramusic.domain.model.*
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun getAllSongs(): Flow<List<Song>>
    fun getSongById(songId: Long): Flow<Song?>
    suspend fun getSongByIdOnce(songId: Long): Song?
    fun getFavoriteSongs(): Flow<List<Song>>
    fun searchSongs(query: String): Flow<List<Song>>
    fun getAllArtists(): Flow<List<String>>
    fun getSongsByArtist(artist: String): Flow<List<Song>>
    fun getAllAlbums(): Flow<List<Album>>
    fun getSongsByAlbum(albumId: Long): Flow<List<Song>>
    fun getRecentlyPlayed(limit: Int): Flow<List<Song>>
    fun getMostPlayed(limit: Int): Flow<List<Song>>
    fun getRecentlyAdded(limit: Int): Flow<List<Song>>
    suspend fun toggleFavorite(songId: Long, isFavorite: Boolean)
    suspend fun incrementPlayCount(songId: Long)
    suspend fun scanAndInsertSongs(songs: List<Song>)
    suspend fun deleteSong(songId: Long)
    suspend fun clearAllSongs()

    fun getAllPlaylists(): Flow<List<Playlist>>
    fun getPlaylistById(id: Long): Flow<Playlist?>
    suspend fun getPlaylistByIdOnce(id: Long): Playlist?
    suspend fun createPlaylist(name: String, description: String): Long
    suspend fun updatePlaylist(playlist: Playlist)
    suspend fun deletePlaylist(id: Long)
    fun getPlaylistSongs(playlistId: Long): Flow<List<Song>>
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long)
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)
    suspend fun isSongInPlaylist(playlistId: Long, songId: Long): Boolean
    suspend fun getPlaylistSongCount(playlistId: Long): Int
    suspend fun reorderPlaylistSongs(playlistId: Long, songIds: List<Long>)
    suspend fun updatePlaylistName(id: Long, name: String, description: String)

    fun getAllGenres(): Flow<List<String>>
    fun getSongsByGenre(genre: String): Flow<List<Song>>
    fun getAllFolders(): Flow<List<String>>
    fun getSongsByPathPrefix(prefix: String): Flow<List<Song>>
    fun searchPlaylists(query: String): Flow<List<Playlist>>
    fun searchFolders(query: String): Flow<List<String>>
}
