package com.auramusic.ui.screens.library

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.auramusic.R
import com.auramusic.domain.model.Album
import com.auramusic.domain.model.Playlist
import com.auramusic.domain.model.Song
import com.auramusic.ui.components.EmptyState
import com.auramusic.ui.components.NeonDivider
import com.auramusic.ui.components.SongItem
import java.io.File
import com.auramusic.ui.components.AlbumArtImage
import com.auramusic.ui.theme.*
import com.auramusic.util.getAlbumArtUri


@Composable
fun LibraryScreen(
    songs: List<Song>,
    artists: List<String>,
    albums: List<Album>,
    genres: List<String> = emptyList(),
    folders: List<String> = emptyList(),
    playlists: List<Playlist>,
    onPlaySong: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onSongMoreOptions: (Song) -> Unit = {},
    onDeleteSong: (Song) -> Unit = {},
    onNavigateToPlaylist: (Long) -> Unit,
    onNavigateToNowPlaying: (Long) -> Unit,
    onCreatePlaylist: () -> Unit,
    onImportPlaylist: () -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (Long) -> Unit = {},
    onGenreClick: (String) -> Unit = {},
    onFolderClick: (String) -> Unit = {},
    currentSongId: Long? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val songsLabel = stringResource(R.string.songs)
    val artistsLabel = stringResource(R.string.artists)
    val albumsLabel = stringResource(R.string.albums)
    val genresLabel = stringResource(R.string.genres)
    val foldersLabel = stringResource(R.string.folders)
    val playlistsLabel = stringResource(R.string.playlists)
    val tabList = remember(genres, folders, songsLabel, artistsLabel, albumsLabel, genresLabel, foldersLabel, playlistsLabel) {
        buildList {
            add(TabData(songsLabel, Icons.Rounded.MusicNote, TabType.SONGS))
            add(TabData(artistsLabel, Icons.Rounded.Person, TabType.ARTISTS))
            add(TabData(albumsLabel, Icons.Rounded.Album, TabType.ALBUMS))
            if (genres.isNotEmpty()) add(TabData(genresLabel, Icons.Rounded.Category, TabType.GENRES))
            if (folders.isNotEmpty()) add(TabData(foldersLabel, Icons.Rounded.Folder, TabType.FOLDERS))
            add(TabData(playlistsLabel, Icons.Rounded.MusicNote, TabType.PLAYLISTS))
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabList.forEachIndexed { index, tab ->
                    LibraryTabChip(
                        icon = tab.icon,
                        label = tab.label,
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "library_tab_content",
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                when (tabList[tab].type) {
                    TabType.SONGS -> SongsTab(
                        songs = songs,
                        onPlaySong = onPlaySong,
                        onToggleFavorite = onToggleFavorite,
                        onNavigateToNowPlaying = onNavigateToNowPlaying,
                        onSongMoreOptions = onSongMoreOptions,
                        onDeleteSong = onDeleteSong,
                        currentSongId = currentSongId
                    )
                    TabType.ARTISTS -> ArtistsTab(
                        artists = artists,
                        songs = songs,
                        onArtistClick = onArtistClick
                    )
                    TabType.ALBUMS -> AlbumsTab(
                        albums = albums,
                        onAlbumClick = onAlbumClick
                    )
                    TabType.GENRES -> GenresTab(
                        genres = genres,
                        onGenreClick = onGenreClick
                    )
                    TabType.FOLDERS -> FoldersTab(
                        folders = folders,
                        onFolderClick = onFolderClick
                    )
                    TabType.PLAYLISTS -> PlaylistsTab(
                        playlists = playlists,
                        onCreatePlaylist = onCreatePlaylist,
                        onImportPlaylist = onImportPlaylist,
                        onNavigateToPlaylist = onNavigateToPlaylist
                    )
                }
            }
        }
    }
}

private enum class TabType { SONGS, ARTISTS, ALBUMS, GENRES, FOLDERS, PLAYLISTS }

private data class TabData(val label: String, val icon: ImageVector, val type: TabType)

@Composable
private fun LibraryTabChip(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
private fun SongsTab(songs: List<Song>, onPlaySong: (Song) -> Unit, onToggleFavorite: (Song) -> Unit, onNavigateToNowPlaying: (Long) -> Unit, onSongMoreOptions: (Song) -> Unit = {}, onDeleteSong: (Song) -> Unit = {}, currentSongId: Long? = null) {
    if (songs.isEmpty()) {
        EmptyState(icon = { Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp)) }, title = stringResource(R.string.no_songs_found), subtitle = stringResource(R.string.empty_library))
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
            items(songs, key = { it.id }) { song ->
                SongItem(song = song, onClick = { onPlaySong(song); onNavigateToNowPlaying(song.id) }, onFavoriteToggle = { onToggleFavorite(song) }, onAddToPlaylist = { onSongMoreOptions(song) }, onDeleteSong = { onDeleteSong(song) }, isCurrentlyPlaying = song.id == currentSongId)
            }
        }
    }
}

