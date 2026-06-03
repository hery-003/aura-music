package com.auramusic.ui.screens.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.auramusic.R
import com.auramusic.data.preferences.AppPreferences
import com.auramusic.player.EqualizerManager

import com.auramusic.ui.components.NeonDivider
import com.auramusic.ui.theme.*
import android.Manifest
import android.content.Intent
import android.util.Log
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.content.pm.PackageManager
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: Int,
    equalizerPreset: Int,
    crossfadeEnabled: Boolean,
    crossfadeDuration: Int,
    showVisualizer: Boolean,
    showGamerMode: Boolean,
    accentColor: Long,
    audioQuality: Int = AppPreferences.AUDIO_QUALITY_NORMAL,
    animationsEnabled: Boolean = true,
    playbackSpeed: Float = 1f,
    onPlaybackSpeedChange: (Float) -> Unit = {},
    sleepTimerActive: Boolean = false,
    customEqBands: List<Float> = emptyList(),
    bandFrequencies: List<Int> = emptyList(),
    eqBandLevelRange: Pair<Int, Int> = Pair(-1500, 1500),
    onThemeModeChange: (Int) -> Unit,
    onAccentColorChange: (Long) -> Unit,
    onEqualizerPresetChange: (Int) -> Unit,
    onCustomBandLevelChange: (Int, Short) -> Unit,
    onCrossfadeEnabledChange: (Boolean) -> Unit,
    onCrossfadeDurationChange: (Int) -> Unit,
    onShowVisualizerChange: (Boolean) -> Unit,
    onGamerModeChange: (Boolean) -> Unit,
    onAudioQualityChange: (Int) -> Unit,
    onAnimationsEnabledChange: (Boolean) -> Unit,
    onRescan: () -> Unit,
    onBack: () -> Unit,
    onStatistics: () -> Unit = {},
    onHistory: () -> Unit = {},
    onSleepTimerStart: (Int) -> Unit = {},
    onSleepTimerStop: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item(key = "theme") {
                SettingsLabel(stringResource(R.string.theme))
                NeonDivider()
                Spacer(Modifier.height(12.dp))
                ThemeSelector(themeMode = themeMode, onThemeModeChange = onThemeModeChange)
                Spacer(Modifier.height(12.dp))
                AccentColorPicker(
                    selectedColor = accentColor,
                    onColorSelected = { onAccentColorChange(it) }
                )
                Spacer(Modifier.height(24.dp))
            }

            item(key = "equalizer") {
                SettingsLabel(stringResource(R.string.equalizer))
                NeonDivider()
                Spacer(Modifier.height(12.dp))
                EqualizerSelector(
                    selectedPreset = equalizerPreset,
                    onPresetChange = onEqualizerPresetChange
                )
                if (equalizerPreset == EqualizerManager.PRESET_CUSTOM) {
                    Spacer(Modifier.height(16.dp))
                    EqualizerSliders(
                        bandLevels = customEqBands,
                        bandFrequencies = bandFrequencies,
                        levelRange = eqBandLevelRange,
                        onBandLevelChange = onCustomBandLevelChange
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            item(key = "crossfade") {
                SettingsLabel(stringResource(R.string.crossfade))
                NeonDivider()
                Spacer(Modifier.height(12.dp))
                CrossfadeSection(
                    enabled = crossfadeEnabled,
                    duration = crossfadeDuration,
                    onEnabledChange = onCrossfadeEnabledChange,
                    onDurationChange = onCrossfadeDurationChange
                )
                Spacer(Modifier.height(24.dp))
            }

            item(key = "visualizer") {
                SettingsLabel(stringResource(R.string.visualizer))
                NeonDivider()
                Spacer(Modifier.height(12.dp))
                VisualizerSection(
                    show = showVisualizer,
                    onChange = onShowVisualizerChange
                )
                Spacer(Modifier.height(24.dp))
            }

            item(key = "audio") {
                SettingsLabel(stringResource(R.string.audio))
                NeonDivider()
                Spacer(Modifier.height(12.dp))
                AudioQualitySection(
                    quality = audioQuality,
                    onChange = onAudioQualityChange
                )
                Spacer(Modifier.height(16.dp))
                AnimationsSection(
                    enabled = animationsEnabled,
                    onChange = onAnimationsEnabledChange
                )
                Spacer(Modifier.height(24.dp))
            }

            item(key = "playback_speed") {
                SettingsLabel(stringResource(R.string.playback_speed))
                NeonDivider()
                Spacer(Modifier.height(12.dp))
                PlaybackSpeedSection(
                    speed = playbackSpeed,
                    onChange = onPlaybackSpeedChange
                )
                Spacer(Modifier.height(24.dp))
            }

            item(key = "sleep_timer") {
                SettingsLabel(stringResource(R.string.sleep_timer))
                NeonDivider()
                Spacer(Modifier.height(12.dp))
                SleepTimerSettings(
                    isActive = sleepTimerActive,
                    onStart = { minutes -> onSleepTimerStart(minutes) },
                    onStop = onSleepTimerStop
                )
                Spacer(Modifier.height(24.dp))
            }

            item(key = "gamer_mode") {
                SettingsLabel(stringResource(R.string.gamer_mode))
                NeonDivider()
                Spacer(Modifier.height(12.dp))
                GamerModeSection(
                    enabled = showGamerMode,
                    onChange = onGamerModeChange
                )
                Spacer(Modifier.height(24.dp))
            }

            item(key = "permissions") {
                SettingsLabel(stringResource(R.string.permissions))
                NeonDivider()
                Spacer(Modifier.height(12.dp))
                PermissionSection()
                Spacer(Modifier.height(24.dp))
            }

            item(key = "statistics") {
                SettingsLabel(stringResource(R.string.statistics))
                NeonDivider()
                Spacer(Modifier.height(12.dp))
                StatisticsButton(onStatistics = onStatistics)
                Spacer(Modifier.height(12.dp))
                HistoryButton(onHistory = onHistory)
                Spacer(Modifier.height(24.dp))
            }

            item(key = "storage") {
                SettingsLabel(stringResource(R.string.storage))
                NeonDivider()
                Spacer(Modifier.height(12.dp))
                RescanButton(onRescan = onRescan)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun ThemeSelector(
    themeMode: Int,
    onThemeModeChange: (Int) -> Unit
) {
    val themes = listOf(
        Triple(stringResource(R.string.amoled), AppPreferences.THEME_AMOLED, Icons.Rounded.DarkMode),
        Triple(stringResource(R.string.neon), AppPreferences.THEME_NEON, Icons.Rounded.Nightlight),
        Triple(stringResource(R.string.dynamic), AppPreferences.THEME_DYNAMIC, Icons.Rounded.AutoAwesome),
        Triple(stringResource(R.string.monet), AppPreferences.THEME_MONET, Icons.Rounded.Palette),
        Triple(stringResource(R.string.light), AppPreferences.THEME_LIGHT, Icons.Rounded.LightMode)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        themes.forEach { (name, value, icon) ->
            val selected = themeMode == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onThemeModeChange(value) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = name,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun EqualizerSelector(
    selectedPreset: Int,
    onPresetChange: (Int) -> Unit
) {
    val presets = listOf(
        stringResource(R.string.normal) to 0,
        stringResource(R.string.bass_boost) to 1,
        stringResource(R.string.rock) to 2,
        stringResource(R.string.pop) to 3,
        stringResource(R.string.jazz) to 4,
        stringResource(R.string.classical) to 5,
        stringResource(R.string.gamer) to 6,
        stringResource(R.string.custom) to 7,
        stringResource(R.string.clarity) to 8
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { (name, value) ->
            val selected = selectedPreset == value
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onPresetChange(value) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = name,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun EqualizerSliders(
    bandLevels: List<Float>,
    bandFrequencies: List<Int>,
    levelRange: Pair<Int, Int>,
    onBandLevelChange: (Int, Short) -> Unit
) {
    if (bandLevels.isEmpty()) {
        Text(
            text = stringResource(R.string.no_eq_bands),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        return
    }

    val minLevel = levelRange.first.toFloat()
    val maxLevel = levelRange.second.toFloat()

    Text(
        text = stringResource(R.string.custom_band_levels),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bandLevels.forEachIndexed { index, level ->
            val freqHz = if (index < bandFrequencies.size) bandFrequencies[index] else 0
            val freqLabel = when {
                freqHz >= 1000 -> "${freqHz / 1000}k"
                freqHz >= 100 -> "${freqHz / 100 * 100}"
                else -> "${freqHz}Hz"
            }
            val range = (maxLevel - minLevel).coerceAtLeast(1f)
            val normalized = ((level - minLevel) / range).coerceIn(0f, 1f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(36.dp)
            ) {
                Text(
                    text = "${(level / 100).roundToInt()}dB",
                    color = if (abs(level) > 100) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize
                )

                Spacer(Modifier.height(4.dp))

                val sliderValue = remember(index, normalized) { mutableFloatStateOf(normalized) }

                Box(
                    modifier = Modifier.size(24.dp, 140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Slider(
                        value = sliderValue.floatValue,
                        onValueChange = { newVal ->
                            sliderValue.floatValue = newVal
                            val newLevel = (minLevel + newVal * (maxLevel - minLevel)).roundToInt()
                                .coerceIn(minLevel.toInt(), maxLevel.toInt())
                            onBandLevelChange(index, newLevel.toShort())
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(-90f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = freqLabel,
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun CrossfadeSection(
    enabled: Boolean,
    duration: Int,
    onEnabledChange: (Boolean) -> Unit,
    onDurationChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.crossfade_songs),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
        if (enabled) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.crossfade_duration),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.seconds_format, duration),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(8.dp))
            Slider(
                value = duration.toFloat(),
                onValueChange = { onDurationChange(it.roundToInt()) },
                valueRange = 1f..12f,
                steps = 10,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
private fun VisualizerSection(
    show: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!show) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Waves,
                contentDescription = null,
                tint = if (show) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.show_visualizer),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.visualizer_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Switch(
            checked = show,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun AudioQualitySection(
    quality: Int,
    onChange: (Int) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.audio_quality),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val options = listOf(
                stringResource(R.string.normal) to AppPreferences.AUDIO_QUALITY_NORMAL,
                stringResource(R.string.high) to AppPreferences.AUDIO_QUALITY_HIGH
            )
            options.forEach { (name, value) ->
                val selected = quality == value
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onChange(value) }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = name,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.audio_quality_hint),
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun AnimationsSection(
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!enabled) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Animation,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.animations),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.animations_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun SleepTimerSettings(
    isActive: Boolean,
    onStart: (Int) -> Unit,
    onStop: () -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isActive) stringResource(R.string.timer_active) else stringResource(R.string.sleep_timer),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isActive) stringResource(R.string.music_will_stop)
                    else stringResource(R.string.set_timer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isActive) {
                TextButton(onClick = onStop) {
                    Text(stringResource(R.string.stop), color = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(onClick = { showPicker = true }) {
                    Text(stringResource(R.string.set), color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (showPicker) {
            val options = listOf(5, 10, 15, 30, 45, 60)
            Column {
                options.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { minutes ->
                            OutlinedButton(
                                onClick = {
                                    showPicker = false
                                    onStart(minutes)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = if (minutes < 60) "${minutes}m"
                                    else "${minutes / 60}h",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                TextButton(
                    onClick = { showPicker = false },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun GamerModeSection(
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!enabled) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.VideogameAsset,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.gamer_mode),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.rgb_effects),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun PlaybackSpeedSection(
    speed: Float,
    onChange: (Float) -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        speeds.forEach { s ->
            val selected = speed == s
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onChange(s) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${s}x",
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun PermissionSection() {
    val context = LocalContext.current
    val audioGranted = ContextCompat.checkSelfPermission(
        context,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = if (audioGranted) Color(0xFF06D6A0) else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(if (audioGranted) R.string.permission_audio_granted else R.string.permission_audio_denied),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (!audioGranted) {
            OutlinedButton(
                onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("SettingsScreen", "Failed to open settings", e)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.open_app_settings), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.open_app_settings))
            }
        }
    }
}

@Composable
private fun AccentColorPicker(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit
) {
    val colors = listOf(
        0xFF8B5CF6L to "Purple",
        0xFF3B82F6L to "Blue",
        0xFF06D6A0L to "Green",
        0xFFFF6B6BL to "Red",
        0xFFFFB347L to "Orange",
        0xFFFF69B4L to "Pink",
        0xFF00D4FFL to "Cyan",
        0xFFA855F7L to "Violet"
    )

    Column {
        Text(
            text = stringResource(R.string.accent_color),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colors.forEach { (colorLong, _) ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(colorLong))
                        .border(
                            width = if (colorLong == selectedColor) 2.dp else 0.dp,
                            color = MaterialTheme.colorScheme.onBackground,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onColorSelected(colorLong) }
                )
            }
        }
    }
}

@Composable
private fun StatisticsButton(onStatistics: () -> Unit) {
    Button(
        onClick = onStatistics,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = Icons.Rounded.BarChart,
            contentDescription = stringResource(R.string.statistics),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.statistics),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
private fun HistoryButton(onHistory: () -> Unit) {
    Button(
        onClick = onHistory,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = stringResource(R.string.listening_history),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.listening_history),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
private fun RescanButton(onRescan: () -> Unit) {
    Button(
        onClick = onRescan,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = stringResource(R.string.rescan_music),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.rescan_music),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleSmall
        )
    }
}


