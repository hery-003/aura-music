package com.auramusic.data.local.dao

import androidx.room.*
import com.auramusic.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :songId")
    fun getSongById(songId: Long): Flow<SongEntity?>

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongByIdOnce(songId: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE is_favorite = 1 ORDER BY title ASC")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%'")
    fun searchSongs(query: String): Flow<List<SongEntity>>

    @Query("SELECT DISTINCT artist FROM songs ORDER BY artist ASC")
    fun getAllArtists(): Flow<List<String>>

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album ASC, track_number ASC")
    fun getSongsByArtist(artist: String): Flow<List<SongEntity>>

    @Query("SELECT DISTINCT album, artist, album_id FROM songs ORDER BY album ASC")
    fun getAllAlbums(): Flow<List<AlbumInfo>>

    @Query("SELECT * FROM songs WHERE album_id = :albumId ORDER BY track_number ASC")
    fun getSongsByAlbum(albumId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY last_played DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 20): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY play_count DESC LIMIT :limit")
    fun getMostPlayed(limit: Int = 20): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY date_added DESC LIMIT :limit")
    fun getRecentlyAdded(limit: Int = 20): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("UPDATE songs SET is_favorite = :isFavorite WHERE id = :songId")
    suspend fun updateFavorite(songId: Long, isFavorite: Boolean)

    @Query("UPDATE songs SET play_count = play_count + 1, last_played = :timestamp WHERE id = :songId")
    suspend fun incrementPlayCount(songId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSongById(songId: Long)

    @Query("DELETE FROM songs")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Query("SELECT DISTINCT genre FROM songs WHERE genre IS NOT NULL AND genre != 'Unknown' ORDER BY genre ASC")
    fun getAllGenres(): Flow<List<String>>

    @Query("SELECT * FROM songs WHERE genre = :genre ORDER BY title ASC")
    fun getSongsByGenre(genre: String): Flow<List<SongEntity>>

    @Query("SELECT COUNT(*) FROM songs WHERE genre = :genre")
    suspend fun getGenreSongCount(genre: String): Int

    @Query("SELECT path FROM songs LIMIT 1")
    suspend fun getAnyPath(): String?

    @Query("SELECT DISTINCT path FROM songs WHERE path IS NOT NULL")
    fun getAllPaths(): Flow<List<String>>

    @Query("SELECT * FROM songs WHERE path LIKE :prefix || '%'")
    fun getSongsByPathPrefix(prefix: String): Flow<List<SongEntity>>
}

data class AlbumInfo(
    val album: String,
    val artist: String,
    val album_id: Long
)
