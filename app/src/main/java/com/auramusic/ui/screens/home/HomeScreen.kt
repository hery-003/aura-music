package com.auramusic.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auramusic.domain.model.Album
import com.auramusic.domain.model.Playlist
import com.auramusic.domain.model.Song
import com.auramusic.ui.components.*
import com.auramusic.ui.theme.*
import androidx.compose.ui.res.stringResource
import com.auramusic.R
import com.auramusic.util.getAlbumArtUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToLibrary: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNowPlaying: (Long) -> Unit,
    onNavigateToPlaylist: (Long) -> Unit,
    onNavigateToAlbum: (Long) -> Unit = {},
    onPlaySong: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onSongMoreOptions: (Song) -> Unit = {},
    recentlyPlayed: List<Song>,
    favoriteSongs: List<Song>,
    mostPlayed: List<Song>,
    recentlyAdded: List<Song>,
    playlists: List<Playlist>,
    albums: List<Album>,
    isScanning: Boolean,
    onScan: () -> Unit,
    onCreatePlaylist: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "A",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Aura Music",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Rounded.Search, stringResource(R.string.search), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isScanning) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.scanning_your_music), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                if (recentlyPlayed.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.recently_played)) }
                    item {
                        HorizontalSongRow(
                            songs = recentlyPlayed,
                            onPlay = onPlaySong,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }

                if (favoriteSongs.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.favorites)) }
                    item {
                        HorizontalSongRow(
                            songs = favoriteSongs,
                            onPlay = onPlaySong,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }

                if (recentlyAdded.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.recently_added)) }
                    item {
                        HorizontalSongRow(
                            songs = recentlyAdded,
                            onPlay = onPlaySong,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }

                if (playlists.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.playlists),
                            actionText = stringResource(R.string.see_all),
                            onActionClick = onNavigateToLibrary
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            playlists.take(5).forEach { playlist ->
                                PlaylistCard(
                                    playlist = playlist,
                                    onClick = { onNavigateToPlaylist(playlist.id) }
                                )
                            }
                        }
                    }
                }

                if (albums.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.albums),
                            actionText = stringResource(R.string.see_all),
                            onActionClick = onNavigateToLibrary
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            albums.take(10).forEach { album ->
                                AlbumCard(album = album, onClick = { onNavigateToAlbum(album.id) })
                            }
                        }
                    }
                }

                if (mostPlayed.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.most_played)) }
                    item {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            mostPlayed.take(10).forEach { song ->
                                SongCard(song = song, onClick = { onPlaySong(song) })
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun HorizontalSongRow(
    songs: List<Song>,
    onPlay: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        songs.take(10).forEach { song ->
            SongCard(song = song, onClick = { onPlay(song) })
        }
    }
}

@Composable
fun SongCard(song: Song, onClick: () -> Unit) {
    val context = LocalContext.current
    val albumArtUri = remember(song.albumId) { context.getAlbumArtUri(song.albumId) }
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AlbumArtImage(
                uri = albumArtUri,
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = song.title.firstOrNull()?.uppercase() ?: "♪",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.play),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artistDisplay,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PlaylistCard(playlist: Playlist, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.linearGradient(
                        listOf(Color(playlist.color), Color(playlist.color).copy(alpha = 0.6f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${playlist.songCount} ${stringResource(R.string.songs).lowercase()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AlbumCard(album: Album, onClick: () -> Unit = {}) {
    val context = LocalContext.current
    val albumArtUri = remember(album.id) { context.getAlbumArtUri(album.id) }
    Column(modifier = Modifier.width(140.dp).clickable { onClick() }) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AlbumArtImage(
                uri = albumArtUri,
                contentDescription = album.title,
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Album,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
