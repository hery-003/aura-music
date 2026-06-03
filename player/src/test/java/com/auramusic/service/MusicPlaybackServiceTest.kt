package com.auramusic.service

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicPlaybackServiceTest {

    @Test
    fun `notification constants are correct`() {
        assertEquals("aura_music_playback", MusicPlaybackService.CHANNEL_ID)
        assertEquals(1, MusicPlaybackService.NOTIFICATION_ID)
        assertEquals("com.auramusic.PLAY_PAUSE", MusicPlaybackService.ACTION_PLAY_PAUSE)
        assertEquals("com.auramusic.NEXT", MusicPlaybackService.ACTION_NEXT)
        assertEquals("com.auramusic.PREVIOUS", MusicPlaybackService.ACTION_PREVIOUS)
        assertEquals("com.auramusic.CLOSE", MusicPlaybackService.ACTION_CLOSE)
        assertEquals("com.auramusic.OPEN_APP", MusicPlaybackService.ACTION_OPEN_APP)
    }
}
