package com.auramusic.domain.usecase

import com.auramusic.domain.model.Song
import com.auramusic.domain.repository.MusicRepository
import com.auramusic.util.MusicScanner
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class ScanMusicUseCaseTest {

    private val scanner: MusicScanner = mockk()
    private val repository: MusicRepository = mockk()
    private lateinit var useCase: ScanMusicUseCase

    @Before
    fun setup() {
        useCase = ScanMusicUseCase(scanner, repository)
    }

    @Test
    fun `invoke merges scanned songs with existing data`() = runTest {
        val scanned = listOf(
            Song(id = 1, title = "A", artist = "X", playCount = 0, isFavorite = false),
            Song(id = 2, title = "B", artist = "Y", playCount = 0, isFavorite = false)
        )
        val existing = listOf(
            Song(id = 1, title = "A", artist = "X", playCount = 10, isFavorite = true)
        )
        every { scanner.scanAudioFiles() } returns scanned
        val result = useCase(existing)
        assertEquals(2, result.size)
        assertEquals(10, result[0].playCount)
        assertEquals(true, result[0].isFavorite)
        assertEquals(0, result[1].playCount)
    }

    @Test
    fun `invoke handles empty scan`() = runTest {
        every { scanner.scanAudioFiles() } returns emptyList()
        val result = useCase(emptyList())
        assertEquals(0, result.size)
    }

    @Test
    fun `invoke preserves all existing metadata`() = runTest {
        val scanned = listOf(
            Song(id = 1, title = "A", artist = "X", playCount = 0, isFavorite = false, lastPlayed = 0L)
        )
        val existing = listOf(
            Song(id = 1, title = "A", artist = "X", playCount = 5, isFavorite = true, lastPlayed = 1000L)
        )
        every { scanner.scanAudioFiles() } returns scanned
        val result = useCase(existing)
        assertEquals(5, result[0].playCount)
        assertEquals(true, result[0].isFavorite)
        assertEquals(1000L, result[0].lastPlayed)
    }
}
