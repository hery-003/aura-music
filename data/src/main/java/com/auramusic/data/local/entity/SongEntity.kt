package com.auramusic.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    val id: Long = 0,
    val title: String = "",
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    @ColumnInfo(name = "album_id")
    val albumId: Long = 0,
    val genre: String = "Unknown",
    val duration: Long = 0,
    val path: String = "",
    val size: Long = 0,
    val bitrate: Int = 0,
    @ColumnInfo(name = "date_added")
    val dateAdded: Long = 0,
    @ColumnInfo(name = "date_modified")
    val dateModified: Long = 0,
    @ColumnInfo(name = "track_number")
    val trackNumber: Int = 0,
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    @ColumnInfo(name = "play_count")
    val playCount: Int = 0,
    @ColumnInfo(name = "last_played")
    val lastPlayed: Long = 0
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    val color: Long = 0xFF8B5CF6L
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlist_id", "song_id"]
)
data class PlaylistSongEntity(
    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,
    @ColumnInfo(name = "song_id")
    val songId: Long,
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis(),
    val position: Int = 0
)
