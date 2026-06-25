package dk.schulz.quiettype.dictation

enum class DictationBlockReason {
    None,
    MicrophonePermissionMissing,
}

data class DictationSessionState(
    val hasMicrophonePermission: Boolean,
    val isRecording: Boolean,
    val blockReason: DictationBlockReason,
    val latestPartialTranscript: String,
    val latestFinalTranscript: String,
) {
    companion object {
        fun idle(hasMicrophonePermission: Boolean): DictationSessionState = DictationSessionState(
            hasMicrophonePermission = hasMicrophonePermission,
            isRecording = false,
            blockReason = DictationBlockReason.None,
            latestPartialTranscript = "",
            latestFinalTranscript = "",
        )
    }
}

object DictationSessionReducer {
    const val StubFinalTranscript = "QuietType dictation test"

    fun startRecording(state: DictationSessionState): DictationSessionState = if (!state.hasMicrophonePermission) {
        state.copy(
            isRecording = false,
            blockReason = DictationBlockReason.MicrophonePermissionMissing,
            latestPartialTranscript = "",
        )
    } else {
        state.copy(
            isRecording = true,
            blockReason = DictationBlockReason.None,
            latestPartialTranscript = "Listening locally...",
            latestFinalTranscript = "",
        )
    }

    fun stopRecording(state: DictationSessionState): DictationSessionState = state.copy(
        isRecording = false,
        blockReason = DictationBlockReason.None,
        latestPartialTranscript = "",
        latestFinalTranscript = if (state.isRecording) StubFinalTranscript else state.latestFinalTranscript,
    )
}
