package dk.schulz.voiceme.dictation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DictationTranscriptContractTest {
    @Test
    fun finalTranscriptIsTrimmedBeforeInsertion() {
        assertEquals("hello VoiceMe", DictationTranscriptContract.cleanFinalTranscript("  hello VoiceMe  "))
    }

    @Test
    fun blankFinalTranscriptIsIgnored() {
        assertNull(DictationTranscriptContract.cleanFinalTranscript("   "))
        assertNull(DictationTranscriptContract.cleanFinalTranscript(null))
    }
}
