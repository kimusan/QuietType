package dk.schulz.quiettype.history

import android.content.Context
import java.util.UUID

data class DictationHistoryEntry(
    val id: String,
    val text: String,
    val createdAtEpochMillis: Long,
)

object DictationHistoryPolicy {
    const val MaxEntries: Int = 25

    fun shouldRecord(transcript: String?, historyEnabled: Boolean): Boolean =
        historyEnabled && cleanTranscript(transcript).isNotEmpty()

    fun cleanTranscript(transcript: String?): String = transcript.orEmpty().trim()

    fun addEntry(
        existing: List<DictationHistoryEntry>,
        entry: DictationHistoryEntry,
    ): List<DictationHistoryEntry> = (listOf(entry) + existing.filterNot { it.id == entry.id })
        .take(MaxEntries)

    fun deleteEntry(existing: List<DictationHistoryEntry>, entryId: String): List<DictationHistoryEntry> =
        existing.filterNot { it.id == entryId }
}

object DictationHistoryCodec {
    fun encode(entries: List<DictationHistoryEntry>): String = entries.joinToString("\n") { entry ->
        listOf(
            escape(entry.id),
            entry.createdAtEpochMillis.toString(),
            escape(entry.text),
        ).joinToString("|")
    }

    fun decode(encoded: String): List<DictationHistoryEntry> = encoded.lines()
        .mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val parts = line.split('|')
            if (parts.size < 3) return@mapNotNull null
            DictationHistoryEntry(
                id = unescape(parts[0]).takeIf { it.isNotBlank() } ?: return@mapNotNull null,
                createdAtEpochMillis = parts[1].toLongOrNull() ?: return@mapNotNull null,
                text = unescape(parts.drop(2).joinToString("|")).takeIf { it.isNotBlank() } ?: return@mapNotNull null,
            )
        }

    private fun escape(value: String): String = buildString {
        value.forEach { char ->
            when (char) {
                '%' -> append("%25")
                '|' -> append("%7C")
                '\n' -> append("%0A")
                '\r' -> append("%0D")
                else -> append(char)
            }
        }
    }

    private fun unescape(value: String): String {
        val result = StringBuilder()
        var index = 0
        while (index < value.length) {
            if (value[index] == '%' && index + 2 < value.length) {
                when (value.substring(index, index + 3)) {
                    "%25" -> {
                        result.append('%')
                        index += 3
                        continue
                    }
                    "%7C" -> {
                        result.append('|')
                        index += 3
                        continue
                    }
                    "%0A" -> {
                        result.append('\n')
                        index += 3
                        continue
                    }
                    "%0D" -> {
                        result.append('\r')
                        index += 3
                        continue
                    }
                }
            }
            result.append(value[index])
            index += 1
        }
        return result.toString()
    }
}

class DictationHistoryStore(context: Context) {
    private val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)

    fun load(): List<DictationHistoryEntry> = DictationHistoryCodec.decode(
        preferences.getString(EntriesKey, null).orEmpty(),
    )

    fun save(entries: List<DictationHistoryEntry>) {
        preferences.edit()
            .putString(EntriesKey, DictationHistoryCodec.encode(entries.take(DictationHistoryPolicy.MaxEntries)))
            .apply()
    }

    fun recordTranscript(transcript: String?, historyEnabled: Boolean) {
        if (!DictationHistoryPolicy.shouldRecord(transcript, historyEnabled)) return
        val entry = DictationHistoryEntry(
            id = UUID.randomUUID().toString(),
            text = DictationHistoryPolicy.cleanTranscript(transcript),
            createdAtEpochMillis = System.currentTimeMillis(),
        )
        save(DictationHistoryPolicy.addEntry(load(), entry))
    }

    fun delete(entryId: String) {
        save(DictationHistoryPolicy.deleteEntry(load(), entryId))
    }

    fun clear() {
        save(emptyList())
    }

    companion object {
        private const val PreferencesName = "quiettype_history"
        private const val EntriesKey = "entries"
    }
}
