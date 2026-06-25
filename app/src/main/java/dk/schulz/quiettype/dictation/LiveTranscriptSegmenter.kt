package dk.schulz.quiettype.dictation

import java.util.Locale

class LiveTranscriptSegmenter {
    private var committedLength: Int = 0

    fun nextCompletedSegment(transcript: String): String? {
        val clean = transcript.trim()
        if (clean.length <= committedLength) return null
        val completedEnd = completedSentenceEnd(clean, start = committedLength)
            ?: return null
        return commitSegment(clean, completedEnd)
    }

    fun nextStableSegment(transcript: String): String? {
        val clean = transcript.trim()
        if (clean.length <= committedLength) return null
        val stableEnd = stableWordEnd(clean, start = committedLength)
            ?: return null
        return commitSegment(clean, stableEnd)
    }

    fun remainingSegment(transcript: String): String? {
        val clean = transcript.trim()
        if (clean.length <= committedLength) return null
        return commitSegment(clean, clean.length)
    }

    private fun commitSegment(text: String, end: Int): String? {
        val isFirstCommittedSegment = committedLength == 0
        val segment = text.substring(committedLength, end).trim()
        committedLength = end
        return normalizeAsrText(segment, isFirstCommittedSegment).takeIf { it.isNotBlank() }
    }

    private fun completedSentenceEnd(text: String, start: Int): Int? {
        for (index in start until text.length) {
            if (text[index].isSentenceTerminal()) {
                return index + 1
            }
        }
        return null
    }

    private fun stableWordEnd(text: String, start: Int): Int? {
        val remainder = text.substring(start).trimStart()
        if (!remainder.contains(' ')) return null
        val lastSpace = text.lastIndexOf(' ')
        return lastSpace.takeIf { it > start }
    }

    private fun normalizeAsrText(text: String, isFirstCommittedSegment: Boolean): String {
        if (!text.looksLikeAllCapsAsr()) return text
        val lower = text.lowercase(Locale.getDefault())
        return if (isFirstCommittedSegment && lower.isNotBlank()) {
            lower.replaceFirstChar { first ->
                if (first.isLowerCase()) first.titlecase(Locale.getDefault()) else first.toString()
            }
        } else {
            lower
        }
    }

    private fun String.looksLikeAllCapsAsr(): Boolean {
        val letters = filter { it.isLetter() }
        return letters.length >= 2 && letters.all { it.isUpperCase() }
    }

    private fun Char.isSentenceTerminal(): Boolean = this in setOf('.', '!', '?', '。', '！', '？')
}
