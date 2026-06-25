package dk.schulz.quiettype.dictation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DictationSessionTest {
    @Test
    fun cannotStartRecordingUntilMicrophonePermissionIsGranted() {
        val state = DictationSessionState.idle(hasMicrophonePermission = false)

        val result = DictationSessionReducer.startRecording(state)

        assertFalse(result.isRecording)
        assertEquals(DictationBlockReason.MicrophonePermissionMissing, result.blockReason)
    }

    @Test
    fun startsForegroundRecordingShellWhenPermissionIsGranted() {
        val state = DictationSessionState.idle(hasMicrophonePermission = true)

        val result = DictationSessionReducer.startRecording(state)

        assertTrue(result.isRecording)
        assertEquals(DictationBlockReason.None, result.blockReason)
    }

    @Test
    fun finalStubTranscriptIsAvailableAfterStop() {
        val recording = DictationSessionReducer.startRecording(
            DictationSessionState.idle(hasMicrophonePermission = true),
        )

        val stopped = DictationSessionReducer.stopRecording(recording)

        assertFalse(stopped.isRecording)
        assertEquals("QuietType dictation test", stopped.latestFinalTranscript)
    }
}
