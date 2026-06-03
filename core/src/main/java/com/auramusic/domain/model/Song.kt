package com.auramusic.domain.model

data class Song(
    val id: Long = 0,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val albumId: Long = 0,
    val genre: String = "Unknown",
    val duration: Long = 0,
    val path: String = "",
    val size: Long = 0,
    val bitrate: Int = 0,
    val dateAdded: Long = 0,
    val dateModified: Long = 0,
    val trackNumber: Int = 0,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayed: Long = 0
) {
    val formattedDuration: String
        get() {
            val minutes = (duration / 1000) / 60
            val seconds = (duration / 1000) % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val artistDisplay: String
        get() = if (artist == "<unknown>" || artist.isBlank()) "Unknown Artist" else artist
}

data class Artist(
    val id: Long = 0,
    val name: String,
    val songCount: Int = 0,
    val albumCount: Int = 0
)

data class Album(
    val id: Long = 0,
    val title: String,
    val artist: String = "Unknown Artist",
    val songCount: Int = 0,
    val year: Int = 0,
    val albumArtPath: String = ""
)

data class Playlist(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val songCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val color: Long = 0xFF8B5CF6L
)

data class Genre(
    val id: Long = 0,
    val name: String,
    val songCount: Int = 0
)

data class Folder(
    val id: Long = 0,
    val name: String,
    val path: String,
    val songCount: Int = 0
)
