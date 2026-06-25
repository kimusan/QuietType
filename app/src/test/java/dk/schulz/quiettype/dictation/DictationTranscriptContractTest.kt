package dk.schulz.quiettype.dictation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DictationTranscriptContractTest {
    @Test
    fun finalTranscriptIsTrimmedBeforeInsertion() {
        assertEquals("hello QuietType", DictationTranscriptContract.cleanFinalTranscript("  hello QuietType  "))
    }

    @Test
    fun blankFinalTranscriptIsIgnored() {
        assertNull(DictationTranscriptContract.cleanFinalTranscript("   "))
        assertNull(DictationTranscriptContract.cleanFinalTranscript(null))
    }
}
