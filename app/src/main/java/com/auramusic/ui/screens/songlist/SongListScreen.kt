package com.auramusic.ui.screens.songlist

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.auramusic.R
import com.auramusic.domain.model.Song
import com.auramusic.ui.components.AnimatedListItem
import com.auramusic.ui.components.EmptyState
import com.auramusic.ui.components.SongItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListScreen(
    title: String,
    subtitle: String = "",
    songs: List<Song>,
    onPlaySong: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onSongMoreOptions: (Long) -> Unit,
    onDeleteSong: (Song) -> Unit,
    onBack: () -> Unit,
    onPlayAll: () -> Unit = {},
    onShufflePlay: () -> Unit = {},
    animationsEnabled: Boolean = true
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1
                        )
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    if (songs.isNotEmpty()) {
                        IconButton(onClick = onShufflePlay) {
                            Icon(Icons.Rounded.Shuffle, stringResource(R.string.shuffle_play), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onPlayAll) {
                            Icon(Icons.Rounded.PlaylistPlay, stringResource(R.string.play_all), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (songs.isEmpty()) {
            EmptyState(
                modifier = Modifier.fillMaxSize().padding(padding),
                icon = { Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp)) },
                title = stringResource(R.string.no_songs_found),
                subtitle = stringResource(R.string.empty_library)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                item {
                    Text(
                        text = "${songs.size} ${stringResource(R.string.songs).lowercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    AnimatedListItem(index = index, animationEnabled = animationsEnabled) {
                        SongItem(
                            song = song,
                            onClick = { onPlaySong(song) },
                            onFavoriteToggle = { onToggleFavorite(song) },
                            onAddToPlaylist = { onSongMoreOptions(song.id) },
                            onDeleteSong = { onDeleteSong(song) }
                        )
                    }
                }
            }
        }
    }
}
