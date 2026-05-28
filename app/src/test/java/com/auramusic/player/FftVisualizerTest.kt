package com.auramusic.player

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FftVisualizerTest {

    @Test
    fun `processFft handles valid data`() = runTest {
        val visualizer = FftVisualizer()
        val fftData = ByteArray(128) { (it * 2).toByte() }
        visualizer.processFftForTest(fftData)
        val magnitudes = visualizer.fftMagnitudes.first()
        assertEquals(6, magnitudes.size)
        magnitudes.forEach { assertTrue(it in 0f..1f) }
    }

    @Test
    fun `processFft handles empty data`() = runTest {
        val visualizer = FftVisualizer()
        visualizer.processFftForTest(ByteArray(0))
        val magnitudes = visualizer.fftMagnitudes.first()
        assertEquals(6, magnitudes.size)
        magnitudes.forEach { assertEquals(0f, it) }
    }

    @Test
    fun `processFft handles small data`() = runTest {
        val visualizer = FftVisualizer()
        visualizer.processFftForTest(ByteArray(2) { 1 })
        val magnitudes = visualizer.fftMagnitudes.first()
        assertEquals(6, magnitudes.size)
    }

    @Test
    fun `attach with negative id does nothing`() = runTest {
        val visualizer = FftVisualizer()
        visualizer.attach(-1)
        assertEquals(false, visualizer.isActive.first())
    }

    @Test
    fun `attach with zero id does nothing`() = runTest {
        val visualizer = FftVisualizer()
        visualizer.attach(0)
        assertEquals(false, visualizer.isActive.first())
    }

    @Test
    fun `release resets state`() = runTest {
        val visualizer = FftVisualizer()
        visualizer.release()
        assertEquals(false, visualizer.isActive.first())
    }
}
