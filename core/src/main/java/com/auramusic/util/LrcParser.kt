package com.auramusic.util

import java.io.File

data class LrcLine(
    val timestampMs: Long,
    val text: String
)

data class LyricData(
    val lines: List<LrcLine>,
    val title: String? = null,
    val artist: String? = null
)

object LrcParser {

    fun parse(file: File): LyricData? {
        if (!file.exists() || !file.extension.equals("lrc", ignoreCase = true)) return null
        return try {
            val lines = file.readLines(Charsets.UTF_8)
            parseLines(lines)
        } catch (e: Exception) {
            try {
                val lines = file.readLines(Charsets.ISO_8859_1)
                parseLines(lines)
            } catch (e2: Exception) {
                null
            }
        }
    }

    fun parse(content: String): LyricData? {
        return try {
            parseLines(content.lines())
        } catch (e: Exception) {
            null
        }
    }

    private fun parseLines(lines: List<String>): LyricData {
        val lrcLines = mutableListOf<LrcLine>()
        var title: String? = null
        var artist: String? = null

        val regex = Regex("""\[(\d{2}):(\d{2})[\.:](\d{2,3})\](.*)""")
        val simpleRegex = Regex("""\[(\d{2}):(\d{2})\](.*)""")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            if (trimmed.startsWith("[ti:")) {
                title = trimmed.removePrefix("[ti:").removeSuffix("]").trim()
                continue
            }
            if (trimmed.startsWith("[ar:")) {
                artist = trimmed.removePrefix("[ar:").removeSuffix("]").trim()
                continue
            }
            if (trimmed.startsWith("[al:")) continue
            if (trimmed.startsWith("[by:")) continue
            if (trimmed.startsWith("[offset:")) continue

            val match = regex.find(trimmed)
            if (match != null) {
                val minutes = match.groupValues[1].toIntOrNull() ?: 0
                val seconds = match.groupValues[2].toIntOrNull() ?: 0
                val millisStr = match.groupValues[3]
                val millis = if (millisStr.length == 2) millisStr.toIntOrNull()?.times(10) ?: 0
                           else millisStr.toIntOrNull() ?: 0
                val text = match.groupValues[4].trim()
                if (text.isNotBlank()) {
                    lrcLines.add(LrcLine(
                        timestampMs = minutes * 60_000L + seconds * 1000L + millis,
                        text = text
                    ))
                }
                continue
            }

            val simpleMatch = simpleRegex.find(trimmed)
            if (simpleMatch != null) {
                val minutes = simpleMatch.groupValues[1].toIntOrNull() ?: 0
                val seconds = simpleMatch.groupValues[2].toIntOrNull() ?: 0
                val text = simpleMatch.groupValues[3].trim()
                if (text.isNotBlank()) {
                    lrcLines.add(LrcLine(
                        timestampMs = minutes * 60_000L + seconds * 1000L,
                        text = text
                    ))
                }
            }
        }

        lrcLines.sortBy { it.timestampMs }
        return LyricData(lines = lrcLines, title = title, artist = artist)
    }

    fun findLrcFile(musicFilePath: String): File? {
        if (musicFilePath.isBlank()) return null
        val musicFile = File(musicFilePath)
        val parent = musicFile.parentFile ?: return null

        val lrcFile = File(parent, musicFile.nameWithoutExtension + ".lrc")
        if (lrcFile.exists()) return lrcFile

        val txtFile = File(parent, musicFile.nameWithoutExtension + ".txt")
        if (txtFile.exists()) return txtFile

        return null
    }

    fun getLineAtPosition(lines: List<LrcLine>, positionMs: Long): Int {
        var index = lines.binarySearch { it.timestampMs.compareTo(positionMs) }
        if (index < 0) index = -index - 2
        return index.coerceIn(0, lines.size - 1)
    }
}
