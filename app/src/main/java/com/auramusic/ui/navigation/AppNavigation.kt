package com.auramusic.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.auramusic.ui.components.MiniPlayer
import com.auramusic.ui.components.LibraryViewModel
import com.auramusic.ui.components.PlayerViewModel
import com.auramusic.ui.components.SettingsViewModel
import com.auramusic.ui.components.AddToPlaylistDialog
import com.auramusic.ui.screens.home.HomeScreen
import com.auramusic.ui.screens.library.LibraryScreen
import com.auramusic.ui.screens.nowplaying.NowPlayingScreen
import com.auramusic.ui.screens.playlist.CreatePlaylistScreen
import com.auramusic.ui.screens.playlist.PlaylistDetailScreen
import com.auramusic.ui.screens.history.HistoryScreen
import com.auramusic.ui.screens.onboarding.OnboardingScreen
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
import timber.log.Timber
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
    playerViewModel: PlayerViewModel,
    libraryViewModel: LibraryViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val musicPlayer = playerViewModel.musicPlayer
    val safePopBackStack = remember {
        { if (navController.previousBackStackEntry != null) navController.popBackStack() }
    }
    val playAllSongs = remember {
        { songs: List<Song> ->
            if (songs.isNotEmpty()) {
                musicPlayer.setQueue(songs, 0)
                musicPlayer.play()
                playerViewModel.startPlaybackService()
                navController.navigate(Routes.NowPlaying.createRoute(songs.first().id))
            }
        }
    }
    val shufflePlaySongs = remember {
        { songs: List<Song> ->
            if (songs.isNotEmpty()) {
                val shuffled = songs.shuffled()
                musicPlayer.setQueue(shuffled, 0)
                musicPlayer.play()
                playerViewModel.startPlaybackService()
                navController.navigate(Routes.NowPlaying.createRoute(shuffled.first().id))
            }
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem({ Text(stringResource(R.string.home)) }, { Icon(Icons.Rounded.Home, stringResource(R.string.home)) }, Routes.Home.route),
        BottomNavItem({ Text(stringResource(R.string.library)) }, { Icon(Icons.Rounded.MusicNote, stringResource(R.string.library)) }, Routes.Library.route),
        BottomNavItem({ Text(stringResource(R.string.search)) }, { Icon(Icons.Rounded.Search, stringResource(R.string.search)) }, Routes.Search.route),
        BottomNavItem({ Text(stringResource(R.string.settings)) }, { Icon(Icons.Rounded.Settings, stringResource(R.string.settings)) }, Routes.Settings.route),
    )

    val bottomNavRoutes = remember { bottomNavItems.map { it.route } }
    val showBottomBar = currentRoute in bottomNavRoutes

    val currentSong by musicPlayer.currentSong.collectAsStateWithLifecycle()
    val isPlaying by musicPlayer.isPlaying.collectAsStateWithLifecycle()
    val prefs = remember { settingsViewModel.preferences }
    val gamerMode by prefs.gamerMode.collectAsStateWithLifecycle(initialValue = false)
    val animationsEnabled by prefs.animationsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val playlists by libraryViewModel.allPlaylists.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val importPlaylistLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            libraryViewModel.importPlaylist(uri)
        }
    }
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    var pendingDeleteSongId by remember { mutableStateOf<Long?>(null) }
    var showQueueSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        libraryViewModel.errorMessage.collect { msg ->
            if (msg != null) {
                snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Short
                )
                libraryViewModel.clearError()
            }
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val id = pendingDeleteSongId
        pendingDeleteSongId = null
        if (result.resultCode == Activity.RESULT_OK && id != null) {
            libraryViewModel.deleteSongFromDb(id)
        }
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
                            pendingDeleteSongId = s.id
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
                            Timber.e(e, "Failed to create delete request")
                            pendingDeleteSongId = null
                            libraryViewModel.deleteSong(s)
                        }
                    } else {
                        libraryViewModel.deleteSong(s)
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
                        onPlayPause = { musicPlayer.togglePlayPause() },
                        onClick = {
                            currentSong?.let {
                                try {
                                    navController.navigate(Routes.NowPlaying.createRoute(it.id)) {
                                        launchSingleTop = true
                                    }
                                } catch (e: Exception) { Timber.e(e, "Navigation failed") }
                            }
                        },
                        gamerMode = gamerMode
                    )
                    val infiniteTransition = rememberInfiniteTransition(label = "nav_bar")
                    val hue by infiniteTransition.animateFloat(
                        initialValue = 0f, targetValue = 360f,
                        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
                        label = "nav_hue"
                    )
                    val navItemColor = if (gamerMode) Color.hsl(hue % 360f, 1f, 0.6f)
                    else MaterialTheme.colorScheme.primary

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
                                    } catch (e: Exception) { Timber.e(e, "Navigation failed") }
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
        val addToPlaylistSongId by libraryViewModel.addToPlaylistSongId
            .collectAsStateWithLifecycle()
        if (addToPlaylistSongId != null) {
            AddToPlaylistDialog(
                playlists = playlists,
                onDismiss = { libraryViewModel.dismissAddToPlaylistDialog() },
                onSelectPlaylist = { playlistId ->
                    libraryViewModel.confirmAddToPlaylist(playlistId)
                }
            )
        }

        SharedTransitionLayout(modifier = Modifier.padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = Routes.Splash.route,
                modifier = Modifier,
                enterTransition = {
                    if (animationsEnabled)
                        fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.98f, animationSpec = tween(250))
                    else
                        fadeIn(animationSpec = tween(0))
                },
                exitTransition = {
                    if (animationsEnabled)
                        fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.98f, animationSpec = tween(250))
                    else
                        fadeOut(animationSpec = tween(0))
                },
                popEnterTransition = {
                    if (animationsEnabled)
                        slideInHorizontally(animationSpec = tween(300)) { -it / 3 } + fadeIn(animationSpec = tween(200))
                    else
                        fadeIn(animationSpec = tween(0))
                },
                popExitTransition = {
                    if (animationsEnabled)
                        slideOutHorizontally(animationSpec = tween(300)) { it / 2 } + fadeOut(animationSpec = tween(200))
                    else
                        fadeOut(animationSpec = tween(0))
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
                                Timber.e(e, "Error checking permission")
                                false
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error in splash")
                            false
                        }
                    }
                    hasPermissionChecked = true
                    permissionsReady = granted
                    if (granted) {
                        try {
                            libraryViewModel.scanMusic()
                        } catch (e: Exception) {
                            Timber.e(e, "Error scanning music")
                        }
                    }
                }

                val onboardingShown by prefs.onboardingShown
                    .collectAsStateWithLifecycle(initialValue = false)

                SplashScreen(
                    onSplashFinished = {
                        try {
                            if (navController.currentDestination?.route == Routes.Splash.route) {
                                val dest = if (!onboardingShown) Routes.Onboarding.route else Routes.Home.route
                                navController.navigate(dest) {
                                    popUpTo(Routes.Splash.route) { inclusive = true }
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Navigation error")
                        }
                    },
                    waitForPermissions = hasPermissionChecked,
                    animationsEnabled = animationsEnabled
                )
            }

            composable(Routes.Home.route) {
                val recentlyPlayed by libraryViewModel.recentlyPlayed.collectAsStateWithLifecycle()
                val favoriteSongs by libraryViewModel.favoriteSongs.collectAsStateWithLifecycle()
                val mostPlayed by libraryViewModel.mostPlayed.collectAsStateWithLifecycle()
                val recentlyAdded by libraryViewModel.recentlyAdded.collectAsStateWithLifecycle()
                val albums by libraryViewModel.albums.collectAsStateWithLifecycle()
                val isScanning by libraryViewModel.isScanning.collectAsStateWithLifecycle()
                HomeScreen(
                    onNavigateToLibrary = { navController.navigate(Routes.Library.route) },
                    onNavigateToSearch = { navController.navigate(Routes.Search.route) },
                    onNavigateToSettings = { navController.navigate(Routes.Settings.route) },
                    onNavigateToNowPlaying = { songId ->
                        try {
                            navController.navigate(Routes.NowPlaying.createRoute(songId))
                        } catch (e: Exception) { Timber.e(e, "Navigation failed") }
                    },
                    onNavigateToPlaylist = { playlistId ->
                        try {
                            navController.navigate(Routes.PlaylistDetail.createRoute(playlistId))
                        } catch (e: Exception) { Timber.e(e, "Navigation failed") }
                    },
                    onNavigateToAlbum = { albumId ->
                        navController.navigate(Routes.AlbumSongs.createRoute(albumId))
                    },
                    onPlaySong = { playerViewModel.playSong(it) },
                    onToggleFavorite = { playerViewModel.toggleFavorite(it) },
                    onSongMoreOptions = { libraryViewModel.showAddToPlaylistDialog(it.id) },
                    recentlyPlayed = recentlyPlayed,
                    favoriteSongs = favoriteSongs,
                    mostPlayed = mostPlayed,
                    recentlyAdded = recentlyAdded,
                    playlists = playlists,
                    albums = albums,
                    isScanning = isScanning,
                    onScan = { libraryViewModel.scanMusic() },
                    onCreatePlaylist = { navController.navigate(Routes.CreatePlaylist.route) },
                    currentSongId = currentSong?.id
                )
            }

            composable(Routes.Library.route) {
                val songs by libraryViewModel.songs.collectAsStateWithLifecycle()
                val albums by libraryViewModel.albums.collectAsStateWithLifecycle()
                val artists by libraryViewModel.artists.collectAsStateWithLifecycle()
                val genres by libraryViewModel.genres.collectAsStateWithLifecycle()
                val folders by libraryViewModel.folders.collectAsStateWithLifecycle()
                LibraryScreen(
                    onNavigateToNowPlaying = { songId ->
                        try {
                            navController.navigate(Routes.NowPlaying.createRoute(songId))
                        } catch (e: Exception) { Timber.e(e, "Navigation failed") }
                    },
                    onPlaySong = { song ->
                        val idx = songs.indexOf(song)
                        if (idx >= 0) {
                            musicPlayer.setQueue(songs, idx)
                            musicPlayer.play()
                            playerViewModel.startPlaybackService()
                        }
                    },
                    onToggleFavorite = { playerViewModel.toggleFavorite(it) },
                    onSongMoreOptions = { libraryViewModel.showAddToPlaylistDialog(it.id) },
                    onPlayNext = { playerViewModel.playSongNext(it) },
                    onAddToQueue = { playerViewModel.addToQueue(it) },
                    onDeleteSong = { songToDelete = it },
                    onAlbumClick = { albumId ->
                        navController.navigate(Routes.AlbumSongs.createRoute(albumId))
                    },
                    onCreatePlaylist = { navController.navigate(Routes.CreatePlaylist.route) },
                    onNavigateToPlaylist = { playlistId ->
                        try {
                            navController.navigate(Routes.PlaylistDetail.createRoute(playlistId))
                        } catch (e: Exception) { Timber.e(e, "Navigation failed") }
                    },
                    onImportPlaylist = { importPlaylistLauncher.launch(arrayOf("application/json")) },
                    onArtistClick = { artistName ->
                        navController.navigate(Routes.ArtistSongs.createRoute(artistName))
                    },
                    onGenreClick = { genreName ->
                        navController.navigate(Routes.GenreSongs.createRoute(genreName))
                    },
                    onFolderClick = { folderPath ->
                        navController.navigate(Routes.FolderSongs.createRoute(folderPath))
                    },
                    onPlayAll = { playAllSongs(songs) },
                    onShufflePlay = { shufflePlaySongs(songs) },
                    songs = songs,
                    albums = albums,
                    artists = artists,
                    genres = genres,
                    folders = folders,
                    playlists = playlists,
                    currentSongId = currentSong?.id
                )
            }

            composable(Routes.Search.route) {
                val allArtists by libraryViewModel.artists.collectAsStateWithLifecycle()
                val allAlbums by libraryViewModel.albums.collectAsStateWithLifecycle()
                val searchHistory by libraryViewModel.searchHistory.collectAsStateWithLifecycle()
                SearchScreen(
                    onSearch = { query -> libraryViewModel.searchSongs(query) },
                    onSearchPlaylists = { query -> libraryViewModel.searchPlaylists(query) },
                    onSearchFolders = { query -> libraryViewModel.searchFolders(query) },
                    onSearchArtists = { query ->
                        if (query.isBlank()) emptyList()
                        else allArtists.filter { it.contains(query, ignoreCase = true) }
                    },
                    onSearchAlbums = { query ->
                        if (query.isBlank()) emptyList()
                        else allAlbums.filter { it.title.contains(query, ignoreCase = true) }
                    },
                    onPlaySong = { playerViewModel.playSong(it) },
                    onPlayNext = { playerViewModel.playSongNext(it) },
                    onAddToQueue = { playerViewModel.addToQueue(it) },
                    onToggleFavorite = { playerViewModel.toggleFavorite(it) },
                    onBack = safePopBackStack,
                    onSongMoreOptions = { libraryViewModel.showAddToPlaylistDialog(it.id) },
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
                    onClearSearchHistory = { libraryViewModel.clearSearchHistory() },
                    currentSongId = currentSong?.id,
                    animationsEnabled = animationsEnabled
                )
            }

            composable(Routes.Settings.route) {
                val settingsPrefs = remember { settingsViewModel.preferences }
                val themeMode by settingsPrefs.themeMode.collectAsStateWithLifecycle(initialValue = 0)
                val equalizerPreset by settingsPrefs.equalizerPreset.collectAsStateWithLifecycle(initialValue = 0)
                val crossfadeEnabled by settingsPrefs.crossfadeEnabled.collectAsStateWithLifecycle(initialValue = false)
                val crossfadeDuration by settingsPrefs.crossfadeDuration.collectAsStateWithLifecycle(initialValue = 3)
                val showVisualizer by settingsPrefs.showVisualizer.collectAsStateWithLifecycle(initialValue = true)
                val accentColor by settingsPrefs.accentColor.collectAsStateWithLifecycle(initialValue = 0xFF8B5CF6.toInt())
                val audioQuality by settingsPrefs.audioQuality.collectAsStateWithLifecycle(initialValue = AppPreferences.AUDIO_QUALITY_NORMAL)
                val playbackSpeed by settingsPrefs.playbackSpeed.collectAsStateWithLifecycle(initialValue = 1f)
                val sleepTimerActive by musicPlayer.sleepTimerManager.isActive.collectAsStateWithLifecycle()
                val eqManager = remember { musicPlayer.equalizerManager }
                val eqBandLevels by eqManager.bandLevels.collectAsStateWithLifecycle()
                val eqBandFreqs by eqManager.bandFrequencies.collectAsStateWithLifecycle()
                val eqLevelRange = remember { eqManager.getBandLevelRange() }
                var previousPreset by rememberSaveable { mutableIntStateOf(EqualizerManager.PRESET_NORMAL) }
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
                    onAccentColorChange = { scope.launch { settingsPrefs.setAccentColor(it) } },
                    onThemeModeChange = { scope.launch { settingsPrefs.setThemeMode(it) } },
                    onEqualizerPresetChange = { preset ->
                        settingsViewModel.setEqualizerPreset(preset)
                        if (preset == EqualizerManager.PRESET_CUSTOM) {
                            settingsViewModel.loadCustomEqualizerBands()
                        }
                    },
                    onCustomBandLevelChange = { bandIndex, level ->
                        settingsViewModel.setCustomEqualizerBand(bandIndex, level)
                    },
                    onCrossfadeEnabledChange = { enabled ->
                        playerViewModel.setCrossfade(enabled, crossfadeDuration)
                    },
                    onCrossfadeDurationChange = { duration ->
                        playerViewModel.setCrossfade(crossfadeEnabled, duration)
                    },
                    onShowVisualizerChange = { scope.launch { settingsPrefs.setShowVisualizer(it) } },
                    onGamerModeChange = { enabled ->
                        scope.launch { settingsPrefs.setGamerMode(enabled) }
                        if (enabled) {
                            previousPreset = equalizerPreset
                            settingsViewModel.setEqualizerPreset(EqualizerManager.PRESET_GAMER)
                        } else {
                            settingsViewModel.setEqualizerPreset(previousPreset)
                        }
                    },
                    onAudioQualityChange = { playerViewModel.setAudioQuality(it) },
                    onAnimationsEnabledChange = { playerViewModel.setAnimationsEnabled(it) },
                    playbackSpeed = playbackSpeed,
                    onPlaybackSpeedChange = { musicPlayer.setPlaybackSpeed(it) },
                    onRescan = { libraryViewModel.scanMusic() },
                    onBack = safePopBackStack,
                    onStatistics = { navController.navigate(Routes.Statistics.route) },
                    onHistory = { navController.navigate(Routes.History.route) },
                    onSleepTimerStart = { playerViewModel.startSleepTimer(it) },
                    onSleepTimerStop = { playerViewModel.stopSleepTimer() }
                )
            }

            composable(Routes.Onboarding.route) {
                OnboardingScreen(
                    onComplete = {
                        scope.launch { prefs.setOnboardingShown() }
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.History.route) {
                val recentlyPlayed by libraryViewModel.recentlyPlayed.collectAsStateWithLifecycle()
                HistoryScreen(
                    recentlyPlayed = recentlyPlayed,
                    onSongClick = { playerViewModel.playSong(it) },
                    onBack = safePopBackStack,
                    currentSongId = currentSong?.id
                )
            }

            composable(
                route = Routes.NowPlaying.route,
                arguments = listOf(navArgument("songId") { type = NavType.LongType }),
                enterTransition = {
                    if (animationsEnabled)
                        slideInVertically(animationSpec = tween(400)) { it } + fadeIn(animationSpec = tween(250))
                    else
                        fadeIn(animationSpec = tween(0))
                },
                exitTransition = {
                    if (animationsEnabled)
                        slideOutVertically(animationSpec = tween(350)) { it } + fadeOut(animationSpec = tween(200))
                    else
                        fadeOut(animationSpec = tween(0))
                },
                popEnterTransition = {
                    if (animationsEnabled)
                        fadeIn(animationSpec = tween(200))
                    else
                        fadeIn(animationSpec = tween(0))
                },
                popExitTransition = {
                    if (animationsEnabled)
                        slideOutVertically(animationSpec = tween(350)) { it } + fadeOut(animationSpec = tween(200))
                    else
                        fadeOut(animationSpec = tween(0))
                }
            ) { _ ->
                val currentPosition by musicPlayer.currentPosition.collectAsStateWithLifecycle()
                val duration by musicPlayer.duration.collectAsStateWithLifecycle()
                val shuffleMode by musicPlayer.shuffleMode.collectAsStateWithLifecycle()
                val repeatMode by musicPlayer.repeatMode.collectAsStateWithLifecycle()

                val playbackSpeed by musicPlayer.playbackSpeed
                    .collectAsStateWithLifecycle()
                val dominantColor by musicPlayer.colorManager.dominantColor
                    .collectAsStateWithLifecycle()
                val auraMode by musicPlayer.colorManager.auraMode
                    .collectAsStateWithLifecycle()
                val fftMagnitudes by musicPlayer.fftVisualizer.fftMagnitudes
                    .collectAsStateWithLifecycle()
                val waveform by musicPlayer.fftVisualizer.waveform
                    .collectAsStateWithLifecycle()
                val beat by musicPlayer.fftVisualizer.beat
                    .collectAsStateWithLifecycle()
                val lyricData by musicPlayer.lyricData
                    .collectAsStateWithLifecycle()
                val sleepTimerActive by musicPlayer.sleepTimerManager.isActive
                    .collectAsStateWithLifecycle()
                val sleepTimerWarning by musicPlayer.sleepTimerManager.warningSeconds
                    .collectAsStateWithLifecycle()
                val showVisualizer by prefs.showVisualizer.collectAsStateWithLifecycle(initialValue = true)

                NowPlayingScreen(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    shuffleMode = shuffleMode,
                    repeatMode = repeatMode,
                    playbackSpeed = playbackSpeed,
                    onPlayPause = { musicPlayer.togglePlayPause() },
                    onNext = { musicPlayer.playNext() },
                    onPrevious = { musicPlayer.playPrevious() },
                    onSeek = { musicPlayer.seekTo(it) },
                    onToggleShuffle = { musicPlayer.toggleShuffle() },
                    onToggleRepeat = { musicPlayer.toggleRepeatMode() },
                    onToggleFavorite = {
                        currentSong?.let { playerViewModel.toggleFavorite(it) }
                    },
                    onBack = safePopBackStack,
                    onQueue = { showQueueSheet = true },
                    onSpeedChange = { musicPlayer.setPlaybackSpeed(it) },
                    sleepTimerActive = sleepTimerActive,
                    sleepTimerWarning = sleepTimerWarning,
                    onSleepTimerClick = {
                        if (sleepTimerActive) {
                            playerViewModel.stopSleepTimer()
                        } else {
                            playerViewModel.startSleepTimer(15)
                        }
                    },
                    dominantColor = dominantColor,
                    gamerMode = gamerMode,
                    auraMode = auraMode,
                    fftMagnitudes = fftMagnitudes,
                    waveform = waveform,
                    beat = beat,
                    animationsEnabled = animationsEnabled,
                    showVisualizer = showVisualizer,
                    lyricData = lyricData
                )
            }

            composable(
                route = Routes.PlaylistDetail.route,
                arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
                val playlist = playlists.find { it.id == playlistId }
                val playlistSongs by libraryViewModel.repository.getPlaylistSongs(playlistId)
                    .collectAsStateWithLifecycle(initialValue = emptyList())

                PlaylistDetailScreen(
                    playlistId = playlistId,
                    playlistName = playlist?.name ?: stringResource(R.string.playlists),
                    playlistDescription = playlist?.description ?: "",
                    songs = playlistSongs,
                    onPlaySong = { song ->
                        val idx = playlistSongs.indexOf(song)
                        if (idx >= 0) {
                            musicPlayer.setQueue(playlistSongs, idx)
                            musicPlayer.play()
                            playerViewModel.startPlaybackService()
                        }
                    },
                    onToggleFavorite = { playerViewModel.toggleFavorite(it) },
                    onDeleteSong = { songToDelete = it },
                    onRemoveSong = { songId ->
                        libraryViewModel.removeSongFromPlaylist(playlistId, songId)
                    },
                    onPlayAll = { playAllSongs(playlistSongs) },
                    onShufflePlay = { shufflePlaySongs(playlistSongs) },
                    onBack = safePopBackStack,
                    onDeletePlaylist = {
                        libraryViewModel.deletePlaylist(playlistId)
                        safePopBackStack()
                    },
                    onReorderSongs = { songIds ->
                        libraryViewModel.reorderPlaylistSongs(playlistId, songIds)
                    },
                    onEditPlaylist = { _, name, desc ->
                        libraryViewModel.updatePlaylistName(playlistId, name, desc)
                    },
                    onExportPlaylist = {
                        libraryViewModel.exportPlaylist(context, playlistId)
                    },
                    currentSongId = currentSong?.id
                )
            }

            composable(Routes.CreatePlaylist.route) {
                CreatePlaylistScreen(
                    onBack = safePopBackStack,
                    onCreatePlaylist = { name, desc ->
                        libraryViewModel.createPlaylist(name, desc)
                        safePopBackStack()
                    }
                )
            }

            composable(Routes.Statistics.route) {
                val listenTime by playerViewModel.totalListeningTime
                    .collectAsStateWithLifecycle(initialValue = 0L)
                val songCount by libraryViewModel.songCount
                    .collectAsStateWithLifecycle(initialValue = 0)
                val artistCount by libraryViewModel.artistCount
                    .collectAsStateWithLifecycle(initialValue = 0)
                val albumCount by libraryViewModel.albumCount
                    .collectAsStateWithLifecycle(initialValue = 0)
                val mostPlayed by libraryViewModel.mostPlayed
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                val topSongs = mostPlayed.take(5)
                val topArtistNames = remember(mostPlayed) {
                    mostPlayed
                        .groupBy { it.artistDisplay }
                        .entries
                        .sortedByDescending { it.value.sumOf { s -> s.playCount } }
                        .take(5)
                        .map { it.key }
                }
                val favoriteSongs by libraryViewModel.favoriteSongs
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                val recentlyPlayed by libraryViewModel.recentlyPlayed
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                StatisticsScreen(
                    totalSongs = songCount,
                    totalArtists = artistCount,
                    totalAlbums = albumCount,
                    totalListeningTimeMinutes = listenTime / 60,
                    topSongs = topSongs,
                    topArtistNames = topArtistNames,
                    favoriteSongs = favoriteSongs,
                    recentlyPlayed = recentlyPlayed,
                    onBack = safePopBackStack
                )
            }

            @Composable
            fun SongListContent(
                songs: List<Song>,
                title: String,
                subtitle: String
            ) {
                SongListScreen(
                    title = title,
                    subtitle = subtitle,
                    songs = songs,
                    onPlaySong = { song ->
                        val idx = songs.indexOf(song)
                        if (idx >= 0) {
                            musicPlayer.setQueue(songs, idx)
                            musicPlayer.play()
                            playerViewModel.startPlaybackService()
                        }
                    },
                    onToggleFavorite = { playerViewModel.toggleFavorite(it) },
                    onSongMoreOptions = { libraryViewModel.showAddToPlaylistDialog(it) },
                    onPlayNext = { playerViewModel.playSongNext(it) },
                    onAddToQueue = { playerViewModel.addToQueue(it) },
                    onDeleteSong = { songToDelete = it },
                    onBack = safePopBackStack,
                    onPlayAll = { playAllSongs(songs) },
                    onShufflePlay = { shufflePlaySongs(songs) },
                    currentSongId = currentSong?.id,
                    animationsEnabled = animationsEnabled
                )
            }

            composable(
                route = Routes.ArtistSongs.route,
                arguments = listOf(navArgument("artistName") { type = NavType.StringType })
            ) { backStackEntry ->
                val artistName = Uri.decode(backStackEntry.arguments?.getString("artistName") ?: return@composable)
                val artistSongs by libraryViewModel.repository.getSongsByArtist(artistName)
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                SongListContent(songs = artistSongs, title = artistName, subtitle = stringResource(R.string.artist))
            }

            composable(
                route = Routes.AlbumSongs.route,
                arguments = listOf(navArgument("albumId") { type = NavType.LongType })
            ) { backStackEntry ->
                val albumId = backStackEntry.arguments?.getLong("albumId") ?: return@composable
                val albumSongs by libraryViewModel.repository.getSongsByAlbum(albumId)
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                val albumTitle = albumSongs.firstOrNull()?.album ?: ""
                SongListContent(songs = albumSongs, title = albumTitle, subtitle = stringResource(R.string.album))
            }

            composable(
                route = Routes.GenreSongs.route,
                arguments = listOf(navArgument("genreName") { type = NavType.StringType })
            ) { backStackEntry ->
                val genreName = Uri.decode(backStackEntry.arguments?.getString("genreName") ?: return@composable)
                val genreSongs by libraryViewModel.repository.getSongsByGenre(genreName)
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                SongListContent(songs = genreSongs, title = genreName, subtitle = stringResource(R.string.genre))
            }

            composable(
                route = Routes.FolderSongs.route,
                arguments = listOf(navArgument("folderPath") { type = NavType.StringType })
            ) { backStackEntry ->
                val folderPath = Uri.decode(backStackEntry.arguments?.getString("folderPath") ?: return@composable)
                val folderSongs by libraryViewModel.repository.getSongsByPathPrefix(folderPath)
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                val folderName = folderPath.substringAfterLast(java.io.File.separator)
                SongListContent(songs = folderSongs, title = folderName, subtitle = stringResource(R.string.folder))
            }
        }

        if (showQueueSheet) {
            val queue by musicPlayer.queue.collectAsStateWithLifecycle()
            val currentIdx by musicPlayer.currentIndex.collectAsStateWithLifecycle()
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.queue),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (queue.isNotEmpty()) {
                            TextButton(onClick = { musicPlayer.clearQueue() }) {
                                Text(stringResource(R.string.clear_all), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    if (queue.isEmpty()) {
                        Text(
                            text = stringResource(R.string.queue_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    } else {
                        LazyColumn {
                            itemsIndexed(queue, key = { index, song -> "${index}_${song.id}" }) { index, song ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconButton(
                                            onClick = {
                                                if (index > 0) musicPlayer.moveInQueue(index, index - 1)
                                            },
                                            enabled = index > 0,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.KeyboardArrowUp,
                                                contentDescription = stringResource(R.string.move_up),
                                                tint = if (index > 0) MaterialTheme.colorScheme.onSurfaceVariant
                                                       else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                if (index < queue.size - 1) musicPlayer.moveInQueue(index, index + 1)
                                            },
                                            enabled = index < queue.size - 1,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.KeyboardArrowDown,
                                                contentDescription = stringResource(R.string.move_down),
                                                tint = if (index < queue.size - 1) MaterialTheme.colorScheme.onSurfaceVariant
                                                       else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    com.auramusic.ui.components.SongItem(
                                        song = song,
                                        onClick = {
                                            musicPlayer.seekToMediaItem(index)
                                            musicPlayer.play()
                                            showQueueSheet = false
                                        },
                                        onPlayNext = { musicPlayer.playSongNext(song); showQueueSheet = false },
                                        onAddToQueue = { musicPlayer.addToQueue(song) },
                                        onFavoriteToggle = { playerViewModel.toggleFavorite(song) },
                                        onAddToPlaylist = { libraryViewModel.showAddToPlaylistDialog(song.id) },
                                        onDeleteSong = { songToDelete = song },
                                        isCurrentlyPlaying = index == currentIdx
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
}
