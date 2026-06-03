package com.auramusic.player

import com.auramusic.data.preferences.AppPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var preferences: AppPreferences
    private var timerEnded = false

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        preferences = mockk(relaxed = true)
        coEvery { preferences.sleepTimerActive } returns flowOf(false)
        coEvery { preferences.sleepTimerDuration } returns flowOf(0L)
        timerEnded = false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `start sets remaining seconds correctly`() = runTest {
        val manager = SleepTimerManager(preferences) { timerEnded = true }
        manager.start(5)
        assertEquals(300L, manager.remainingSeconds.value)
        assertTrue(manager.isActive.value)
    }

    @Test
    fun `stop cancels timer`() = runTest {
        val manager = SleepTimerManager(preferences) { timerEnded = true }
        manager.start(10)
        manager.stop()
        assertEquals(0L, manager.remainingSeconds.value)
        assertFalse(manager.isActive.value)
    }

    @Test
    fun `formatted time shows correct format`() = runTest {
        val manager = SleepTimerManager(preferences) { timerEnded = true }
        manager.start(5)
        assertEquals("5:00", manager.formattedTime)
    }

    @Test
    fun `addTime increases remaining seconds when active`() = runTest {
        val manager = SleepTimerManager(preferences) { timerEnded = true }
        manager.start(5)
        manager.addTime(5)
        assertEquals(600L, manager.remainingSeconds.value)
    }

    @Test
    fun `addTime does nothing when timer is not active`() = runTest {
        val manager = SleepTimerManager(preferences) { timerEnded = true }
        manager.addTime(5)
        assertEquals(0L, manager.remainingSeconds.value)
    }

    @Test
    fun `restore resumes timer`() = runTest {
        val manager = SleepTimerManager(preferences) { timerEnded = true }
        manager.restore(3)
        assertEquals(180L, manager.remainingSeconds.value)
        assertTrue(manager.isActive.value)
    }

    @Test
    fun `release stops timer`() = runTest {
        val manager = SleepTimerManager(preferences) { timerEnded = true }
        manager.start(5)
        manager.release()
        assertFalse(manager.isActive.value)
        assertEquals(0L, manager.remainingSeconds.value)
        coVerify { preferences.setSleepTimerActive(false) }
    }
}
