package com.auramusic.domain.usecase

import com.auramusic.domain.repository.MusicRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(songId: Long, isFavorite: Boolean) {
        repository.toggleFavorite(songId, isFavorite)
    }
}