@Composable
private fun ArtistsTab(artists: List<String>, songs: List<Song>, onArtistClick: (String) -> Unit = {}) {
    if (artists.isEmpty()) {
        EmptyState(icon = { Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp)) }, title = stringResource(R.string.no_artists_found), subtitle = stringResource(R.string.empty_library))
    } else {
        val songCountByArtist = remember(songs) {
            songs.groupingBy { it.artist }.eachCount()
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
            items(artists, key = { it }) { artist ->
                val count = songCountByArtist[artist] ?: 0
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp).clickable { onArtistClick(artist) }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(artist, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
                        Text(LocalResources.current.getQuantityString(R.plurals.song_count, count, count), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                NeonDivider()
            }
        }
    }
}

@Composable
private fun AlbumsTab(albums: List<Album>, onAlbumClick: (Long) -> Unit = {}) {
    if (albums.isEmpty()) {
        EmptyState(
            icon = { Icon(Icons.Rounded.Album, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp)) },
            title = stringResource(R.string.no_albums_found),
            subtitle = stringResource(R.string.empty_library)
        )
    } else {
        LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 160.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(albums, key = { it.id }) { album -> AlbumGridItem(album = album, onClick = { onAlbumClick(album.id) }) }
        }
    }
}

@Composable
private fun AlbumGridItem(album: Album, onClick: () -> Unit = {}) {
    val context = LocalContext.current
    val uri = remember(album.id) { context.getAlbumArtUri(album.id) }
    Column(Modifier.fillMaxWidth().clickable { onClick() }) {
        Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            AlbumArtImage(
                uri = uri,
                contentDescription = album.title,
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    Box(Modifier.fillMaxSize().background(brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Album, null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), modifier = Modifier.size(48.dp))
                    }
                }
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(album.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${album.artist}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PlaylistsTab(playlists: List<Playlist>, onCreatePlaylist: () -> Unit, onImportPlaylist: () -> Unit = {}, onNavigateToPlaylist: (Long) -> Unit) {
    if (playlists.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.no_playlists_yet), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.create_first_playlist), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onCreatePlaylist, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.create_playlist), fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(onClick = onImportPlaylist, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Rounded.FileOpen, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.import_playlist), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                FilledTonalButton(onClick = onImportPlaylist, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), modifier = Modifier.height(36.dp)) {
                    Icon(Icons.Rounded.FileOpen, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.import_playlist), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = onCreatePlaylist, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))) {
                    Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.new_playlist), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 160.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistGridItem(playlist = playlist, onClick = { onNavigateToPlaylist(playlist.id) })
                }
            }
        }
    }
}

@Composable
private fun PlaylistGridItem(playlist: Playlist, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable { onClick() }) {
        Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(brush = Brush.linearGradient(listOf(Color(playlist.color), Color(playlist.color).copy(alpha = 0.5f)))), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f), modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(playlist.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(LocalResources.current.getQuantityString(R.plurals.song_count, playlist.songCount, playlist.songCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GenresTab(genres: List<String>, onGenreClick: (String) -> Unit = {}) {
    if (genres.isEmpty()) {
        EmptyState(
            icon = { Icon(Icons.Rounded.Category, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp)) },
            title = stringResource(R.string.no_genres_found),
            subtitle = stringResource(R.string.no_genre_tags)
        )
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
            items(genres, key = { it }) { genre ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp).clickable { onGenreClick(genre) }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Category, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(genre, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
                }
                NeonDivider()
            }
        }
    }
}

@Composable
private fun FoldersTab(folders: List<String>, onFolderClick: (String) -> Unit = {}) {
    if (folders.isEmpty()) {
        EmptyState(
            icon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp)) },
            title = stringResource(R.string.no_folders_found),
            subtitle = stringResource(R.string.folders_description)
        )
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
            items(folders, key = { it }) { folder ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp).clickable { onFolderClick(folder) }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(16.dp))
                    val file = File(folder)
                    Text(file.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    Text(file.parent ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(100.dp))
                }
                NeonDivider()
            }
        }
    }
}
