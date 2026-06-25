package dk.schulz.quiettype.dictation

object DictationTranscriptContract {
    const val ActionFinalTranscript = "dk.schulz.quiettype.action.FINAL_TRANSCRIPT"
    const val ActionLiveTranscript = "dk.schulz.quiettype.action.LIVE_TRANSCRIPT"
    const val ActionProcessingState = "dk.schulz.quiettype.action.PROCESSING_STATE"
    const val ExtraTranscript = "dk.schulz.quiettype.extra.TRANSCRIPT"
    const val ExtraIsProcessing = "dk.schulz.quiettype.extra.IS_PROCESSING"

    fun cleanFinalTranscript(text: String?): String? = cleanTranscript(text)

    fun cleanLiveTranscript(text: String?): String? = cleanTranscript(text)

    private fun cleanTranscript(text: String?): String? = text
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}
