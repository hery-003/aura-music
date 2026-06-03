package com.auramusic.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val NeonPurple = Color(0xFF8B5CF6)
private val ElectricBlue = Color(0xFF3B82F6)

private data class Orb(
    val size: Dp,
    val speed: Float,
    val phase: Float,
    val color: Color,
    val alpha: Float
)

@Suppress("unused")
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    waitForPermissions: Boolean = true,
    animationsEnabled: Boolean = true
) {
    var stage by remember { mutableIntStateOf(0) }
    var navigationHandled by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    val auroraOffsetX = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_x"
    )

    val auroraOffsetY = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_y"
    )

    val glowPulse = infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    val glowScale = infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    val orbs = remember {
        List(5) {
            Orb(
                size = (20 + Random.nextInt(60)).dp,
                speed = 0.3f + Random.nextFloat() * 0.7f,
                phase = Random.nextFloat() * 6.28f,
                color = if (it % 2 == 0) NeonPurple else ElectricBlue,
                alpha = 0.08f + Random.nextFloat() * 0.12f
            )
        }
    }

    val orbAnimations = orbs.mapIndexed { index, orb ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000 + (orb.speed * 4000).toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "orb_$index"
        )
    }

    val logoAlpha = animateFloatAsState(
        targetValue = if (stage >= 1) 1f else 0f,
        animationSpec = if (animationsEnabled) tween(600, easing = EaseOutCubic) else tween(0),
        label = "logo_alpha"
    )

    val logoScale = animateFloatAsState(
        targetValue = if (stage >= 1) 1f else 0.3f,
        animationSpec = if (animationsEnabled) tween(800, easing = EaseOutBack) else tween(0),
        label = "logo_scale"
    )

    val titleAlpha = animateFloatAsState(
        targetValue = if (stage >= 2) 1f else 0f,
        animationSpec = if (animationsEnabled) tween(700, easing = EaseOutCubic) else tween(0),
        label = "title_alpha"
    )

    val titleOffset = animateFloatAsState(
        targetValue = if (stage >= 2) 0f else 20f,
        animationSpec = if (animationsEnabled) tween(700, easing = EaseOutCubic) else tween(0),
        label = "title_offset"
    )

    val subtitleAlpha = animateFloatAsState(
        targetValue = if (stage >= 3) 1f else 0f,
        animationSpec = if (animationsEnabled) tween(700, easing = EaseOutCubic) else tween(0),
        label = "subtitle_alpha"
    )

    val subtitleOffset = animateFloatAsState(
        targetValue = if (stage >= 3) 0f else 15f,
        animationSpec = if (animationsEnabled) tween(700, easing = EaseOutCubic) else tween(0),
        label = "subtitle_offset"
    )

    val taglineAlpha = animateFloatAsState(
        targetValue = if (stage >= 4) 1f else 0f,
        animationSpec = if (animationsEnabled) tween(800, easing = EaseOutCubic) else tween(0),
        label = "tagline_alpha"
    )

    val bottomLineAlpha = animateFloatAsState(
        targetValue = if (stage >= 5) 1f else 0f,
        animationSpec = if (animationsEnabled) tween(600, easing = EaseOutCubic) else tween(0),
        label = "bottom_line_alpha"
    )

    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val containerSize = windowInfo.containerSize
    val screenWidthDp = with(density) { containerSize.width.toDp() }
    val screenHeightDp = with(density) { containerSize.height.toDp() }

    var currentWaitForPermissions by remember { mutableStateOf(waitForPermissions) }
    LaunchedEffect(waitForPermissions) {
        currentWaitForPermissions = waitForPermissions
    }

    @Suppress("UNUSED_VALUE")
    LaunchedEffect(Unit) {
        if (!animationsEnabled) {
            stage = 5
            delay(300)
        } else {
            delay(200)
            stage = 1
            delay(400)
            stage = 2
            delay(350)
            stage = 3
            delay(350)
            stage = 4
            delay(300)
            stage = 5
            delay(900)
        }

        if (!currentWaitForPermissions) {
            try {
                withTimeout(5000L) {
                    snapshotFlow { currentWaitForPermissions }.first { it }
                }
            } catch (_: Exception) { }
        }

        if (!navigationHandled) {
            navigationHandled = true
            onSplashFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080510))
    ) {
        // Animated aurora gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1A0A2E),
                            Color(0xFF0D0618),
                            Color(0xFF050208),
                            Color.Black
                        ),
                        center = Offset(
                            auroraOffsetX.value * 0.3f + 0.35f,
                            auroraOffsetY.value * 0.3f + 0.35f
                        ),
                        radius = 1.5f
                    )
                )
        )

        // Secondary aurora layer (shifted)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.4f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonPurple.copy(alpha = 0.15f),
                            ElectricBlue.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = Offset(
                            auroraOffsetX.value * 0.5f + 0.2f,
                            auroraOffsetY.value * 0.5f + 0.7f
                        ),
                        radius = 1.2f
                    )
                )
        )

        // Floating orbs
        orbs.forEachIndexed { index, orb ->
            val animValue = orbAnimations[index].value
            val angle = animValue * 6.28f + orb.phase
            val radius = 0.15f + orb.speed * 0.15f
            val centerX = 0.5f + cos(angle) * radius
            val centerY = 0.5f + sin(angle * 0.7f + orb.phase) * radius

            Box(
                modifier = Modifier
                    .offset(
                        x = screenWidthDp * (centerX - 0.5f) * 0.8f,
                        y = screenHeightDp * (centerY - 0.5f)
                    )
                    .size(orb.size)
                    .alpha(orb.alpha * glowPulse.value)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                orb.color.copy(alpha = 0.25f),
                                orb.color.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Outer glow ring
        Box(
            modifier = Modifier
                .size(340.dp)
                .scale(glowScale.value)
                .alpha(glowPulse.value * 0.2f)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonPurple.copy(alpha = 0.2f),
                            ElectricBlue.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.Center)
        )

        // Mid glow ring
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(glowScale.value * 0.9f)
                .alpha(glowPulse.value * 0.25f)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ElectricBlue.copy(alpha = 0.18f),
                            NeonPurple.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.Center)
        )

        // Main content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            // Logo with glassmorphism card
            Box(
                modifier = Modifier
                    .alpha(logoAlpha.value)
                    .scale(logoScale.value)
            ) {
                // Neon glow behind logo
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(glowScale.value * 1.1f)
                        .alpha(glowPulse.value * 0.35f)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    NeonPurple.copy(alpha = 0.3f),
                                    ElectricBlue.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            )
                        )
                        .align(Alignment.Center)
                )

                // Glassmorphism card
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0x2AF0F0FF),
                                    Color(0x0DA0A0FF)
                                )
                            )
                        )
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0x08000000),
                                    Color(0x14000000)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        NeonPurple.copy(alpha = 0.2f),
                                        ElectricBlue.copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_splash_logo),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // AURA text with neon glow shadow
            Text(
                text = stringResource(R.string.splash_title),
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 14.sp,
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFD0C0FF),
                            Color(0xFFB090FF)
                        )
                    ),
                    shadow = Shadow(
                        color = NeonPurple.copy(alpha = glowPulse.value * 0.3f),
                        blurRadius = 12f
                    )
                ),
                modifier = Modifier
                    .offset { IntOffset(0, titleOffset.value.dp.roundToPx()) }
                    .alpha(titleAlpha.value)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // MUSIC text
            Text(
                text = stringResource(R.string.splash_subtitle),
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFFA898C8),
                letterSpacing = 10.sp,
                modifier = Modifier
                    .offset { IntOffset(0, subtitleOffset.value.dp.roundToPx()) }
                    .alpha(subtitleAlpha.value)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tagline
            Text(
                text = stringResource(R.string.experience_sound),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF706090).copy(alpha = taglineAlpha.value * 0.8f),
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 48.dp)
                    .alpha(taglineAlpha.value)
            )
        }

        // Bottom decorative line
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
                .alpha(bottomLineAlpha.value)
        ) {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                NeonPurple.copy(alpha = 0.5f),
                                ElectricBlue.copy(alpha = 0.5f),
                                NeonPurple.copy(alpha = 0.5f)
                            )
                        )
                    )
            )
        }

        // Version subtle at very bottom
        Text(
            text = stringResource(R.string.splash_version),
            fontSize = 11.sp,
            fontWeight = FontWeight.Light,
            color = Color(0xFF403050).copy(alpha = bottomLineAlpha.value * 0.6f),
            letterSpacing = 2.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .alpha(bottomLineAlpha.value)
        )
    }
}
