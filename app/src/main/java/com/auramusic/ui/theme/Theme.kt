package com.auramusic.ui.theme

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.auramusic.data.preferences.AppPreferences

private val AmoledColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = SoftWhite,
    primaryContainer = NeonPurpleDark,
    onPrimaryContainer = SoftWhite,
    secondary = ElectricBlue,
    onSecondary = SoftWhite,
    secondaryContainer = ElectricBlueDark,
    onSecondaryContainer = SoftWhite,
    tertiary = Color(0xFFA78BFA),
    onTertiary = SoftWhite,
    background = Color.Black,
    onBackground = SoftWhite,
    surface = Color(0xFF050505),
    onSurface = SoftWhite,
    surfaceVariant = Color(0xFF111111),
    onSurfaceVariant = TextSecondary,
    outline = TextTertiary,
    outlineVariant = Color(0xFF1A1A1A),
    surfaceTint = NeonPurple,
    error = FavoriteRed,
    onError = SoftWhite,
    inverseSurface = SoftWhite,
    inverseOnSurface = Color.Black,
    inversePrimary = NeonPurpleLight
)

private val DarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = SoftWhite,
    primaryContainer = NeonPurpleDark,
    onPrimaryContainer = SoftWhite,
    secondary = ElectricBlue,
    onSecondary = SoftWhite,
    secondaryContainer = ElectricBlueDark,
    onSecondaryContainer = SoftWhite,
    tertiary = Color(0xFFA78BFA),
    onTertiary = SoftWhite,
    background = DeepBlack,
    onBackground = SoftWhite,
    surface = SurfaceDark,
    onSurface = SoftWhite,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = TextTertiary,
    outlineVariant = SurfaceVariant,
    surfaceTint = NeonPurple,
    error = FavoriteRed,
    onError = SoftWhite,
    inverseSurface = SoftWhite,
    inverseOnSurface = DeepBlack,
    inversePrimary = NeonPurpleLight
)

private val LightColorScheme = lightColorScheme(
    primary = NeonPurple,
    onPrimary = SoftWhite,
    primaryContainer = NeonPurpleLight,
    onPrimaryContainer = DeepBlack,
    secondary = ElectricBlue,
    onSecondary = SoftWhite,
    secondaryContainer = ElectricBlueLight,
    onSecondaryContainer = DeepBlack,
    tertiary = NeonPurpleDark,
    onTertiary = SoftWhite,
    background = Color(0xFFF8F8F8),
    onBackground = DeepBlack,
    surface = Color.White,
    onSurface = DeepBlack,
    surfaceVariant = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFF555555),
    outline = Color(0xFFAAAAAA),
    outlineVariant = Color(0xFFDDDDDD),
    surfaceTint = NeonPurple,
    error = FavoriteRed,
    onError = SoftWhite
)

private fun buildAccentColorScheme(accent: Color): ColorScheme {
    return darkColorScheme(
        primary = accent,
        onPrimary = SoftWhite,
        primaryContainer = accent.copy(alpha = 0.3f),
        onPrimaryContainer = SoftWhite,
        secondary = accent.copy(alpha = 0.7f),
        onSecondary = SoftWhite,
        secondaryContainer = accent.copy(alpha = 0.2f),
        onSecondaryContainer = SoftWhite,
        tertiary = accent.copy(alpha = 0.5f),
        onTertiary = SoftWhite,
        background = DeepBlack,
        onBackground = SoftWhite,
        surface = SurfaceDark,
        onSurface = SoftWhite,
        surfaceVariant = SurfaceVariant,
        onSurfaceVariant = TextSecondary,
        outline = TextTertiary,
        outlineVariant = SurfaceVariant,
        surfaceTint = accent,
        error = FavoriteRed,
        onError = SoftWhite,
        inverseSurface = SoftWhite,
        inverseOnSurface = DeepBlack,
        inversePrimary = accent.copy(alpha = 0.8f)
    )
}

@Composable
private fun buildMonetColorScheme(): ColorScheme {
    val context = LocalContext.current
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        DarkColorScheme
    }
}

@Composable
fun AuraMusicTheme(
    themeMode: Int = AppPreferences.THEME_AMOLED,
    accentColor: Color = NeonPurple,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppPreferences.THEME_AMOLED -> AmoledColorScheme
        AppPreferences.THEME_NEON -> DarkColorScheme
        AppPreferences.THEME_DYNAMIC -> buildAccentColorScheme(accentColor)
        AppPreferences.THEME_MONET -> buildMonetColorScheme()
        AppPreferences.THEME_LIGHT -> LightColorScheme
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
