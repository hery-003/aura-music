package com.auramusic.player

import com.auramusic.data.preferences.AppPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicPlayerTest {

    @Test
    fun `repeat mode constants are correct`() {
        assertEquals(0, AppPreferences.REPEAT_NONE)
        assertEquals(1, AppPreferences.REPEAT_ALL)
        assertEquals(2, AppPreferences.REPEAT_ONE)
    }

    @Test
    fun `audio quality constants are correct`() {
        assertEquals(0, AppPreferences.AUDIO_QUALITY_NORMAL)
        assertEquals(1, AppPreferences.AUDIO_QUALITY_HIGH)
    }
}
