package com.auramusic.ui.components

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import com.auramusic.data.preferences.AppPreferences
import com.auramusic.player.EqualizerManager
import com.auramusic.player.MusicPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val musicPlayer: MusicPlayer,
    val preferences: AppPreferences
) : ViewModel() {

    fun setEqualizerPreset(preset: Int) {
        try {
            musicPlayer.equalizerManager.setPreset(preset)
        } catch (e: Exception) { Timber.e(e, "setEqualizerPreset failed") }
        viewModelScope.launch {
            try {
                preferences.setEqualizerPreset(preset)
            } catch (e: Exception) { Timber.e(e, "Failed to save eq preset") }
        }
    }

    fun setCustomEqualizerBand(bandIndex: Int, levelMillibels: Short) {
        try {
            musicPlayer.equalizerManager.setBandLevel(bandIndex, levelMillibels)
        } catch (e: Exception) { Timber.e(e, "setCustomEqualizerBand failed") }
        viewModelScope.launch {
            try {
                preferences.setEqualizerPreset(EqualizerManager.PRESET_CUSTOM)
                val bands = musicPlayer.equalizerManager.exportCustomBands()
                preferences.setCustomEqBands(bands)
            } catch (e: Exception) { Timber.e(e, "Failed to save custom eq bands") }
        }
    }

    fun loadCustomEqualizerBands() {
        viewModelScope.launch {
            try {
                preferences.customEqBands.first().let { csv ->
                    if (csv.isNotBlank()) {
                        musicPlayer.equalizerManager.loadCustomBands(csv)
                    }
                }
            } catch (e: Exception) { Timber.e(e, "loadCustomEqualizerBands failed") }
        }
    }
}
