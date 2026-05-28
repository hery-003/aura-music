package com.auramusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auramusic.domain.model.Playlist
import com.auramusic.domain.model.Song
import androidx.compose.ui.res.stringResource
import com.auramusic.R
import com.auramusic.ui.theme.*
import com.auramusic.util.getAlbumArtUri

@Composable
fun SongItem(
    song: Song,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onAddToPlaylist: () -> Unit = {},
    onDeleteSong: () -> Unit = {},
    showArtwork: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val context = LocalContext.current
    val albumArtUri = remember(song.albumId) { context.getAlbumArtUri(song.albumId) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showArtwork) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AlbumArtImage(
                    uri = albumArtUri,
                    contentDescription = stringResource(R.string.album_art),
                    modifier = Modifier.fillMaxSize(),
                    fallback = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = song.title.firstOrNull()?.uppercase() ?: "♪",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.artistDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = song.formattedDuration,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        if (trailingContent != null) {
            trailingContent()
        } else {
            var showMenu by remember { mutableStateOf(false) }
            Row {
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(R.string.favorite),
                        tint = if (song.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.more_options),
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_to_playlist)) },
                            onClick = {
                                showMenu = false
                                onAddToPlaylist()
                            },
                            leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_song), color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDeleteSong()
                            },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        textContentColor = MaterialTheme.colorScheme.onBackground,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.PlaylistAdd, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_to_playlist), color = MaterialTheme.colorScheme.onBackground)
            }
        },
        text = {
            if (playlists.isEmpty()) {
                Text(stringResource(R.string.no_playlists_create_first), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column {
                    playlists.forEach { playlist ->
                        TextButton(
                            onClick = { onSelectPlaylist(playlist.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.QueueMusic, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = playlist.name,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}
