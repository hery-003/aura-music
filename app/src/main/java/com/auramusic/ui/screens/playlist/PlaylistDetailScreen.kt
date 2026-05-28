package com.auramusic.ui.screens.playlist

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.auramusic.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.domain.model.Song
import com.auramusic.ui.components.SongItem
import com.auramusic.ui.theme.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playlistName: String,
    playlistDescription: String = "",
    songs: List<Song>,
    onPlaySong: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onRemoveSong: (Long) -> Unit,
    onDeleteSong: (Song) -> Unit = {},
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit = onPlayAll,
    onBack: () -> Unit,
    onDeletePlaylist: () -> Unit,
    onReorderSongs: (List<Long>) -> Unit = {},
    onEditPlaylist: (Long, String, String) -> Unit = { _, _, _ -> }
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(playlistName) }
    var editDescription by remember { mutableStateOf(playlistDescription) }
    var isReordering by remember { mutableStateOf(false) }
    var songToRemoveId by remember { mutableStateOf<Long?>(null) }

    songToRemoveId?.let { songId ->
        val song = songs.find { it.id == songId }
        if (song != null) {
            AlertDialog(
                onDismissRequest = { songToRemoveId = null },
                containerColor = MaterialTheme.colorScheme.background,
                title = { Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.onBackground) },
                text = { Text(stringResource(R.string.remove_song_confirm, song.title), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton = {
                    TextButton(onClick = {
                        songToRemoveId = null
                        onRemoveSong(songId)
                    }) {
                        Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { songToRemoveId = null }) {
                        Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_playlist_title), color = MaterialTheme.colorScheme.onBackground) },
            text = { Text(stringResource(R.string.delete_playlist_confirm, playlistName), color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeletePlaylist()
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.edit_playlist), color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(stringResource(R.string.name), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text(stringResource(R.string.description_optional), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editName.isNotBlank()) {
                        onEditPlaylist(playlistId, editName, editDescription)
                        showEditDialog = false
                    }
                }) {
                    Text(stringResource(R.string.save), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlistName, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editName = playlistName
                        editDescription = playlistDescription
                        showEditDialog = true
                    }) {
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit_playlist), tint = MaterialTheme.colorScheme.onBackground)
                    }
                    if (songs.size > 1) {
                        IconButton(onClick = { isReordering = !isReordering }) {
                            Icon(
                                if (isReordering) Icons.Rounded.Close else Icons.Rounded.DragHandle,
                                contentDescription = if (isReordering) stringResource(R.string.done_reordering) else stringResource(R.string.reorder_songs),
                                tint = if (isReordering) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete_playlist_cd), tint = Color(0xFFFF4444))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f), MaterialTheme.colorScheme.background)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = playlistName,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${songs.size} song${if (songs.size != 1) "s" else ""}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = onShufflePlay,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.shuffle_play), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = onPlayAll,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.play_all), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (songs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_songs_in_playlist),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(
                        items = songs,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        if (isReordering) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val mutable = songs.toMutableList()
                                                val temp = mutable[index]
                                                mutable[index] = mutable[index - 1]
                                                mutable[index - 1] = temp
                                                onReorderSongs(mutable.map { it.id })
                                            }
                                        },
                                        enabled = index > 0
                                    ) {
                                        Icon(
                                            Icons.Rounded.KeyboardArrowUp,
                                            contentDescription = stringResource(R.string.move_up),
                                            tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < songs.size - 1) {
                                                val mutable = songs.toMutableList()
                                                val temp = mutable[index]
                                                mutable[index] = mutable[index + 1]
                                                mutable[index + 1] = temp
                                                onReorderSongs(mutable.map { it.id })
                                            }
                                        },
                                        enabled = index < songs.size - 1
                                    ) {
                                        Icon(
                                            Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = stringResource(R.string.move_down),
                                            tint = if (index < songs.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    SongItem(
                                        song = song,
                                        onClick = { onPlaySong(song) },
                                        onFavoriteToggle = { onToggleFavorite(song) },
                                        onAddToPlaylist = { songToRemoveId = song.id },
                                        onDeleteSong = { onDeleteSong(song) }
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SongItem(
                                        song = song,
                                        onClick = { onPlaySong(song) },
                                        onFavoriteToggle = { onToggleFavorite(song) },
                                        onAddToPlaylist = { songToRemoveId = song.id },
                                        onDeleteSong = { onDeleteSong(song) }
                                    )
                                }
                                IconButton(onClick = { songToRemoveId = song.id }) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = stringResource(R.string.remove),
                                        tint = Color(0xFFFF4444)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
