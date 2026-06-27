package dk.schulz.quiettype.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DictationHistoryPolicyTest {
    @Test
    fun historyIsDisabledByDefaultForPrivacy() {
        assertFalse(DictationHistoryPolicy.shouldRecord(transcript = "hello", historyEnabled = false))
    }

    @Test
    fun blankTranscriptsAreNeverRecorded() {
        assertFalse(DictationHistoryPolicy.shouldRecord(transcript = "   ", historyEnabled = true))
    }

    @Test
    fun enabledHistoryRecordsCleanTranscript() {
        assertTrue(DictationHistoryPolicy.shouldRecord(transcript = " hello world ", historyEnabled = true))
        assertEquals("hello world", DictationHistoryPolicy.cleanTranscript(" hello world "))
    }

    @Test
    fun addingEntryKeepsNewestFirstAndLimitsRetention() {
        val existing = (1..DictationHistoryPolicy.MaxEntries).map { index ->
            DictationHistoryEntry(id = "old-$index", text = "old $index", createdAtEpochMillis = index.toLong())
        }

        val updated = DictationHistoryPolicy.addEntry(
            existing = existing,
            entry = DictationHistoryEntry(id = "new", text = "new text", createdAtEpochMillis = 99L),
        )

        assertEquals(DictationHistoryPolicy.MaxEntries, updated.size)
        assertEquals("new", updated.first().id)
        assertFalse(updated.any { it.id == "old-${DictationHistoryPolicy.MaxEntries}" })
    }

    @Test
    fun entriesCanBeDeletedIndividually() {
        val entries = listOf(
            DictationHistoryEntry(id = "a", text = "alpha", createdAtEpochMillis = 1L),
            DictationHistoryEntry(id = "b", text = "beta", createdAtEpochMillis = 2L),
        )

        assertEquals(listOf(entries.first()), DictationHistoryPolicy.deleteEntry(entries, "b"))
    }
}
