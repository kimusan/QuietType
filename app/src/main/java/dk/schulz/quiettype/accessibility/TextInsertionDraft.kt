package dk.schulz.quiettype.accessibility

data class TextInsertionRequest(
    val focusedField: FocusedFieldSnapshot,
    val existingText: String,
    val transcript: String,
    val hintText: String? = null,
)

enum class TextInsertionBlockReason {
    None,
    NotEditable,
    SensitiveField,
    EmptyTranscript,
}

data class TextInsertionDraft(
    val canInsert: Boolean,
    val textToSet: String,
    val blockReason: TextInsertionBlockReason,
) {
    companion object {
        fun from(request: TextInsertionRequest): TextInsertionDraft {
            val transcript = request.transcript.trim()
            return when {
                transcript.isEmpty() -> blocked(TextInsertionBlockReason.EmptyTranscript)
                !request.focusedField.isFocused || !request.focusedField.isEditable ->
                    blocked(TextInsertionBlockReason.NotEditable)
                request.focusedField.isPassword -> blocked(TextInsertionBlockReason.SensitiveField)
                else -> TextInsertionDraft(
                    canInsert = true,
                    textToSet = appendTranscript(
                        existingText = request.existingText,
                        hintText = request.hintText,
                        transcript = transcript,
                    ),
                    blockReason = TextInsertionBlockReason.None,
                )
            }
        }

        private fun blocked(reason: TextInsertionBlockReason): TextInsertionDraft = TextInsertionDraft(
            canInsert = false,
            textToSet = "",
            blockReason = reason,
        )

        private fun appendTranscript(existingText: String, hintText: String?, transcript: String): String {
            val existing = existingText.trimEnd()
            val hint = hintText?.trim()
            val realExisting = if (hint != null && existing.trim() == hint) "" else existing
            return if (realExisting.isBlank()) {
                transcript
            } else {
                "$realExisting $transcript"
            }
        }
    }
}
