package com.auramusic.domain.usecase

import com.auramusic.domain.model.Song
import com.auramusic.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSongsByAlbumUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    operator fun invoke(albumId: Long): Flow<List<Song>> {
        return repository.getSongsByAlbum(albumId)
    }
}
