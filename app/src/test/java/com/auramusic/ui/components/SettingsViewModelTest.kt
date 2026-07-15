package com.auramusic.ui.components

import com.auramusic.data.preferences.AppPreferences
import com.auramusic.player.EqualizerManager
import com.auramusic.player.MusicPlayer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SettingsViewModelTest {

    private val musicPlayer: MusicPlayer = mockk()
    private val equalizerManager: EqualizerManager = mockk()
    private val preferences: AppPreferences = mockk()

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        every { musicPlayer.equalizerManager } returns equalizerManager
        every { equalizerManager.setPreset(any()) } returns Unit
        every { equalizerManager.setBandLevel(any(), any()) } returns Unit
        every { equalizerManager.exportCustomBands() } returns ""
        every { equalizerManager.loadCustomBands(any()) } returns Unit

        every { preferences.setEqualizerPreset(any()) } returns Unit
        every { preferences.setCustomEqBands(any()) } returns Unit
        coEvery { preferences.customEqBands.first() } returns ""

        viewModel = SettingsViewModel(
            musicPlayer = musicPlayer,
            preferences = preferences
        )
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setEqualizerPreset updates equalizer and saves preference`() = runTest {
        viewModel.setEqualizerPreset(EqualizerManager.PRESET_ROCK)

        verify { equalizerManager.setPreset(EqualizerManager.PRESET_ROCK) }
        coVerify { preferences.setEqualizerPreset(EqualizerManager.PRESET_ROCK) }
    }

    @Test
    fun `setEqualizerPreset handles equalizer exception gracefully`() = runTest {
        every { equalizerManager.setPreset(any()) } throws RuntimeException("EQ error")

        viewModel.setEqualizerPreset(EqualizerManager.PRESET_BASS_BOOST)

        coVerify { preferences.setEqualizerPreset(EqualizerManager.PRESET_BASS_BOOST) }
    }

    @Test
    fun `setCustomEqualizerBand sets band level and saves as custom preset`() = runTest {
        every { equalizerManager.exportCustomBands() } returns "0,1500"

        viewModel.setCustomEqualizerBand(0, 1500)

        verify { equalizerManager.setBandLevel(0, 1500) }
        coVerify { preferences.setEqualizerPreset(EqualizerManager.PRESET_CUSTOM) }
        coVerify { preferences.setCustomEqBands("0,1500") }
    }

    @Test
    fun `setCustomEqualizerBand handles equalizer exception gracefully`() = runTest {
        every { equalizerManager.setBandLevel(any(), any()) } throws RuntimeException("Band error")
        every { equalizerManager.exportCustomBands() } returns ""

        viewModel.setCustomEqualizerBand(1, -300)

        coVerify { preferences.setEqualizerPreset(EqualizerManager.PRESET_CUSTOM) }
        coVerify { preferences.setCustomEqBands("") }
    }

    @Test
    fun `loadCustomEqualizerBands loads non-blank csv`() = runTest {
        coEvery { preferences.customEqBands.first() } returns "0,1000;1,-500"

        viewModel.loadCustomEqualizerBands()

        verify { equalizerManager.loadCustomBands("0,1000;1,-500") }
    }

    @Test
    fun `loadCustomEqualizerBands does nothing when csv is blank`() = runTest {
        coEvery { preferences.customEqBands.first() } returns ""

        viewModel.loadCustomEqualizerBands()

        verify(exactly = 0) { equalizerManager.loadCustomBands(any()) }
    }

    @Test
    fun `loadCustomEqualizerBands handles exception gracefully`() = runTest {
        coEvery { preferences.customEqBands.first() } throws RuntimeException("Prefs error")

        viewModel.loadCustomEqualizerBands()

        verify(exactly = 0) { equalizerManager.loadCustomBands(any()) }
    }
}
