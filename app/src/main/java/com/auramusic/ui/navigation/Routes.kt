package com.auramusic.ui.navigation

import android.net.Uri

sealed class Routes(val route: String) {
    data object Splash : Routes("splash")
    data object Home : Routes("home")
    data object Library : Routes("library")
    data object Search : Routes("search")
    data object Settings : Routes("settings")
    data object NowPlaying : Routes("now_playing/{songId}") {
        fun createRoute(songId: Long) = "now_playing/$songId"
    }
    data object PlaylistDetail : Routes("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }
    data object CreatePlaylist : Routes("create_playlist")
    data object Statistics : Routes("statistics")
    data object ArtistSongs : Routes("artist_songs/{artistName}") {
        fun createRoute(artistName: String) = "artist_songs/${Uri.encode(artistName)}"
    }
    data object AlbumSongs : Routes("album_songs/{albumId}") {
        fun createRoute(albumId: Long) = "album_songs/$albumId"
    }
    data object GenreSongs : Routes("genre_songs/{genreName}") {
        fun createRoute(genreName: String) = "genre_songs/${Uri.encode(genreName)}"
    }
    data object FolderSongs : Routes("folder_songs/{folderPath}") {
        fun createRoute(folderPath: String) = "folder_songs/${Uri.encode(folderPath)}"
    }
}
