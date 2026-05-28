package com.auramusic.domain.usecase

import com.auramusic.domain.model.Song
import com.auramusic.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSongsByArtistUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    operator fun invoke(artist: String): Flow<List<Song>> {
        return repository.getSongsByArtist(artist)
    }
}
