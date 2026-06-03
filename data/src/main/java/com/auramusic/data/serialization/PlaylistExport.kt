package com.auramusic.data.serialization

import com.auramusic.domain.model.Playlist
import com.auramusic.domain.model.Song
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ExportedPlaylist(
    val name: String,
    val description: String,
    val songs: List<ExportedSongInfo>
)

@Serializable
data class ExportedSongInfo(
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long
)

object PlaylistSerializer {

    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun exportToJson(playlist: Playlist, songs: List<Song>): String {
        val exported = ExportedPlaylist(
            name = playlist.name,
            description = playlist.description,
            songs = songs.map { song ->
                ExportedSongInfo(
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    duration = song.duration
                )
            }
        )
        return json.encodeToString(exported)
    }

    fun importFromJson(jsonString: String): ExportedPlaylist? {
        return try {
            json.decodeFromString<ExportedPlaylist>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
}
