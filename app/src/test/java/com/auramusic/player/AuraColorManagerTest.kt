package com.auramusic.player

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class AuraColorManagerTest {

    private lateinit var colorManager: AuraColorManager

    @Before
    fun setup() {
        colorManager = AuraColorManager()
    }

    @Test
    fun `default mode is DEFAULT`() = runTest {
        assertEquals(AuraMode.DEFAULT, colorManager.auraMode.first())
    }

    @Test
    fun `default color is neon purple`() = runTest {
        val color = colorManager.dominantColor.first()
        assertEquals(0xFF8B5CF6.toInt(), color.hashCode())
    }

    @Test
    fun `rock sets ENERGY mode`() = runTest {
        colorManager.updateMode("Rock")
        assertEquals(AuraMode.ENERGY, colorManager.auraMode.first())
    }

    @Test
    fun `metal sets ENERGY mode`() = runTest {
        colorManager.updateMode("Metal")
        assertEquals(AuraMode.ENERGY, colorManager.auraMode.first())
    }

    @Test
    fun `electronic sets ENERGY mode`() = runTest {
        colorManager.updateMode("Electronic")
        assertEquals(AuraMode.ENERGY, colorManager.auraMode.first())
    }

    @Test
    fun `pop sets ENERGY mode`() = runTest {
        colorManager.updateMode("Pop")
        assertEquals(AuraMode.ENERGY, colorManager.auraMode.first())
    }

    @Test
    fun `hip hop sets ENERGY mode`() = runTest {
        colorManager.updateMode("Hip Hop")
        assertEquals(AuraMode.ENERGY, colorManager.auraMode.first())
    }

    @Test
    fun `classical sets CALM mode`() = runTest {
        colorManager.updateMode("Classical")
        assertEquals(AuraMode.CALM, colorManager.auraMode.first())
    }

    @Test
    fun `jazz sets CALM mode`() = runTest {
        colorManager.updateMode("Jazz")
        assertEquals(AuraMode.CALM, colorManager.auraMode.first())
    }

    @Test
    fun `ambient sets CALM mode`() = runTest {
        colorManager.updateMode("Ambient")
        assertEquals(AuraMode.CALM, colorManager.auraMode.first())
    }

    @Test
    fun `lo-fi sets CALM mode`() = runTest {
        colorManager.updateMode("Lo-fi")
        assertEquals(AuraMode.CALM, colorManager.auraMode.first())
    }

    @Test
    fun `synthwave sets NEON mode`() = runTest {
        colorManager.updateMode("Synthwave")
        assertEquals(AuraMode.NEON, colorManager.auraMode.first())
    }

    @Test
    fun `vaporwave sets NEON mode`() = runTest {
        colorManager.updateMode("Vaporwave")
        assertEquals(AuraMode.NEON, colorManager.auraMode.first())
    }

    @Test
    fun `unknown genre sets DEFAULT mode`() = runTest {
        colorManager.updateMode("Unknown Genre")
        assertEquals(AuraMode.DEFAULT, colorManager.auraMode.first())
    }

    @Test
    fun `case insensitive matching`() = runTest {
        colorManager.updateMode("rock")
        assertEquals(AuraMode.ENERGY, colorManager.auraMode.first())
    }

    @Test
    fun `reset restores default`() = runTest {
        colorManager.updateMode("Rock")
        colorManager.reset()
        assertEquals(AuraMode.DEFAULT, colorManager.auraMode.first())
    }

    @Test
    fun `blues sets CALM mode`() = runTest {
        colorManager.updateMode("Blues")
        assertEquals(AuraMode.CALM, colorManager.auraMode.first())
    }

    @Test
    fun `reggae sets CALM mode`() = runTest {
        colorManager.updateMode("Reggae")
        assertEquals(AuraMode.CALM, colorManager.auraMode.first())
    }

    @Test
    fun `kpop sets NEON mode`() = runTest {
        colorManager.updateMode("K-pop")
        assertEquals(AuraMode.NEON, colorManager.auraMode.first())
    }

    @Test
    fun `anime sets NEON mode`() = runTest {
        colorManager.updateMode("Anime")
        assertEquals(AuraMode.NEON, colorManager.auraMode.first())
    }
}
