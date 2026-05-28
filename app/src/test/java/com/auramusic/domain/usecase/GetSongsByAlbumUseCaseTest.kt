package com.auramusic.domain.usecase

import com.auramusic.domain.model.Song
import com.auramusic.domain.repository.MusicRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class GetSongsByAlbumUseCaseTest {

    private val repository: MusicRepository = mockk()
    private lateinit var useCase: GetSongsByAlbumUseCase

    @Before
    fun setup() {
        useCase = GetSongsByAlbumUseCase(repository)
    }

    @Test
    fun `invoke returns songs by album`() = runTest {
        val songs = listOf(Song(id = 1, title = "Song A", album = "Album X"))
        every { repository.getSongsByAlbum(1L) } returns flowOf(songs)
        val result = useCase(1L).first()
        assertEquals(1, result.size)
        assertEquals("Song A", result[0].title)
    }

    @Test
    fun `invoke returns empty for unknown album`() = runTest {
        every { repository.getSongsByAlbum(999L) } returns flowOf(emptyList())
        val result = useCase(999L).first()
        assertEquals(0, result.size)
    }
}
