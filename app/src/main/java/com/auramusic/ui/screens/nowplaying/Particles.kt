package com.auramusic.ui.screens.nowplaying

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import com.auramusic.player.AuraMode
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

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

private data class BurstParticle(
    val startX: Float,
    val startY: Float,
    val angle: Float,
    val speed: Float,
    val size: Float,
    val hue: Float,
    val createdAt: Long
)

@Composable
fun AmbientParticlesOverlay(dominantColor: Color, auraMode: AuraMode = AuraMode.DEFAULT, beat: Boolean = false, fftMagnitudes: List<Float> = List(6) { 0f }) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isResumed = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (!isResumed) return

    val beatBoost by animateFloatAsState(
        targetValue = if (beat) 1f else 0f,
        animationSpec = tween(200),
        label = "ambient_beat"
    )
    val energy = fftMagnitudes.sum() / fftMagnitudes.size.coerceAtLeast(1)

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
        val pulse = 1f + beatBoost * 2f + energy * 3f
        particles.forEach { p ->
            val angleRad = p.baseAngleRad + time * p.speed * 30f
            val x = ((p.startX + cos(angleRad) * 0.2f + time * p.driftX) % 1f + 1f) % 1f
            val y = ((p.startY + sin(angleRad) * 0.15f + time * p.driftY) % 1f + 1f) % 1f
            val alpha = ((cos(angleRad * 2f) * 0.15f + 0.25f)).coerceIn(0.05f, 0.35f) * (1f + beatBoost)

            drawCircle(
                color = dominantColor.copy(alpha = alpha),
                radius = p.size.dp.toPx() * pulse,
                center = Offset(x * size.width, y * size.height)
            )
        }
    }
}

@Composable
fun GamerParticlesOverlay(beat: Boolean = false, fftMagnitudes: List<Float> = List(6) { 0f }) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isResumed = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (!isResumed) return

    val beatBoost by animateFloatAsState(
        targetValue = if (beat) 1f else 0f,
        animationSpec = tween(200),
        label = "beat_boost"
    )
    val energy = fftMagnitudes.sum() / fftMagnitudes.size.coerceAtLeast(1)

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

    val bursts = remember { mutableStateListOf<BurstParticle>() }

    LaunchedEffect(beat) {
        if (beat && energy > 0.3f) {
            val count = 6 + (energy * 8).toInt().coerceAtMost(12)
            repeat(count) {
                bursts.add(
                    BurstParticle(
                        startX = Random.nextFloat(),
                        startY = Random.nextFloat(),
                        angle = (Random.nextFloat() * 2f * PI).toFloat(),
                        speed = 0.004f + Random.nextFloat() * 0.008f,
                        size = 4f + Random.nextFloat() * 6f,
                        hue = Random.nextFloat() * 360f,
                        createdAt = System.nanoTime()
                    )
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            val now = System.nanoTime()
            bursts.removeAll { (now - it.createdAt) / 1_000_000 > 1200 }
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
            val beatScale = 1f + beatBoost * 1.5f
            val fftPulse = 1f + energy * 2f

            val now = System.nanoTime()

            bursts.forEach { b ->
                val elapsed = ((now - b.createdAt) / 1_000_000).toFloat()
                if (elapsed > 1200f) return@forEach
                val progress = elapsed / 1200f
                val dist = progress * b.speed * 0.5f
                val bx = ((b.startX + cos(b.angle) * dist) % 1f + 1f) % 1f
                val by = ((b.startY + sin(b.angle) * dist) % 1f + 1f) % 1f
                val alpha = (1f - progress).coerceIn(0f, 1f) * 0.8f
                val radius = b.size.dp.toPx() * (1f + progress * 2f) * beatScale * fftPulse

                drawCircle(
                    color = Color.hsl(b.hue, 1f, 0.6f, alpha),
                    radius = radius,
                    center = Offset(bx * size.width, by * size.height)
                )
            }

            particles.forEach { p ->
                val t = time * p.speed * (1f + beatBoost * 2f)
                val angleRad1 = p.baseAngleRad + t * 60f
                val angleRad2 = p.baseAngleRad + t * 45f
                val x = ((p.startX + cos(angleRad1) * 0.35f + t * 0.12f) % 1f + 1f) % 1f
                val y = ((p.startY + sin(angleRad2) * 0.35f + t * 0.08f) % 1f + 1f) % 1f
                val hue = (p.hueOffset + time * 0.8f) % 360f
                val alpha = ((cos(angleRad1 + p.hueOffset) * 0.2f + 0.5f)).coerceIn(0.15f, 0.7f) * (1f + beatBoost * 0.5f)
                val radius = p.size.dp.toPx() * beatScale * fftPulse

                if (p.trail) {
                    drawCircle(
                        color = Color.hsl(hue, 1f, 0.5f, alpha * 0.15f * (1f + beatBoost)),
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
