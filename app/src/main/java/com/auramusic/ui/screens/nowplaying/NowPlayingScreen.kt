package com.auramusic.ui.screens.nowplaying

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.R
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import android.os.Build
import com.auramusic.domain.model.Song
import com.auramusic.player.AuraMode
import com.auramusic.ui.components.AlbumArtImage
import com.auramusic.ui.components.LyricsDisplay
import com.auramusic.ui.components.rememberWindowAdaptiveInfo
import com.auramusic.ui.theme.*
import com.auramusic.util.LyricData
@Composable
fun NowPlayingScreen(
    currentSong: Song?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    shuffleMode: Boolean,
    repeatMode: Int,
    playbackSpeed: Float = 1f,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onQueue: () -> Unit,
    onSpeedChange: (Float) -> Unit = {},
    sleepTimerActive: Boolean = false,
    sleepTimerWarning: Long = 0L,
    onSleepTimerClick: () -> Unit = {},
    dominantColor: Color = NeonPurple,
    gamerMode: Boolean = false,
    auraMode: AuraMode = AuraMode.DEFAULT,
    fftMagnitudes: List<Float> = List(6) { 0f },
    waveform: List<Float> = List(48) { 0f },
    beat: Boolean = false,
    animationsEnabled: Boolean = true,
    showVisualizer: Boolean = true,
    lyricData: LyricData? = null
) {
    var showLyrics by remember { mutableStateOf(false) }
    val adaptiveInfo = rememberWindowAdaptiveInfo()
    val isLandscape = adaptiveInfo.isLandscape && adaptiveInfo.screenWidthDp >= 480

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (!gamerMode && Build.VERSION.SDK_INT >= 31 && animationsEnabled) Modifier.blur(16.dp) else Modifier)
                .background(
                    if (isLandscape) Brush.horizontalGradient(
                        colors = listOf(
                            dominantColor.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        )
                    ) else Brush.verticalGradient(
                        colors = listOf(
                            dominantColor.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        )
                    )
                )
        )

        if (animationsEnabled) {
            if (gamerMode) {
                GamerParticlesOverlay(beat = beat, fftMagnitudes = fftMagnitudes)
            } else if (isPlaying) {
                AmbientParticlesOverlay(dominantColor = dominantColor, auraMode = auraMode, beat = beat, fftMagnitudes = fftMagnitudes)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            NowPlayingTopBar(
                isFavorite = currentSong?.isFavorite ?: false,
                onBack = onBack,
                onToggleFavorite = onToggleFavorite,
                onQueue = onQueue,
                sleepTimerActive = sleepTimerActive,
                onSleepTimerClick = onSleepTimerClick,
                dominantColor = dominantColor,
                gamerMode = gamerMode,
                hasLyrics = lyricData != null && lyricData.lines.isNotEmpty(),
                showLyrics = showLyrics,
                onToggleLyrics = { showLyrics = !showLyrics }
            )

            if (sleepTimerWarning > 0) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = stringResource(R.string.sleep_timer_warning, sleepTimerWarning),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (isLandscape && currentSong != null) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AlbumArtSection(currentSong = currentSong, dominantColor = dominantColor, gamerMode = gamerMode, beat = beat)
                        Spacer(modifier = Modifier.height(16.dp))
                        SongInfoSection(currentSong = currentSong, gamerMode = gamerMode)
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    NowPlayingControls(
                        isPlaying = isPlaying,
                        showVisualizer = showVisualizer,
                        animationsEnabled = animationsEnabled,
                        waveform = waveform,
                        currentPosition = currentPosition,
                        duration = duration,
                        onSeek = onSeek,
                        shuffleMode = shuffleMode,
                        repeatMode = repeatMode,
                        playbackSpeed = playbackSpeed,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onToggleShuffle = onToggleShuffle,
                        onToggleRepeat = onToggleRepeat,
                        onSpeedChange = onSpeedChange,
                        dominantColor = dominantColor,
                        gamerMode = gamerMode,
                        auraMode = auraMode,
                        fftMagnitudes = fftMagnitudes
                    )
                }
            } else {
                val swipeThreshold = with(LocalDensity.current) { 80.dp.toPx() }
                var dragOffsetX by remember { mutableFloatStateOf(0f) }
                var isDragging by remember { mutableStateOf(false) }
                val swipeProgress by animateFloatAsState(
                    targetValue = if (isDragging) dragOffsetX else 0f,
                    animationSpec = spring(
                        dampingRatio = 0.7f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "swipe_offset"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (showLyrics && lyricData != null) {
                        LyricsDisplay(
                            lines = lyricData.lines,
                            currentPositionMs = currentPosition,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val density = LocalDensity.current
                        AnimatedContent(
                            targetState = currentSong?.id ?: -1L,
                            transitionSpec = {
                                val direction = if (targetState > initialState) -1 else 1
                                val animOffset = with(density) { 120.dp.toPx() * direction }
                                slideInHorizontally(
                                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                                    initialOffsetX = { animOffset.toInt() }
                                ) togetherWith slideOutHorizontally(
                                    animationSpec = tween(250),
                                    targetOffsetX = { -animOffset.toInt() }
                                ) using SizeTransform(clip = false)
                            },
                            label = "now_playing_content"
                        ) { _ ->
                            if (currentSong != null) {
                                val song = currentSong
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset { IntOffset(swipeProgress.roundToInt(), 0) }
                                        .graphicsLayer {
                                            val progress = (swipeProgress / swipeThreshold).coerceIn(-1f, 1f)
                                            val absProgress = progress.absoluteValue
                                            scaleX = 1f - absProgress * 0.05f
                                            scaleY = 1f - absProgress * 0.05f
                                            alpha = 1f - absProgress * 0.15f
                                        }
                                        .pointerInput(song.id) {
                                            var totalDrag = 0f
                                            detectHorizontalDragGestures(
                                                onDragStart = {
                                                    isDragging = true
                                                },
                                                onDragEnd = {
                                                    isDragging = false
                                                    if (totalDrag < -swipeThreshold) {
                                                        dragOffsetX = 0f
                                                        onNext()
                                                    } else if (totalDrag > swipeThreshold) {
                                                        dragOffsetX = 0f
                                                        onPrevious()
                                                    } else {
                                                        dragOffsetX = 0f
                                                    }
                                                    totalDrag = 0f
                                                },
                                                onDragCancel = {
                                                    isDragging = false
                                                    totalDrag = 0f
                                                    dragOffsetX = 0f
                                                },
                                                onHorizontalDrag = { _, dragAmount ->
                                                    totalDrag = (totalDrag + dragAmount).coerceIn(
                                                        -swipeThreshold * 2,
                                                        swipeThreshold * 2
                                                    )
                                                    dragOffsetX = totalDrag
                                                }
                                            )
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    AlbumArtSection(currentSong = song, dominantColor = dominantColor, gamerMode = gamerMode, beat = beat)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    SongInfoSection(currentSong = song, gamerMode = gamerMode)
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.no_track_selected),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = currentSong != null,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = 100)),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    NowPlayingControls(
                        isPlaying = isPlaying,
                        showVisualizer = showVisualizer,
                        animationsEnabled = animationsEnabled,
                        waveform = waveform,
                        currentPosition = currentPosition,
                        duration = duration,
                        onSeek = onSeek,
                        shuffleMode = shuffleMode,
                        repeatMode = repeatMode,
                        playbackSpeed = playbackSpeed,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onToggleShuffle = onToggleShuffle,
                        onToggleRepeat = onToggleRepeat,
                        onSpeedChange = onSpeedChange,
                        dominantColor = dominantColor,
                        gamerMode = gamerMode,
                        auraMode = auraMode,
                        fftMagnitudes = fftMagnitudes,
                        horizontalPadding = 24.dp,
                        topSpacer = 12.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NowPlayingControls(
    isPlaying: Boolean,
    showVisualizer: Boolean,
    animationsEnabled: Boolean,
    waveform: List<Float>,
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    shuffleMode: Boolean,
    repeatMode: Int,
    playbackSpeed: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    dominantColor: Color,
    gamerMode: Boolean,
    auraMode: AuraMode,
    fftMagnitudes: List<Float>,
    horizontalPadding: Dp = 24.dp,
    topSpacer: Dp = 12.dp
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showVisualizer) {
            AudioVisualizer(isActive = isPlaying, dominantColor = dominantColor, gamerMode = gamerMode, auraMode = auraMode, fftMagnitudes = fftMagnitudes)
        }
        if (showVisualizer && animationsEnabled && waveform.any { it > 0.01f }) {
            Spacer(modifier = Modifier.height(6.dp))
            WaveformVisualizer(waveform = waveform, dominantColor = dominantColor, gamerMode = gamerMode)
        }
        Spacer(modifier = Modifier.height(topSpacer))
        GradientSeekbar(
            currentPosition = currentPosition,
            duration = duration,
            onSeek = onSeek,
            dominantColor = dominantColor,
            isPlaying = isPlaying
        )
        Spacer(modifier = Modifier.height(6.dp))
        TimeLabels(
            currentPosition = currentPosition,
            duration = duration
        )
        Spacer(modifier = Modifier.height(20.dp))
        ControlsRow(
            isPlaying = isPlaying,
            shuffleMode = shuffleMode,
            repeatMode = repeatMode,
            playbackSpeed = playbackSpeed,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            onToggleShuffle = onToggleShuffle,
            onToggleRepeat = onToggleRepeat,
            onSpeedChange = onSpeedChange,
            dominantColor = dominantColor,
            gamerMode = gamerMode
        )
    }
}


