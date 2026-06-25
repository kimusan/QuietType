package dk.schulz.voiceme.dictation

object DictationTranscriptContract {
    const val ActionFinalTranscript = "dk.schulz.voiceme.action.FINAL_TRANSCRIPT"
    const val ActionProcessingState = "dk.schulz.voiceme.action.PROCESSING_STATE"
    const val ExtraTranscript = "dk.schulz.voiceme.extra.TRANSCRIPT"
    const val ExtraIsProcessing = "dk.schulz.voiceme.extra.IS_PROCESSING"

    fun cleanFinalTranscript(text: String?): String? = text
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}
