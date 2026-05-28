package com.auramusic.player

import com.auramusic.data.preferences.AppPreferences
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SleepTimerManagerTest {

    private val preferences: AppPreferences = mockk()
    private var timerEnded = false
    private lateinit var timerManager: SleepTimerManager

    @Before
    fun setup() {
        every { preferences.sleepTimerDuration } returns MutableStateFlow(0L)
        every { preferences.sleepTimerActive } returns MutableStateFlow(false)
        coEvery { preferences.setSleepTimer(any()) } returns Unit
        coEvery { preferences.setSleepTimerActive(any()) } returns Unit
        timerEnded = false
        timerManager = SleepTimerManager(preferences) { timerEnded = true }
    }

    @Test
    fun `initial state is inactive`() = runTest {
        assertFalse(timerManager.isActive.value)
    }

    @Test
    fun `start sets timer active`() = runTest {
        timerManager.start(5)
        assertTrue(timerManager.isActive.value)
        assertTrue(timerManager.remainingSeconds.value > 0)
    }

    @Test
    fun `stop clears timer`() = runTest {
        timerManager.start(5)
        timerManager.stop()
        assertFalse(timerManager.isActive.value)
    }

    @Test
    fun `remainingSeconds matches duration`() = runTest {
        timerManager.start(10)
        val remaining = timerManager.remainingSeconds.value
        assertTrue(remaining in 590..600)
    }

    @Test
    fun `release stops timer`() {
        timerManager.start(5)
        timerManager.release()
        assertFalse(timerManager.isActive.value)
    }
}
