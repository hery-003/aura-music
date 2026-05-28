package com.auramusic.domain.usecase

import com.auramusic.domain.repository.MusicRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ToggleFavoriteUseCaseTest {

    private val repository: MusicRepository = mockk()
    private lateinit var useCase: ToggleFavoriteUseCase

    @Before
    fun setup() {
        useCase = ToggleFavoriteUseCase(repository)
    }

    @Test
    fun `invoke toggles favorite`() = runTest {
        coEvery { repository.toggleFavorite(1L, true) } returns Unit
        useCase(1L, true)
        coVerify { repository.toggleFavorite(1L, true) }
    }

    @Test
    fun `invoke toggles unfavorite`() = runTest {
        coEvery { repository.toggleFavorite(1L, false) } returns Unit
        useCase(1L, false)
        coVerify { repository.toggleFavorite(1L, false) }
    }
}
