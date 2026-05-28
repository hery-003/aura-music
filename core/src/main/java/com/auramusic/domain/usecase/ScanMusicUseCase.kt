package com.auramusic.domain.usecase

import com.auramusic.domain.model.Song
import com.auramusic.domain.repository.MusicRepository
import com.auramusic.util.MusicScanner
import javax.inject.Inject

class ScanMusicUseCase @Inject constructor(
    private val scanner: MusicScanner,
    private val repository: MusicRepository
) {
    suspend operator fun invoke(existingSongs: List<Song>): List<Song> {
        val scannedSongs = scanner.scanAudioFiles()
        val existingById = existingSongs.associateBy { it.id }
        val existingByPath = existingSongs.associateBy { it.path }
        return scannedSongs.map { scanned ->
            existingById[scanned.id]?.let { existing ->
                scanned.copy(
                    isFavorite = existing.isFavorite,
                    playCount = existing.playCount,
                    lastPlayed = existing.lastPlayed
                )
            } ?: scanned.path.let { path ->
                if (path.isNotBlank()) {
                    existingByPath[path]?.let { existing ->
                        scanned.copy(
                            isFavorite = existing.isFavorite,
                            playCount = existing.playCount,
                            lastPlayed = existing.lastPlayed
                        )
                    }
                } else null
            } ?: scanned
        }
    }
}
