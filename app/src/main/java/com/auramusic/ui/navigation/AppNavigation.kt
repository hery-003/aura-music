package com.auramusic.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.auramusic.ui.components.MiniPlayer
import com.auramusic.ui.components.SharedViewModel
import com.auramusic.ui.components.AddToPlaylistDialog
import com.auramusic.ui.screens.home.HomeScreen
import com.auramusic.ui.screens.library.LibraryScreen
import com.auramusic.ui.screens.nowplaying.NowPlayingScreen
import com.auramusic.ui.screens.playlist.CreatePlaylistScreen
import com.auramusic.ui.screens.playlist.PlaylistDetailScreen
import com.auramusic.ui.screens.search.SearchScreen
import com.auramusic.ui.screens.settings.SettingsScreen
import com.auramusic.ui.screens.statistics.StatisticsScreen
import com.auramusic.ui.screens.songlist.SongListScreen
import com.auramusic.ui.screens.splash.SplashScreen
import com.auramusic.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import com.auramusic.domain.model.Song
import com.auramusic.data.preferences.AppPreferences
import com.auramusic.player.EqualizerManager
import android.Manifest
import android.app.Activity
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.content.ContentUris
import android.net.Uri
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import com.auramusic.R

data class BottomNavItem(
    val label: @Composable () -> Unit,
    val icon: @Composable () -> Unit,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    sharedViewModel: SharedViewModel
) {
    val navController = rememberNavController()
    fun safePopBackStack() {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem({ Text(stringResource(R.string.home)) }, { Icon(Icons.Rounded.Home, stringResource(R.string.home)) }, Routes.Home.route),
        BottomNavItem({ Text(stringResource(R.string.library)) }, { Icon(Icons.Rounded.LibraryMusic, stringResource(R.string.library)) }, Routes.Library.route),
        BottomNavItem({ Text(stringResource(R.string.search)) }, { Icon(Icons.Rounded.Search, stringResource(R.string.search)) }, Routes.Search.route),
        BottomNavItem({ Text(stringResource(R.string.settings)) }, { Icon(Icons.Rounded.Settings, stringResource(R.string.settings)) }, Routes.Settings.route),
    )

    val bottomNavRoutes = remember { bottomNavItems.map { it.route } }
    val showBottomBar = currentRoute in bottomNavRoutes

    val musicPlayer = remember { sharedViewModel.musicPlayer }
    val currentSong by musicPlayer.currentSong.collectAsStateWithLifecycle()
    val isPlaying by musicPlayer.isPlaying.collectAsStateWithLifecycle()
    val songs by sharedViewModel.songs.collectAsStateWithLifecycle()
    val isScanning by sharedViewModel.isScanning.collectAsStateWithLifecycle()
    val recentlyPlayed by sharedViewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val favoriteSongs by sharedViewModel.favoriteSongs.collectAsStateWithLifecycle()
    val mostPlayed by sharedViewModel.mostPlayed.collectAsStateWithLifecycle()
    val recentlyAdded by sharedViewModel.recentlyAdded.collectAsStateWithLifecycle()
    val playlists by sharedViewModel.allPlaylists.collectAsStateWithLifecycle()
    val artists by sharedViewModel.artists.collectAsStateWithLifecycle()
    val albums by sharedViewModel.albums.collectAsStateWithLifecycle()
    val genres by sharedViewModel.genres.collectAsStateWithLifecycle()
    val folders by sharedViewModel.folders.collectAsStateWithLifecycle()
    val prefs = remember { sharedViewModel.preferences }
    val themeMode by prefs.themeMode.collectAsStateWithLifecycle(initialValue = 0)
    val equalizerPreset by prefs.equalizerPreset.collectAsStateWithLifecycle(initialValue = 0)
    val crossfadeEnabled by prefs.crossfadeEnabled.collectAsStateWithLifecycle(initialValue = false)
    val crossfadeDuration by prefs.crossfadeDuration.collectAsStateWithLifecycle(initialValue = 3)
    val showVisualizer by prefs.showVisualizer.collectAsStateWithLifecycle(initialValue = true)
    val gamerMode by prefs.gamerMode.collectAsStateWithLifecycle(initialValue = false)
    val accentColor by prefs.accentColor.collectAsStateWithLifecycle(initialValue = 0xFF8B5CF6L)
    val audioQuality by prefs.audioQuality.collectAsStateWithLifecycle(initialValue = AppPreferences.AUDIO_QUALITY_NORMAL)
    val animationsEnabled by prefs.animationsEnabled.collectAsStateWithLifecycle(initialValue = true)

    val sleepTimerActive by musicPlayer.sleepTimerManager.isActive
        .collectAsStateWithLifecycle()
    val sleepTimerWarning by musicPlayer.sleepTimerManager.warningSeconds
        .collectAsStateWithLifecycle()

    val eqManager = remember { musicPlayer.equalizerManager }
    val eqBandLevels by eqManager.bandLevels.collectAsStateWithLifecycle()
    val eqBandFreqs by eqManager.bandFrequencies.collectAsStateWithLifecycle()
    val eqLevelRange = remember { eqManager.getBandLevelRange() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    var showQueueSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        sharedViewModel.errorMessage.collect { msg ->
            if (msg != null) {
                snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Short
                )
                sharedViewModel.clearError()
            }
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            songToDelete?.let { sharedViewModel.deleteSongFromDb(it.id) }
        }
        songToDelete = null
    }

    if (songToDelete != null) {
        val song = songToDelete!!
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.delete_song), color = MaterialTheme.colorScheme.onBackground)
                }
            },
            text = {
                Column {
                    Text(stringResource(R.string.delete_song_confirm, song.title))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.delete_song_warning),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val s = songToDelete ?: return@TextButton
                    songToDelete = null
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                            val uri = ContentUris.withAppendedId(collection, s.id)
                            val pendingIntent = MediaStore.createDeleteRequest(
                                context.contentResolver,
                                listOf(uri)
                            )
                            deleteLauncher.launch(
                                IntentSenderRequest.Builder(pendingIntent).build()
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            sharedViewModel.deleteSong(s)
                        }
                    } else {
                        sharedViewModel.deleteSong(s)
                    }
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { songToDelete = null }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                Column {
                    MiniPlayer(
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onPlayPause = { sharedViewModel.musicPlayer.togglePlayPause() },
                        onClick = {
                            currentSong?.let {
                                try {
                                    navController.navigate(Routes.NowPlaying.createRoute(it.id)) {
                                        launchSingleTop = true
                                    }
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        }
                    )
                    val navItemColor = if (gamerMode) {
                        val hue = rememberInfiniteTransition(label = "nav_bar").animateFloat(
                            initialValue = 0f, targetValue = 360f,
                            animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
                            label = "nav_hue"
                        )
                        Color.hsl(hue.value, 1f, 0.6f)
                    } else MaterialTheme.colorScheme.primary

                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        bottomNavItems.forEach { item ->
                            NavigationBarItem(
                                icon = item.icon,
                                label = { item.label() },
                                selected = currentRoute == item.route,
                                onClick = {
                                    try {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    } catch (e: Exception) { e.printStackTrace() }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = navItemColor,
                                    selectedTextColor = navItemColor,
                                    unselectedIconColor = MaterialTheme.colorScheme.outline,
                                    unselectedTextColor = MaterialTheme.colorScheme.outline,
                                    indicatorColor = navItemColor.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val addToPlaylistSongId by sharedViewModel.addToPlaylistSongId
            .collectAsStateWithLifecycle()
        if (addToPlaylistSongId != null) {
            AddToPlaylistDialog(
                playlists = playlists,
                onDismiss = { sharedViewModel.dismissAddToPlaylistDialog() },
                onSelectPlaylist = { playlistId ->
                    sharedViewModel.confirmAddToPlaylist(playlistId)
                }
            )
        }

        NavHost(
            navController = navController,
            startDestination = Routes.Splash.route,
            modifier = Modifier.padding(padding),
            enterTransition = {
                if (animationsEnabled) fadeIn(animationSpec = tween(300)) else fadeIn(animationSpec = tween(0))
            },
            exitTransition = {
                if (animationsEnabled) fadeOut(animationSpec = tween(300)) else fadeOut(animationSpec = tween(0))
            }
        ) {
            composable(Routes.Splash.route) {
                val splashContext = LocalContext.current
                var hasPermissionChecked by remember { mutableStateOf(false) }
                var permissionsReady by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val granted = withContext(Dispatchers.IO) {
                        try {
                            delay(500)
                            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                Manifest.permission.READ_MEDIA_AUDIO
                            else
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            try {
                                ContextCompat.checkSelfPermission(splashContext, permission) == PackageManager.PERMISSION_GRANTED
                            } catch (e: Exception) {
                                Log.e("AppNavigation", "Error checking permission", e)
                                false
                            }
                        } catch (e: Exception) {
                            Log.e("AppNavigation", "Error in splash", e)
                            false
                        }
                    }
                    hasPermissionChecked = true
                    permissionsReady = granted
                    if (granted) {
                        try {
                            sharedViewModel.scanMusic()
                        } catch (e: Exception) {
                            Log.e("AppNavigation", "Error scanning music", e)
                        }
                    }
                }

                SplashScreen(
                    onSplashFinished = {
                        try {
                            if (navController.currentDestination?.route == Routes.Splash.route) {
                                navController.navigate(Routes.Home.route) {
                                    popUpTo(Routes.Splash.route) { inclusive = true }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("AppNavigation", "Navigation error", e)
                        }
                    },
                    waitForPermissions = hasPermissionChecked,
                    animationsEnabled = animationsEnabled
                )
            }

            composable(Routes.Home.route) {
                HomeScreen(
                    onNavigateToLibrary = { navController.navigate(Routes.Library.route) },
                    onNavigateToSearch = { navController.navigate(Routes.Search.route) },
                    onNavigateToSettings = { navController.navigate(Routes.Settings.route) },
                    onNavigateToNowPlaying = { songId ->
                        try {
                            navController.navigate(Routes.NowPlaying.createRoute(songId))
                        } catch (e: Exception) { e.printStackTrace() }
                    },
                    onNavigateToPlaylist = { playlistId ->
                        try {
                            navController.navigate(Routes.PlaylistDetail.createRoute(playlistId))
                        } catch (e: Exception) { e.printStackTrace() }
                    },
                    onNavigateToAlbum = { albumId ->
                        navController.navigate(Routes.AlbumSongs.createRoute(albumId))
                    },
                    onPlaySong = { sharedViewModel.playSong(it) },
                    onToggleFavorite = { sharedViewModel.toggleFavorite(it) },
                    onSongMoreOptions = { sharedViewModel.showAddToPlaylistDialog(it.id) },
                    recentlyPlayed = recentlyPlayed,
                    favoriteSongs = favoriteSongs,
                    mostPlayed = mostPlayed,
                    recentlyAdded = recentlyAdded,
                    playlists = playlists,
                    albums = albums,
                    isScanning = isScanning,
                    onScan = { sharedViewModel.scanMusic() },
                    onCreatePlaylist = { navController.navigate(Routes.CreatePlaylist.route) }
                )
            }

            composable(Routes.Library.route) {
                LibraryScreen(
                    songs = songs,
                    artists = artists,
                    albums = albums,
                    genres = genres,
                    folders = folders,
                    playlists = playlists,
                    onPlaySong = { sharedViewModel.playSong(it) },
                    onToggleFavorite = { sharedViewModel.toggleFavorite(it) },
                    onSongMoreOptions = { sharedViewModel.showAddToPlaylistDialog(it.id) },
                    onDeleteSong = { songToDelete = it },
                    onNavigateToPlaylist = { navController.navigate(Routes.PlaylistDetail.createRoute(it)) },
                    onNavigateToNowPlaying = { navController.navigate(Routes.NowPlaying.createRoute(it)) },
                    onCreatePlaylist = { navController.navigate(Routes.CreatePlaylist.route) },
                    onArtistClick = { artistName ->
                        navController.navigate(Routes.ArtistSongs.createRoute(artistName))
                    },
                    onAlbumClick = { albumId ->
                        navController.navigate(Routes.AlbumSongs.createRoute(albumId))
                    },
                    onGenreClick = { genreName ->
                        navController.navigate(Routes.GenreSongs.createRoute(genreName))
                    },
                    onFolderClick = { folderPath ->
                        navController.navigate(Routes.FolderSongs.createRoute(folderPath))
                    }
                )
            }

            composable(Routes.Search.route) {
                val allArtists by sharedViewModel.artists.collectAsStateWithLifecycle()
                val allAlbums by sharedViewModel.albums.collectAsStateWithLifecycle()
                val searchHistory by sharedViewModel.searchHistory.collectAsStateWithLifecycle()
                SearchScreen(
                    onSearch = { query -> sharedViewModel.searchSongs(query) },
                    onSearchPlaylists = { query -> sharedViewModel.searchPlaylists(query) },
                    onSearchFolders = { query -> sharedViewModel.searchFolders(query) },
                    onSearchArtists = { query ->
                        if (query.isBlank()) emptyList()
                        else allArtists.filter { it.contains(query, ignoreCase = true) }
                    },
                    onSearchAlbums = { query ->
                        if (query.isBlank()) emptyList()
                        else allAlbums.filter { it.title.contains(query, ignoreCase = true) }
                    },
                    onPlaySong = { sharedViewModel.playSong(it) },
                    onToggleFavorite = { sharedViewModel.toggleFavorite(it) },
                    onBack = { safePopBackStack() },
                    onSongMoreOptions = { sharedViewModel.showAddToPlaylistDialog(it.id) },
                    onDeleteSong = { songToDelete = it },
                    onPlaylistClick = { navController.navigate(Routes.PlaylistDetail.createRoute(it)) },
                    onFolderClick = { folderPath ->
                        navController.navigate(Routes.FolderSongs.createRoute(folderPath))
                    },
                    onArtistClick = { artistName ->
                        navController.navigate(Routes.ArtistSongs.createRoute(artistName))
                    },
                    onAlbumClick = { albumId ->
                        navController.navigate(Routes.AlbumSongs.createRoute(albumId))
                    },
                    searchHistory = searchHistory,
                    onClearSearchHistory = { sharedViewModel.clearSearchHistory() },
                    animationsEnabled = animationsEnabled
                )
            }

            composable(Routes.Settings.route) {
                SettingsScreen(
                    themeMode = themeMode,
                    equalizerPreset = equalizerPreset,
                    crossfadeEnabled = crossfadeEnabled,
                    crossfadeDuration = crossfadeDuration,
                    showVisualizer = showVisualizer,
                    showGamerMode = gamerMode,
                    accentColor = accentColor,
                    audioQuality = audioQuality,
                    animationsEnabled = animationsEnabled,
                    sleepTimerActive = sleepTimerActive,
                    customEqBands = eqBandLevels,
                    bandFrequencies = eqBandFreqs,
                    eqBandLevelRange = eqLevelRange,
                    onAccentColorChange = { scope.launch { sharedViewModel.preferences.setAccentColor(it) } },
                    onThemeModeChange = { scope.launch { sharedViewModel.preferences.setThemeMode(it) } },
                    onEqualizerPresetChange = { preset ->
                        sharedViewModel.setEqualizerPreset(preset)
                        if (preset == EqualizerManager.PRESET_CUSTOM) {
                            sharedViewModel.loadCustomEqualizerBands()
                        }
                    },
                    onCustomBandLevelChange = { bandIndex, level ->
                        sharedViewModel.setCustomEqualizerBand(bandIndex, level)
                    },
                    onCrossfadeEnabledChange = { enabled ->
                        sharedViewModel.setCrossfade(enabled, crossfadeDuration)
                    },
                    onCrossfadeDurationChange = { duration ->
                        sharedViewModel.setCrossfade(crossfadeEnabled, duration)
                    },
                    onShowVisualizerChange = { scope.launch { sharedViewModel.preferences.setShowVisualizer(it) } },
                    onGamerModeChange = { enabled ->
                        scope.launch { sharedViewModel.preferences.setGamerMode(enabled) }
                        if (enabled) {
                            sharedViewModel.setEqualizerPreset(EqualizerManager.PRESET_GAMER)
                        }
                    },
                    onAudioQualityChange = { sharedViewModel.setAudioQuality(it) },
                    onAnimationsEnabledChange = { sharedViewModel.setAnimationsEnabled(it) },
                    onRescan = { sharedViewModel.scanMusic() },
                    onBack = { safePopBackStack() },
                    onStatistics = { navController.navigate(Routes.Statistics.route) },
                    onSleepTimerStart = { sharedViewModel.startSleepTimer(it) },
                    onSleepTimerStop = { sharedViewModel.stopSleepTimer() }
                )
            }

            composable(
                route = Routes.NowPlaying.route,
                arguments = listOf(navArgument("songId") { type = NavType.LongType })
            ) { _ ->
                val currentPosition by musicPlayer.currentPosition.collectAsStateWithLifecycle()
                val duration by musicPlayer.duration.collectAsStateWithLifecycle()
                val shuffleMode by musicPlayer.shuffleMode.collectAsStateWithLifecycle()
                val repeatMode by musicPlayer.repeatMode.collectAsStateWithLifecycle()

                val dominantColor by musicPlayer.colorManager.dominantColor
                    .collectAsStateWithLifecycle()
                val auraMode by musicPlayer.colorManager.auraMode
                    .collectAsStateWithLifecycle()
                val fftMagnitudes by musicPlayer.fftVisualizer.fftMagnitudes
                    .collectAsStateWithLifecycle()
                val waveform by musicPlayer.fftVisualizer.waveform
                    .collectAsStateWithLifecycle()

                NowPlayingScreen(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    shuffleMode = shuffleMode,
                    repeatMode = repeatMode,
                    onPlayPause = { musicPlayer.togglePlayPause() },
                    onNext = { musicPlayer.playNext() },
                    onPrevious = { musicPlayer.playPrevious() },
                    onSeek = { musicPlayer.seekTo(it) },
                    onToggleShuffle = { musicPlayer.toggleShuffle() },
                    onToggleRepeat = { musicPlayer.toggleRepeatMode() },
                    onToggleFavorite = {
                        currentSong?.let { sharedViewModel.toggleFavorite(it) }
                    },
                    onBack = { safePopBackStack() },
                    onQueue = { showQueueSheet = true },
                    sleepTimerActive = sleepTimerActive,
                    sleepTimerWarning = sleepTimerWarning,
                    onSleepTimerClick = {
                        if (sleepTimerActive) {
                            sharedViewModel.stopSleepTimer()
                        } else {
                            sharedViewModel.startSleepTimer(15)
                        }
                    },
                    dominantColor = dominantColor,
                    gamerMode = gamerMode,
                    auraMode = auraMode,
                    fftMagnitudes = fftMagnitudes,
                    waveform = waveform,
                    animationsEnabled = animationsEnabled
                )
            }

            composable(
                route = Routes.PlaylistDetail.route,
                arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments!!.getLong("playlistId")
                val playlist = playlists.find { it.id == playlistId }
                val playlistSongs by sharedViewModel.repository.getPlaylistSongs(playlistId)
                    .collectAsStateWithLifecycle(initialValue = emptyList())

                PlaylistDetailScreen(
                    playlistId = playlistId,
                    playlistName = playlist?.name ?: stringResource(R.string.playlists),
                    playlistDescription = playlist?.description ?: "",
                    songs = playlistSongs,
                    onPlaySong = { sharedViewModel.playSong(it) },
                    onToggleFavorite = { sharedViewModel.toggleFavorite(it) },
                    onDeleteSong = { songToDelete = it },
                    onRemoveSong = { songId ->
                        sharedViewModel.removeSongFromPlaylist(playlistId, songId)
                    },
                    onPlayAll = {
                        if (playlistSongs.isNotEmpty()) {
                            musicPlayer.setQueue(playlistSongs, 0)
                            musicPlayer.play()
                        }
                    },
                    onShufflePlay = {
                        if (playlistSongs.isNotEmpty()) {
                            val shuffled = playlistSongs.shuffled()
                            musicPlayer.setQueue(shuffled, 0)
                            musicPlayer.play()
                        }
                    },
                    onBack = { safePopBackStack() },
                    onDeletePlaylist = {
                        sharedViewModel.deletePlaylist(playlistId)
                        safePopBackStack()
                    },
                    onReorderSongs = { songIds ->
                        sharedViewModel.reorderPlaylistSongs(playlistId, songIds)
                    },
                    onEditPlaylist = { _, name, desc ->
                        sharedViewModel.updatePlaylistName(playlistId, name, desc)
                    }
                )
            }

            composable(Routes.CreatePlaylist.route) {
                CreatePlaylistScreen(
                    onBack = { safePopBackStack() },
                    onCreatePlaylist = { name, desc ->
                        sharedViewModel.createPlaylist(name, desc)
                        safePopBackStack()
                    }
                )
            }

            composable(Routes.Statistics.route) {
                val listenTime by sharedViewModel.totalListeningTime
                    .collectAsStateWithLifecycle(initialValue = 0L)
                val topSongs = mostPlayed.take(5)
                val topArtistNames = remember(mostPlayed) {
                    mostPlayed
                        .groupBy { it.artistDisplay }
                        .entries
                        .sortedByDescending { it.value.sumOf { s -> s.playCount } }
                        .take(5)
                        .map { it.key }
                }
                StatisticsScreen(
                    totalSongs = songs.size,
                    totalArtists = artists.size,
                    totalAlbums = albums.size,
                    totalListeningTimeMinutes = listenTime / 60,
                    topSongs = topSongs,
                    topArtistNames = topArtistNames,
                    onBack = { safePopBackStack() }
                )
            }

            composable(
                route = Routes.ArtistSongs.route,
                arguments = listOf(navArgument("artistName") { type = NavType.StringType })
            ) { backStackEntry ->
                val artistName = Uri.decode(backStackEntry.arguments?.getString("artistName") ?: return@composable)
                val artistSongs by sharedViewModel.repository.getSongsByArtist(artistName)
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                SongListScreen(
                    title = artistName,
                    subtitle = stringResource(R.string.artist),
                    songs = artistSongs,
                    onPlaySong = { sharedViewModel.playSong(it) },
                    onToggleFavorite = { sharedViewModel.toggleFavorite(it) },
                    onSongMoreOptions = { sharedViewModel.showAddToPlaylistDialog(it) },
                    onDeleteSong = { songToDelete = it },
                    onBack = { safePopBackStack() },
                    onPlayAll = {
                        musicPlayer.setQueue(artistSongs, 0)
                        musicPlayer.play()
                        navController.navigate(Routes.NowPlaying.createRoute(artistSongs.first().id))
                    },
                    onShufflePlay = {
                        val shuffled = artistSongs.shuffled()
                        musicPlayer.setQueue(shuffled, 0)
                        musicPlayer.play()
                        navController.navigate(Routes.NowPlaying.createRoute(shuffled.first().id))
                    },
                    animationsEnabled = animationsEnabled
                )
            }

            composable(
                route = Routes.AlbumSongs.route,
                arguments = listOf(navArgument("albumId") { type = NavType.LongType })
            ) { backStackEntry ->
                val albumId = backStackEntry.arguments!!.getLong("albumId")
                val albumSongs by sharedViewModel.repository.getSongsByAlbum(albumId)
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                val albumTitle = albumSongs.firstOrNull()?.album ?: ""
                SongListScreen(
                    title = albumTitle,
                    subtitle = stringResource(R.string.album),
                    songs = albumSongs,
                    onPlaySong = { sharedViewModel.playSong(it) },
                    onToggleFavorite = { sharedViewModel.toggleFavorite(it) },
                    onSongMoreOptions = { sharedViewModel.showAddToPlaylistDialog(it) },
                    onDeleteSong = { songToDelete = it },
                    onBack = { safePopBackStack() },
                    onPlayAll = {
                        musicPlayer.setQueue(albumSongs, 0)
                        musicPlayer.play()
                        navController.navigate(Routes.NowPlaying.createRoute(albumSongs.first().id))
                    },
                    onShufflePlay = {
                        val shuffled = albumSongs.shuffled()
                        musicPlayer.setQueue(shuffled, 0)
                        musicPlayer.play()
                        navController.navigate(Routes.NowPlaying.createRoute(shuffled.first().id))
                    },
                    animationsEnabled = animationsEnabled
                )
            }

            composable(
                route = Routes.GenreSongs.route,
                arguments = listOf(navArgument("genreName") { type = NavType.StringType })
            ) { backStackEntry ->
                val genreName = Uri.decode(backStackEntry.arguments?.getString("genreName") ?: return@composable)
                val genreSongs by sharedViewModel.repository.getSongsByGenre(genreName)
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                SongListScreen(
                    title = genreName,
                    subtitle = stringResource(R.string.genre),
                    songs = genreSongs,
                    onPlaySong = { sharedViewModel.playSong(it) },
                    onToggleFavorite = { sharedViewModel.toggleFavorite(it) },
                    onSongMoreOptions = { sharedViewModel.showAddToPlaylistDialog(it) },
                    onDeleteSong = { songToDelete = it },
                    onBack = { safePopBackStack() },
                    onPlayAll = {
                        musicPlayer.setQueue(genreSongs, 0)
                        musicPlayer.play()
                        navController.navigate(Routes.NowPlaying.createRoute(genreSongs.first().id))
                    },
                    onShufflePlay = {
                        val shuffled = genreSongs.shuffled()
                        musicPlayer.setQueue(shuffled, 0)
                        musicPlayer.play()
                        navController.navigate(Routes.NowPlaying.createRoute(shuffled.first().id))
                    },
                    animationsEnabled = animationsEnabled
                )
            }

            composable(
                route = Routes.FolderSongs.route,
                arguments = listOf(navArgument("folderPath") { type = NavType.StringType })
            ) { backStackEntry ->
                val folderPath = Uri.decode(backStackEntry.arguments?.getString("folderPath") ?: return@composable)
                val folderSongs by sharedViewModel.repository.getSongsByPathPrefix(folderPath)
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                val folderName = folderPath.substringAfterLast(java.io.File.separator)
                SongListScreen(
                    title = folderName,
                    subtitle = stringResource(R.string.folder),
                    songs = folderSongs,
                    onPlaySong = { sharedViewModel.playSong(it) },
                    onToggleFavorite = { sharedViewModel.toggleFavorite(it) },
                    onSongMoreOptions = { sharedViewModel.showAddToPlaylistDialog(it) },
                    onDeleteSong = { songToDelete = it },
                    onBack = { safePopBackStack() },
                    onPlayAll = {
                        musicPlayer.setQueue(folderSongs, 0)
                        musicPlayer.play()
                        navController.navigate(Routes.NowPlaying.createRoute(folderSongs.first().id))
                    },
                    onShufflePlay = {
                        val shuffled = folderSongs.shuffled()
                        musicPlayer.setQueue(shuffled, 0)
                        musicPlayer.play()
                        navController.navigate(Routes.NowPlaying.createRoute(shuffled.first().id))
                    },
                    animationsEnabled = animationsEnabled
                )
            }
        }

        if (showQueueSheet) {
            val queue by musicPlayer.queue.collectAsStateWithLifecycle()
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = stringResource(R.string.queue),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                    if (queue.isEmpty()) {
                        Text(
                            text = stringResource(R.string.queue_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    } else {
                        LazyColumn {
                            itemsIndexed(queue, key = { _, song -> song.id }) { index, song ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(32.dp)
                                    )
                                    com.auramusic.ui.components.SongItem(
                                        song = song,
                                        onClick = {
                                            musicPlayer.exoPlayer?.seekTo(index, 0L)
                                            musicPlayer.play()
                                            showQueueSheet = false
                                        },
                                        onFavoriteToggle = { sharedViewModel.toggleFavorite(song) },
                                        onAddToPlaylist = { sharedViewModel.showAddToPlaylistDialog(song.id) },
                                        onDeleteSong = { songToDelete = song }
                                    )
                                    IconButton(onClick = { musicPlayer.removeFromQueue(index) }) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = stringResource(R.string.remove),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
}
