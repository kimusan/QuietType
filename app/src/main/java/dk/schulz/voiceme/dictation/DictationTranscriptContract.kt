package dk.schulz.voiceme.dictation

object DictationTranscriptContract {
    const val ActionFinalTranscript = "dk.schulz.voiceme.action.FINAL_TRANSCRIPT"
    const val ExtraTranscript = "dk.schulz.voiceme.extra.TRANSCRIPT"

    fun cleanFinalTranscript(text: String?): String? = text
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}
