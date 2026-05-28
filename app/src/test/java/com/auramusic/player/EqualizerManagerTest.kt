package com.auramusic.player

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class EqualizerManagerTest {

    @Test
    fun `default preset is NORMAL`() = runTest {
        val eq = EqualizerManager()
        assertEquals(EqualizerManager.PRESET_NORMAL, eq.currentPreset.value)
    }

    @Test
    fun `numberOfBands starts at zero`() {
        val eq = EqualizerManager()
        assertEquals(0, eq.numberOfBands)
    }

    @Test
    fun `bandLevelRange returns defaults when not attached`() {
        val eq = EqualizerManager()
        val range = eq.getBandLevelRange()
        assertEquals(-1500, range.first)
        assertEquals(1500, range.second)
    }

    @Test
    fun `attach with invalid id does nothing`() = runTest {
        val eq = EqualizerManager()
        eq.attach(-1)
        assertEquals(0, eq.numberOfBands)
    }

    @Test
    fun `exportCustomBands returns empty when no bands`() {
        val eq = EqualizerManager()
        assertEquals("", eq.exportCustomBands())
    }

    @Test
    fun `loadCustomBands with blank does nothing`() {
        val eq = EqualizerManager()
        eq.loadCustomBands("")
        assertEquals(EqualizerManager.PRESET_NORMAL, eq.currentPreset.value)
    }

    @Test
    fun `constants are correct`() {
        assertEquals(0, EqualizerManager.PRESET_NORMAL)
        assertEquals(1, EqualizerManager.PRESET_BASS_BOOST)
        assertEquals(2, EqualizerManager.PRESET_ROCK)
        assertEquals(3, EqualizerManager.PRESET_POP)
        assertEquals(4, EqualizerManager.PRESET_JAZZ)
        assertEquals(5, EqualizerManager.PRESET_CLASSICAL)
        assertEquals(6, EqualizerManager.PRESET_GAMER)
        assertEquals(7, EqualizerManager.PRESET_CUSTOM)
    }

    @Test
    fun `release clears state`() {
        val eq = EqualizerManager()
        eq.release()
        assertEquals(0, eq.numberOfBands)
    }
}
