package com.auramusic.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack

import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.auramusic.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.auramusic.domain.model.Album
import com.auramusic.domain.model.Playlist
import com.auramusic.domain.model.Song
import com.auramusic.ui.components.AnimatedListItem
import com.auramusic.ui.components.SongItem
import com.auramusic.ui.theme.*
import androidx.compose.foundation.lazy.itemsIndexed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onSearch: (String) -> Flow<List<Song>>,
    onSearchPlaylists: (String) -> Flow<List<Playlist>>,
    onSearchFolders: (String) -> Flow<List<String>>,
    onSearchArtists: (String) -> List<String> = { emptyList() },
    onSearchAlbums: (String) -> List<Album> = { emptyList() },
    onPlaySong: (Song) -> Unit,
    onPlayNext: (Song) -> Unit = {},
    onAddToQueue: (Song) -> Unit = {},
    onToggleFavorite: (Song) -> Unit,
    onBack: () -> Unit,
    onSongMoreOptions: (Song) -> Unit = {},
    onDeleteSong: (Song) -> Unit = {},
    onPlaylistClick: (Long) -> Unit = {},
    onFolderClick: (String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (Long) -> Unit = {},
    searchHistory: List<String> = emptyList(),
    onClearSearchHistory: () -> Unit = {},
    currentSongId: Long? = null,
    animationsEnabled: Boolean = true
) {
    var query by remember { mutableStateOf("") }
    var songResults by remember { mutableStateOf(emptyList<Song>()) }
    var playlistResults by remember { mutableStateOf(emptyList<Playlist>()) }
    var folderResults by remember { mutableStateOf(emptyList<String>()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            songResults = emptyList<Song>()
            playlistResults = emptyList<Playlist>()
            folderResults = emptyList<String>()
            isSearching = false
            return@LaunchedEffect
        }
        delay(300L)
        isSearching = true
        songResults = onSearch(query).firstOrNull() ?: emptyList<Song>()
        playlistResults = onSearchPlaylists(query).firstOrNull() ?: emptyList<Playlist>()
        folderResults = onSearchFolders(query).firstOrNull() ?: emptyList<String>()
        isSearching = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SearchTopBar(
                query = query,
                onQueryChange = { query = it },
                onClear = { query = "" },
                onBack = onBack
            )

            when {
                query.isBlank() -> {
                    if (searchHistory.isNotEmpty()) {
                        RecentSearches(
                            searches = searchHistory,
                            onSelect = { query = it },
                            onClear = onClearSearchHistory
                        )
                    } else {
                        EmptySearchState()
                    }
                }
                !isSearching && songResults.isEmpty() && playlistResults.isEmpty() && folderResults.isEmpty() ->
                    NoResultsState()
                else -> SearchResults(
                    songs = songResults,
                    playlists = playlistResults,
                    folders = folderResults,
                    artists = onSearchArtists(query),
                    albums = onSearchAlbums(query),
                    onPlaySong = onPlaySong,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onToggleFavorite = onToggleFavorite,
                    onSongMoreOptions = onSongMoreOptions,
                    onDeleteSong = onDeleteSong,
                    onPlaylistClick = onPlaylistClick,
                    onFolderClick = onFolderClick,
                    onArtistClick = onArtistClick,
                    onAlbumClick = onAlbumClick,
                    currentSongId = currentSongId,
                    animationsEnabled = animationsEnabled
                )
            }
        }
    }
}

@Composable
private fun RecentSearches(
    searches: List<String>,
    onSelect: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.recent_searches),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onClear) {
                Text(
                    text = stringResource(R.string.clear),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        searches.forEach { search ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(search) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = stringResource(R.string.cd_recent_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = search,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = stringResource(R.string.search_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.search),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {}),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
    }
}

@Composable
private fun EmptySearchState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = stringResource(R.string.cd_search_music),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.search_your_music),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.search_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun NoResultsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.SearchOff,
                contentDescription = stringResource(R.string.cd_no_results),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.no_results),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.try_different_term),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SearchResults(
    songs: List<Song>,
    playlists: List<Playlist>,
    folders: List<String>,
    artists: List<String> = emptyList(),
    albums: List<Album> = emptyList(),
    onPlaySong: (Song) -> Unit,
    onPlayNext: (Song) -> Unit = {},
    onAddToQueue: (Song) -> Unit = {},
    onToggleFavorite: (Song) -> Unit,
    onSongMoreOptions: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onFolderClick: (String) -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (Long) -> Unit = {},
    currentSongId: Long? = null,
    animationsEnabled: Boolean = true
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        if (songs.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.songs),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            itemsIndexed(songs, key = { _, song -> "song_${song.id}" }) { index, song ->
                AnimatedListItem(index = index, animationEnabled = animationsEnabled) {
                    SongItem(
                        song = song,
                        onClick = { onPlaySong(song) },
                        onPlayNext = { onPlayNext(song) },
                        onAddToQueue = { onAddToQueue(song) },
                        onFavoriteToggle = { onToggleFavorite(song) },
                        onAddToPlaylist = { onSongMoreOptions(song) },
                        onDeleteSong = { onDeleteSong(song) },
                        isCurrentlyPlaying = song.id == currentSongId
                    )
                }
            }
        }

        if (artists.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.artists),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            items(artists, key = { "artist_$it" }) { artist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onArtistClick(artist) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = stringResource(R.string.cd_artist),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = artist,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        if (albums.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.albums),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            items(albums, key = { "alb_${it.id}" }) { album ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAlbumClick(album.id) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Album,
                        contentDescription = stringResource(R.string.cd_album),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = album.title,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = album.artist,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        if (playlists.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.playlists),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            items(playlists, key = { "pl_${it.id}" }) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlaylistClick(playlist.id) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = stringResource(R.string.cd_playlist),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        if (folders.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.folders),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            items(folders, key = { "folder_$it" }) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFolderClick(folder) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = stringResource(R.string.cd_folder),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = folder,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
