package com.auramusic.ui.screens.nowplaying

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.auramusic.R
import com.auramusic.player.AuraMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.os.Build
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.unit.sp
import com.auramusic.domain.model.Song
import com.auramusic.ui.components.AlbumArtImage
import com.auramusic.ui.theme.*
import com.auramusic.util.getAlbumArtUri
import com.auramusic.util.formatDuration
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.absoluteValue
import kotlin.random.Random

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Suppress("UnusedContentLambdaTargetStateParameter")
@Composable
fun NowPlayingScreen(
    currentSong: Song?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    shuffleMode: Boolean,
    repeatMode: Int,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onQueue: () -> Unit,
    sleepTimerActive: Boolean = false,
    sleepTimerWarning: Long = 0L,
    onSleepTimerClick: () -> Unit = {},
    dominantColor: Color = NeonPurple,
    gamerMode: Boolean = false,
    auraMode: AuraMode = AuraMode.DEFAULT,
    fftMagnitudes: List<Float> = List(6) { 0f },
    waveform: List<Float> = List(48) { 0f },
    animationsEnabled: Boolean = true
) {
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
                    Brush.verticalGradient(
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
                GamerParticlesOverlay()
            } else if (isPlaying) {
                AmbientParticlesOverlay(dominantColor = dominantColor, auraMode = auraMode)
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
                gamerMode = gamerMode
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
                        text = "Sleep timer: ${sleepTimerWarning}s remaining",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            val swipeThreshold = with(LocalDensity.current) { 80.dp.toPx() }
            var dragOffsetX by remember { mutableStateOf(0f) }
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
                val density = LocalDensity.current
                var previousSong by remember { mutableStateOf<Song?>(null) }
                SideEffect { previousSong = currentSong }

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
                ) { targetSongId ->
                    val displaySong = remember(targetSongId) {
                        when (targetSongId) {
                            currentSong?.id -> currentSong
                            previousSong?.id -> previousSong
                            else -> null
                        }
                    }
                    if (displaySong != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(x = with(density) { swipeProgress.toDp() })
                                .graphicsLayer {
                                    val progress = (swipeProgress / swipeThreshold).coerceIn(-1f, 1f)
                                    val absProgress = progress.absoluteValue
                                    scaleX = 1f - absProgress * 0.05f
                                    scaleY = 1f - absProgress * 0.05f
                                    alpha = 1f - absProgress * 0.15f
                                }
                                .pointerInput(displaySong.id) {
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
                            AlbumArtSection(currentSong = displaySong, dominantColor = dominantColor, gamerMode = gamerMode)
                            Spacer(modifier = Modifier.height(24.dp))
                            SongInfoSection(currentSong = displaySong, gamerMode = gamerMode)
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

            AnimatedVisibility(
                visible = currentSong != null,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 100)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AudioVisualizer(isActive = isPlaying, dominantColor = dominantColor, gamerMode = gamerMode, auraMode = auraMode, fftMagnitudes = fftMagnitudes)
                    if (animationsEnabled && waveform.any { it > 0.01f }) {
                        Spacer(modifier = Modifier.height(6.dp))
                        WaveformVisualizer(waveform = waveform, dominantColor = dominantColor, gamerMode = gamerMode)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    GradientSeekbar(
                        currentPosition = currentPosition,
                        duration = duration,
                        onSeek = onSeek,
                        dominantColor = dominantColor
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
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onToggleShuffle = onToggleShuffle,
                        onToggleRepeat = onToggleRepeat,
                        dominantColor = dominantColor,
                        gamerMode = gamerMode
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NowPlayingTopBar(
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onQueue: () -> Unit,
    sleepTimerActive: Boolean = false,
    onSleepTimerClick: () -> Unit = {},
    dominantColor: Color = NeonPurple,
    gamerMode: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp)
            )
        }

        if (gamerMode) {
            val gamerHue = rememberInfiniteTransition(label = "gamer_badge").animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
                label = "gamer_badge_hue"
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.hsl(gamerHue.value, 1f, 0.6f, 0.2f),
                border = BorderStroke(1.dp, Color.hsl(gamerHue.value, 1f, 0.6f, 0.6f)),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = "GAMER",
                    color = Color.hsl(gamerHue.value, 1f, 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = stringResource(R.string.toggle_favorite),
                tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }

        IconButton(onClick = onQueue) {
            Icon(
                imageVector = Icons.Rounded.QueueMusic,
                contentDescription = stringResource(R.string.queue),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }
        IconButton(onClick = onSleepTimerClick) {
            Icon(
                imageVector = if (sleepTimerActive) Icons.Rounded.Timer else Icons.Rounded.TimerOff,
                contentDescription = stringResource(R.string.sleep_timer),
                tint = if (sleepTimerActive) dominantColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun AlbumArtSection(currentSong: Song, dominantColor: Color = NeonPurple, gamerMode: Boolean = false) {
    val context = LocalContext.current
    val albumArtUri = remember(currentSong.albumId) { context.getAlbumArtUri(currentSong.albumId) }
    val infiniteTransition = rememberInfiniteTransition(label = "rgb_border")
    val hue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hue"
    )

    val borderBrush = if (gamerMode) {
        Brush.sweepGradient(
            colors = listOf(
                Color.hsl(hue, 1f, 0.6f),
                Color.hsl((hue + 60f) % 360f, 1f, 0.6f),
                Color.hsl((hue + 120f) % 360f, 1f, 0.6f),
                Color.hsl((hue + 180f) % 360f, 1f, 0.6f),
                Color.hsl((hue + 240f) % 360f, 1f, 0.6f),
                Color.hsl((hue + 300f) % 360f, 1f, 0.6f),
                Color.hsl(hue, 1f, 0.6f)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(dominantColor.copy(alpha = 0.3f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
        )
    }

    Box(
        modifier = Modifier.size(310.dp),
        contentAlignment = Alignment.Center
    ) {
        if (gamerMode) {
            val glowHue = rememberInfiniteTransition(label = "glow_pulse").animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
                label = "glow_hue"
            )
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.hsl(glowHue.value, 0.8f, 0.5f, 0.15f))
                    .graphicsLayer {
                        scaleX = 1.05f
                        scaleY = 1.05f
                    }
            )
        }

        Box(
            modifier = Modifier
                .size(310.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            dominantColor.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = if (gamerMode) 2.dp else 1.dp,
                    brush = borderBrush,
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            AlbumArtImage(
                uri = albumArtUri,
                contentDescription = currentSong.title,
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(dominantColor.copy(alpha = 0.7f), dominantColor)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentSong.title.firstOrNull()?.uppercase() ?: "\u266A",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun SongInfoSection(currentSong: Song, gamerMode: Boolean = false) {
    val gamerHue = if (gamerMode) {
        rememberInfiniteTransition(label = "gamer_text").animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
            label = "gamer_text_hue"
        )
    } else null

    val titleColor = if (gamerMode && gamerHue != null) {
        Color.hsl(gamerHue.value, 1f, 0.7f)
    } else MaterialTheme.colorScheme.onBackground

    val artistColor = if (gamerMode && gamerHue != null) {
        Color.hsl((gamerHue.value + 120f) % 360f, 0.8f, 0.6f)
    } else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = currentSong.title,
            style = MaterialTheme.typography.headlineSmall,
            color = titleColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = currentSong.artistDisplay,
            style = MaterialTheme.typography.bodyLarge,
            color = artistColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WaveformVisualizer(waveform: List<Float>, dominantColor: Color, gamerMode: Boolean) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
    ) {
        val barWidth = size.width / waveform.size
        val halfHeight = size.height / 2f
        val path = Path()
        waveform.forEachIndexed { index, value ->
            val x = index * barWidth
            val h = (value * halfHeight).coerceIn(1f, halfHeight)
            if (index == 0) {
                path.moveTo(x, halfHeight - h)
            } else {
                path.lineTo(x, halfHeight - h)
            }
        }
        for (i in waveform.indices.reversed()) {
            val x = i * barWidth
            val h = (waveform[i] * halfHeight).coerceIn(1f, halfHeight)
            path.lineTo(x, halfHeight + h)
        }
        path.close()
        drawPath(
            path,
            brush = Brush.horizontalGradient(
                listOf(
                    primaryColor.copy(alpha = 0.6f),
                    dominantColor.copy(alpha = 0.4f)
                )
            )
        )
    }
}

@Composable
private fun AudioVisualizer(isActive: Boolean, dominantColor: Color = NeonPurple, gamerMode: Boolean = false, auraMode: AuraMode = AuraMode.DEFAULT, fftMagnitudes: List<Float> = List(6) { 0f }) {
    val barCount = if (auraMode == AuraMode.CALM) 4 else 6
    val hasFftData = fftMagnitudes.any { it > 0.05f }

    val baseSpeed = when (auraMode) {
        AuraMode.ENERGY -> 150
        AuraMode.CALM -> 600
        AuraMode.NEON -> 250
        AuraMode.DEFAULT -> 350
    }

    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    val restHeights = remember { List(barCount) { mutableStateOf(0.3f) } }
    val animatedHeights: List<State<Float>> = if (isActive) {
        List(barCount) { i ->
            infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = if (gamerMode) 200 + i * 50 else baseSpeed + i * 80,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "visualizer_bar_$i"
            )
        }
    } else {
        restHeights
    }

    Row(
        modifier = Modifier.height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        animatedHeights.forEachIndexed { index, animHeight ->
            val fftHeight = if (hasFftData && index < fftMagnitudes.size) {
                fftMagnitudes[index].coerceIn(0.05f, 1f)
            } else animHeight.value

            val barColor = when {
                gamerMode && isActive -> {
                    val hue = (index * 60f + animHeight.value * 360f) % 360f
                    Color.hsl(hue, 1f, 0.6f)
                }
                auraMode == AuraMode.ENERGY && isActive -> {
                    Color.hsl(350f + animHeight.value * 20f, 0.8f, 0.6f)
                }
                auraMode == AuraMode.CALM && isActive -> {
                    Color.hsl(200f + animHeight.value * 40f, 0.6f, 0.6f)
                }
                auraMode == AuraMode.NEON -> {
                    val hue = (index * 72f + animHeight.value * 180f) % 360f
                    Color.hsl(hue, 1f, 0.6f)
                }
                else -> dominantColor
            }

            Box(
                modifier = Modifier
                    .width(if (gamerMode || auraMode == AuraMode.NEON) 4.dp else 3.dp)
                    .height((4.dp + (fftHeight * 28).dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(barColor, MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f))
                        )
                    )
            )
        }
    }
}

@Composable
private fun GradientSeekbar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    dominantColor: Color = NeonPurple,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    thumbColor: Color = MaterialTheme.colorScheme.onBackground
) {
    val progress = if (duration > 0) {
        (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(duration) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val p = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((p * duration).toLong())
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val p = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeek((p * duration).toLong())
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasHeight = size.height
            val canvasWidth = size.width
            val trackHeight = 4.dp.toPx()
            val trackOffset = (canvasHeight - trackHeight) / 2f
            val progressWidth = canvasWidth * progress
            val thumbOuterRadius = 8.dp.toPx()
            val thumbInnerRadius = 5.dp.toPx()

            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, trackOffset),
                size = Size(canvasWidth, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f)
            )

            if (progressWidth > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(dominantColor, secondaryColor)
                    ),
                    topLeft = Offset(0f, trackOffset),
                    size = Size(progressWidth, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f)
                )
            }

            drawCircle(
                color = thumbColor,
                radius = thumbOuterRadius,
                center = Offset(progressWidth, canvasHeight / 2f)
            )
            drawCircle(
                color = dominantColor,
                radius = thumbInnerRadius,
                center = Offset(progressWidth, canvasHeight / 2f)
            )
        }
    }
}

@Composable
private fun TimeLabels(
    currentPosition: Long,
    duration: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = currentPosition.formatDuration(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Text(
            text = duration.formatDuration(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ControlsRow(
    isPlaying: Boolean,
    shuffleMode: Boolean,
    repeatMode: Int,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    dominantColor: Color = NeonPurple,
    gamerMode: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onToggleShuffle) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = stringResource(R.string.shuffle),
                tint = if (shuffleMode) dominantColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = stringResource(R.string.previous),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        val hueTransition = rememberInfiniteTransition(label = "play_btn")
        val playHue by hueTransition.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
            label = "play_hue"
        )
        val playBtnColor = if (gamerMode) Color.hsl(playHue, 1f, 0.6f) else dominantColor
        val playGlowAlpha by hueTransition.animateFloat(
            initialValue = 0.2f, targetValue = 0.5f,
            animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
            label = "play_glow"
        )

        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            if (gamerMode) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(playBtnColor.copy(alpha = playGlowAlpha))
                )
            }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(playBtnColor)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.next),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        IconButton(onClick = onToggleRepeat) {
            Icon(
                imageVector = Icons.Rounded.Repeat,
                contentDescription = stringResource(R.string.repeat),
                tint = if (repeatMode != 0) dominantColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun GamerParticlesOverlay() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val visible = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    if (!visible) return

    val particles = remember {
        List(45) {
            GamerParticle(
                startX = Random.nextFloat(),
                startY = Random.nextFloat(),
                baseAngleRad = (Random.nextFloat() * 2f * PI).toFloat(),
                speed = 0.005f + Random.nextFloat() * 0.015f,
                size = 2f + Random.nextFloat() * 5f,
                hueOffset = Random.nextFloat() * 360f,
                trail = Random.nextFloat() > 0.5f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "gamer_particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                val t = time * p.speed
                val angleRad1 = p.baseAngleRad + t * 60f
                val angleRad2 = p.baseAngleRad + t * 45f
                val x = ((p.startX + cos(angleRad1) * 0.35f + t * 0.12f) % 1f + 1f) % 1f
                val y = ((p.startY + sin(angleRad2) * 0.35f + t * 0.08f) % 1f + 1f) % 1f
                val hue = (p.hueOffset + time * 0.8f) % 360f
                val alpha = ((cos(angleRad1 + p.hueOffset) * 0.2f + 0.5f)).coerceIn(0.15f, 0.7f)
                val radius = p.size.dp.toPx()

                if (p.trail) {
                    drawCircle(
                        color = Color.hsl(hue, 1f, 0.5f, alpha * 0.15f),
                        radius = radius * 2.5f,
                        center = Offset(x * size.width, y * size.height)
                    )
                }

                drawCircle(
                    color = Color.hsl(hue, 1f, 0.7f, alpha),
                    radius = radius,
                    center = Offset(x * size.width, y * size.height)
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.04f)
        ) {
            val step = 4.dp.toPx()
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = Color.White,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += step
            }
        }
    }
}

@Composable
private fun AmbientParticlesOverlay(dominantColor: Color, auraMode: AuraMode = AuraMode.DEFAULT) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val visible = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    if (!visible) return

    val particleCount = when (auraMode) {
        AuraMode.ENERGY -> 8
        AuraMode.CALM -> 4
        AuraMode.NEON -> 6
        AuraMode.DEFAULT -> 5
    }

    val particles = remember(particleCount) {
        List(particleCount) {
            AmbientParticle(
                startX = Random.nextFloat(),
                startY = Random.nextFloat(),
                baseAngleRad = (Random.nextFloat() * 2f * PI).toFloat(),
                speed = 0.001f + Random.nextFloat() * 0.003f,
                size = 1f + Random.nextFloat() * 2f,
                driftX = (Random.nextFloat() - 0.5f) * 0.02f,
                driftY = -(0.002f + Random.nextFloat() * 0.004f)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ambient_particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val angleRad = p.baseAngleRad + time * p.speed * 30f
            val x = ((p.startX + cos(angleRad) * 0.2f + time * p.driftX) % 1f + 1f) % 1f
            val y = ((p.startY + sin(angleRad) * 0.15f + time * p.driftY) % 1f + 1f) % 1f
            val alpha = ((cos(angleRad * 2f) * 0.15f + 0.25f)).coerceIn(0.05f, 0.35f)

            drawCircle(
                color = dominantColor.copy(alpha = alpha),
                radius = p.size.dp.toPx(),
                center = Offset(x * size.width, y * size.height)
            )
        }
    }
}

private data class AmbientParticle(
    val startX: Float,
    val startY: Float,
    val baseAngleRad: Float,
    val speed: Float,
    val size: Float,
    val driftX: Float,
    val driftY: Float
)

private data class GamerParticle(
    val startX: Float,
    val startY: Float,
    val baseAngleRad: Float,
    val speed: Float,
    val size: Float,
    val hueOffset: Float,
    val trail: Boolean = false
)
