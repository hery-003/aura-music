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

class GetSongsByArtistUseCaseTest {

    private val repository: MusicRepository = mockk()
    private lateinit var useCase: GetSongsByArtistUseCase

    @Before
    fun setup() {
        useCase = GetSongsByArtistUseCase(repository)
    }

    @Test
    fun `invoke returns songs by artist`() = runTest {
        val songs = listOf(
            Song(id = 1, title = "Song A", artist = "Artist X"),
            Song(id = 2, title = "Song B", artist = "Artist X")
        )
        every { repository.getSongsByArtist("Artist X") } returns flowOf(songs)
        val result = useCase("Artist X").first()
        assertEquals(2, result.size)
    }

    @Test
    fun `invoke returns empty for unknown artist`() = runTest {
        every { repository.getSongsByArtist("Unknown") } returns flowOf(emptyList())
        val result = useCase("Unknown").first()
        assertEquals(0, result.size)
    }
}
