package com.auramusic.data.repository

import com.auramusic.data.local.dao.AlbumInfo
import com.auramusic.data.local.dao.PlaylistDao
import com.auramusic.data.local.dao.PlaylistWithCount
import com.auramusic.data.local.dao.SongDao
import com.auramusic.data.local.entity.PlaylistEntity
import com.auramusic.data.local.entity.PlaylistSongEntity
import com.auramusic.data.local.entity.SongEntity
import com.auramusic.domain.model.*
import com.auramusic.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class MusicRepositoryImpl(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao
) : MusicRepository {

    override fun getAllSongs(): Flow<List<Song>> =
        safeSongFlow { songDao.getAllSongs() }

    override fun getSongById(songId: Long): Flow<Song?> =
        try {
            songDao.getSongById(songId).map { it?.toDomain() }
        } catch (e: Exception) {
            Timber.e(e, "getSongById failed")
            emptyFlow()
        }

    override suspend fun getSongByIdOnce(songId: Long): Song? =
        try {
            songDao.getSongByIdOnce(songId)?.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "getSongByIdOnce failed")
            null
        }

    override fun getFavoriteSongs(): Flow<List<Song>> =
        safeSongFlow { songDao.getFavoriteSongs() }

    override fun searchSongs(query: String): Flow<List<Song>> =
        try {
            songDao.searchSongs(query).map { entities ->
                entities.mapNotNull { it.toDomain() }
            }
        } catch (e: Exception) {
            Timber.e(e, "searchSongs failed")
            emptyFlow()
        }

    override fun getAllArtists(): Flow<List<String>> =
        try {
            songDao.getAllArtists().catch { e ->
                Timber.e(e, "getAllArtists flow failed")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Timber.e(e, "getAllArtists failed")
            emptyFlow()
        }

    override fun getSongsByArtist(artist: String): Flow<List<Song>> =
        safeSongFlow { songDao.getSongsByArtist(artist) }

    override fun getAllAlbums(): Flow<List<Album>> =
        try {
            songDao.getAllAlbums().map { infoList ->
                infoList.mapNotNull { info ->
                    try {
                        info.toDomain()
                    } catch (e: Exception) {
                        null
                    }
                }
            }.catch { e ->
                Timber.e(e, "getAllAlbums flow failed")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Timber.e(e, "getAllAlbums failed")
            emptyFlow()
        }

    override fun getSongsByAlbum(albumId: Long): Flow<List<Song>> =
        safeSongFlow { songDao.getSongsByAlbum(albumId) }

    override fun getRecentlyPlayed(limit: Int): Flow<List<Song>> =
        safeSongFlow { songDao.getRecentlyPlayed(limit) }

    override fun getMostPlayed(limit: Int): Flow<List<Song>> =
        safeSongFlow { songDao.getMostPlayed(limit) }

    override fun getRecentlyAdded(limit: Int): Flow<List<Song>> =
        safeSongFlow { songDao.getRecentlyAdded(limit) }

    override suspend fun toggleFavorite(songId: Long, isFavorite: Boolean) {
        try {
            songDao.updateFavorite(songId, isFavorite)
        } catch (e: Exception) {
            Timber.e(e, "toggleFavorite failed")
        }
    }

    override suspend fun incrementPlayCount(songId: Long) {
        try {
            songDao.incrementPlayCount(songId)
        } catch (e: Exception) {
            Timber.e(e, "incrementPlayCount failed")
        }
    }

    override suspend fun scanAndInsertSongs(songs: List<Song>) {
        try {
            if (songs.isEmpty()) return
            songDao.insertSongs(songs.mapNotNull { it.toEntityOrNull() })
        } catch (e: Exception) {
            Timber.e(e, "scanAndInsertSongs failed")
        }
    }

    override suspend fun deleteSong(songId: Long) {
        try {
            songDao.deleteSongById(songId)
        } catch (e: Exception) {
            Timber.e(e, "deleteSong failed")
        }
    }

    override suspend fun getSongCount(): Int {
        return try {
            songDao.getSongCount()
        } catch (e: Exception) {
            Timber.e(e, "getSongCount failed")
            0
        }
    }

    override suspend fun getArtistCount(): Int {
        return try {
            songDao.getArtistCount()
        } catch (e: Exception) {
            Timber.e(e, "getArtistCount failed")
            0
        }
    }

    override suspend fun getAlbumCount(): Int {
        return try {
            songDao.getAlbumCount()
        } catch (e: Exception) {
            Timber.e(e, "getAlbumCount failed")
            0
        }
    }

    override suspend fun clearAllSongs() {
        try {
            songDao.clearAll()
        } catch (e: Exception) {
            Timber.e(e, "clearAllSongs failed")
        }
    }

    override suspend fun deleteSongsNotInIds(ids: List<Long>) {
        try {
            val existingIds = songDao.getAllSongIds()
            val toDelete = existingIds - ids.toSet()
            if (toDelete.isNotEmpty()) {
                playlistDao.removeSongsFromAllPlaylists(toDelete.toList())
                songDao.deleteSongsByIds(toDelete.toList())
            }
        } catch (e: Exception) {
            Timber.e(e, "deleteSongsNotInIds failed")
        }
    }

    override fun getAllPlaylists(): Flow<List<Playlist>> =
        try {
            playlistDao.getAllPlaylistsWithCount().map { rows ->
                rows.map { it.toDomain() }
            }.catch { e ->
                Timber.e(e, "getAllPlaylists flow failed")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Timber.e(e, "getAllPlaylists failed")
            emptyFlow()
        }

    override fun getPlaylistById(id: Long): Flow<Playlist?> =
        try {
            playlistDao.getPlaylistByIdWithCount(id).map { row ->
                row?.toDomain()
            }
        } catch (e: Exception) {
            Timber.e(e, "getPlaylistById failed")
            emptyFlow()
        }

    override suspend fun getPlaylistByIdOnce(id: Long): Playlist? =
        try {
            playlistDao.getPlaylistByIdOnceWithCount(id)?.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "getPlaylistByIdOnce failed")
            null
        }

    override suspend fun createPlaylist(name: String, description: String): Long {
        return try {
            val entity = PlaylistEntity(name = name, description = description)
            playlistDao.createPlaylist(entity)
        } catch (e: Exception) {
            Timber.e(e, "createPlaylist failed")
            -1L
        }
    }

    override suspend fun updatePlaylist(playlist: Playlist) {
        try {
            playlistDao.updatePlaylist(playlist.toEntity())
        } catch (e: Exception) {
            Timber.e(e, "updatePlaylist failed")
        }
    }

    override suspend fun deletePlaylist(id: Long) {
        try {
            playlistDao.deletePlaylistCascade(id)
        } catch (e: Exception) {
            Timber.e(e, "deletePlaylist failed")
        }
    }

    override fun getPlaylistSongs(playlistId: Long): Flow<List<Song>> =
        safeSongFlow { playlistDao.getPlaylistSongs(playlistId) }

    override suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        try {
            val count = playlistDao.getPlaylistSongCount(playlistId)
            val entity = PlaylistSongEntity(playlistId = playlistId, songId = songId, position = count)
            playlistDao.addSongToPlaylist(entity)
        } catch (e: Exception) {
            Timber.e(e, "addSongToPlaylist failed")
        }
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        try {
            playlistDao.removeSongFromPlaylistById(playlistId, songId)
        } catch (e: Exception) {
            Timber.e(e, "removeSongFromPlaylist failed")
        }
    }

    override suspend fun isSongInPlaylist(playlistId: Long, songId: Long): Boolean =
        try {
            playlistDao.isSongInPlaylist(playlistId, songId)
        } catch (e: Exception) {
            Timber.e(e, "isSongInPlaylist failed")
            false
        }

    override suspend fun getPlaylistSongCount(playlistId: Long): Int =
        try {
            playlistDao.getPlaylistSongCount(playlistId)
        } catch (e: Exception) {
            Timber.e(e, "getPlaylistSongCount failed")
            0
        }

    override suspend fun reorderPlaylistSongs(playlistId: Long, songIds: List<Long>) {
        try {
            playlistDao.reorderPlaylistSongs(playlistId, songIds)
        } catch (e: Exception) {
            Timber.e(e, "reorderPlaylistSongs failed")
        }
    }

    override suspend fun updatePlaylistName(id: Long, name: String, description: String) {
        try {
            playlistDao.updatePlaylistName(id, name, description)
        } catch (e: Exception) {
            Timber.e(e, "updatePlaylistName failed")
        }
    }

    override fun getAllGenres(): Flow<List<String>> =
        try {
            songDao.getAllGenres().catch { e ->
                Timber.e(e, "getAllGenres flow failed")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Timber.e(e, "getAllGenres failed")
            emptyFlow()
        }

    override fun getSongsByGenre(genre: String): Flow<List<Song>> =
        safeSongFlow { songDao.getSongsByGenre(genre) }

    override fun getAllFolders(): Flow<List<String>> =
        try {
            songDao.getAllPaths().map { paths ->
                paths.mapNotNull { path ->
                    try {
                        if (path.isNullOrBlank()) null
                        else {
                            val f = java.io.File(path)
                            f.parentFile?.absolutePath
                        }
                    } catch (e: Exception) { null }
                }.distinct().filterNotNull().sorted()
            }.catch { e ->
                Timber.e(e, "getAllFolders flow failed")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Timber.e(e, "getAllFolders failed")
            emptyFlow()
        }

    override fun getSongsByPathPrefix(prefix: String): Flow<List<Song>> =
        try {
            songDao.getSongsByPathPrefix(prefix).map { entities ->
                entities.mapNotNull { it.toDomain() }
            }
        } catch (e: Exception) {
            Timber.e(e, "getSongsByPathPrefix failed")
            emptyFlow()
        }

    override fun searchPlaylists(query: String): Flow<List<Playlist>> =
        try {
            playlistDao.searchPlaylistsWithCount(query).map { rows ->
                rows.map { it.toDomain() }
            }
        } catch (e: Exception) {
            Timber.e(e, "searchPlaylists failed")
            emptyFlow()
        }

    override fun searchFolders(query: String): Flow<List<String>> =
        try {
            songDao.getAllPaths().map { paths ->
                paths.mapNotNull { path ->
                    try {
                        if (path.isNullOrBlank()) null
                        else {
                            val f = java.io.File(path)
                            f.parentFile?.absolutePath
                        }
                    } catch (e: Exception) { null }
                }.distinct().filter {
                    it.contains(query, ignoreCase = true)
                }.sorted()
            }
        } catch (e: Exception) {
            Timber.e(e, "searchFolders failed")
            emptyFlow()
        }

    private fun safeSongFlow(block: () -> Flow<List<SongEntity>>): Flow<List<Song>> {
        return try {
            block().map { entities ->
                entities.mapNotNull { it.toDomain() }
            }.catch { e ->
                Timber.e(e, "safeSongFlow failed")
                emit(emptyList())
            }
        } catch (e: Exception) {
            Timber.e(e, "safeSongFlow block failed")
            emptyFlow()
        }
    }
}

private fun SongEntity.toDomain() = Song(
    id = id,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    genre = genre,
    duration = duration,
    path = path,
    size = size,
    bitrate = bitrate,
    dateAdded = dateAdded,
    dateModified = dateModified,
    trackNumber = trackNumber,
    isFavorite = isFavorite,
    playCount = playCount,
    lastPlayed = lastPlayed
)

private fun Song.toEntity() = SongEntity(
    id = id,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    genre = genre,
    duration = duration,
    path = path,
    size = size,
    bitrate = bitrate,
    dateAdded = dateAdded,
    dateModified = dateModified,
    trackNumber = trackNumber,
    isFavorite = isFavorite,
    playCount = playCount,
    lastPlayed = lastPlayed
)

private fun Song.toEntityOrNull(): SongEntity? {
    return try {
        SongEntity(
            id = id,
            title = title,
            artist = artist,
            album = album,
            albumId = albumId,
            genre = genre,
            duration = duration,
            path = path,
            size = size,
            bitrate = bitrate,
            dateAdded = dateAdded,
            dateModified = dateModified,
            trackNumber = trackNumber,
            isFavorite = isFavorite,
            playCount = playCount,
            lastPlayed = lastPlayed
        )
    } catch (e: Exception) {
        null
    }
}

private fun AlbumInfo.toDomain() = Album(
    id = album_id,
    title = album,
    artist = artist
)

private fun PlaylistWithCount.toDomain() = Playlist(
    id = id,
    name = name,
    description = description,
    songCount = songCount,
    createdAt = createdAt,
    color = color
)

private fun Playlist.toEntity() = PlaylistEntity(
    id = id,
    name = name,
    description = description,
    createdAt = createdAt,
    color = color
)
