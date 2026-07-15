package com.auramusic.data.local.dao

import androidx.room.*
import com.auramusic.data.local.entity.PlaylistEntity
import com.auramusic.data.local.entity.PlaylistSongEntity
import com.auramusic.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchPlaylists(query: String): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun getPlaylistById(id: Long): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistByIdOnce(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: Long)

    @Query("SELECT s.* FROM songs s INNER JOIN playlist_songs ps ON s.id = ps.song_id WHERE ps.playlist_id = :playlistId ORDER BY ps.position ASC")
    fun getPlaylistSongs(playlistId: Long): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(playlistSong: PlaylistSongEntity)

    @Delete
    suspend fun removeSongFromPlaylist(playlistSong: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlist_id = :playlistId AND song_id = :songId")
    suspend fun removeSongFromPlaylistById(playlistId: Long, songId: Long)

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlist_id = :playlistId")
    suspend fun getPlaylistSongCount(playlistId: Long): Int

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_songs WHERE playlist_id = :playlistId AND song_id = :songId)")
    suspend fun isSongInPlaylist(playlistId: Long, songId: Long): Boolean

    @Query("DELETE FROM playlist_songs WHERE playlist_id = :playlistId")
    suspend fun clearPlaylist(playlistId: Long)

    @Transaction
    suspend fun deletePlaylistCascade(playlistId: Long) {
        clearPlaylist(playlistId)
        deletePlaylistById(playlistId)
    }

    @Transaction
    suspend fun reorderPlaylistSongs(playlistId: Long, songIds: List<Long>) {
        clearPlaylist(playlistId)
        songIds.forEachIndexed { index, songId ->
            addSongToPlaylist(PlaylistSongEntity(playlistId = playlistId, songId = songId, position = index))
        }
    }

    @Query("UPDATE playlists SET name = :name, description = :description WHERE id = :id")
    suspend fun updatePlaylistName(id: Long, name: String, description: String)

    @Query("SELECT p.*, COALESCE(COUNT(ps.song_id), 0) AS song_count FROM playlists p LEFT JOIN playlist_songs ps ON p.id = ps.playlist_id GROUP BY p.id ORDER BY p.name ASC")
    fun getAllPlaylistsWithCount(): Flow<List<PlaylistWithCount>>

    @Query("SELECT p.*, COALESCE(COUNT(ps.song_id), 0) AS song_count FROM playlists p LEFT JOIN playlist_songs ps ON p.id = ps.playlist_id WHERE p.id = :id GROUP BY p.id")
    fun getPlaylistByIdWithCount(id: Long): Flow<PlaylistWithCount?>

    @Query("SELECT p.*, COALESCE(COUNT(ps.song_id), 0) AS song_count FROM playlists p LEFT JOIN playlist_songs ps ON p.id = ps.playlist_id WHERE p.id = :id GROUP BY p.id")
    suspend fun getPlaylistByIdOnceWithCount(id: Long): PlaylistWithCount?

    @Query("SELECT p.*, COALESCE(COUNT(ps.song_id), 0) AS song_count FROM playlists p LEFT JOIN playlist_songs ps ON p.id = ps.playlist_id WHERE p.name LIKE '%' || :query || '%' GROUP BY p.id ORDER BY p.name ASC")
    fun searchPlaylistsWithCount(query: String): Flow<List<PlaylistWithCount>>

    @Query("DELETE FROM playlist_songs WHERE song_id IN (:songIds)")
    suspend fun removeSongsFromAllPlaylists(songIds: List<Long>)
}

data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val description: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    val color: Long,
    @ColumnInfo(name = "song_count")
    val songCount: Int
)
