package dk.schulz.quiettype.accessibility

data class TextInsertionRequest(
    val focusedField: FocusedFieldSnapshot,
    val existingText: String,
    val transcript: String,
    val hintText: String? = null,
    val selectionStart: Int? = null,
    val selectionEnd: Int? = null,
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
    val cursorPosition: Int? = null,
) {
    companion object {
        fun from(request: TextInsertionRequest): TextInsertionDraft {
            val transcript = request.transcript.trim()
            return when {
                transcript.isEmpty() -> blocked(TextInsertionBlockReason.EmptyTranscript)
                !request.focusedField.isFocused || !request.focusedField.isEditable ->
                    blocked(TextInsertionBlockReason.NotEditable)
                FocusedFieldSensitivity.isSensitive(request.focusedField) ->
                    blocked(TextInsertionBlockReason.SensitiveField)
                else -> composeInsertion(
                    existingText = request.existingText,
                    hintText = request.hintText,
                    transcript = transcript,
                    selectionStart = request.selectionStart,
                    selectionEnd = request.selectionEnd,
                )
            }
        }

        private fun blocked(reason: TextInsertionBlockReason): TextInsertionDraft = TextInsertionDraft(
            canInsert = false,
            textToSet = "",
            blockReason = reason,
        )

        private fun composeInsertion(
            existingText: String,
            hintText: String?,
            transcript: String,
            selectionStart: Int?,
            selectionEnd: Int?,
        ): TextInsertionDraft {
            val hint = hintText?.trim()
            val fieldLooksEmpty = hint != null && existingText.trim() == hint
            val baseText = if (fieldLooksEmpty) "" else existingText
            val range = normalizedRange(
                selectionStart = selectionStart,
                selectionEnd = selectionEnd,
                textLength = baseText.length,
            )
            val (textToSet, cursor) = if (range != null) {
                val (start, end) = range
                val prefix = baseText.substring(0, start)
                val suffix = baseText.substring(end)
                val replacement = spacingAwareReplacement(prefix, suffix, transcript)
                val replacementText = prefix + replacement + suffix
                replacementText to prefix.length + replacement.length
            } else {
                appendTranscript(baseText, transcript) to null
            }
            return TextInsertionDraft(
                canInsert = true,
                textToSet = textToSet,
                blockReason = TextInsertionBlockReason.None,
                cursorPosition = cursor,
            )
        }

        private fun normalizedRange(selectionStart: Int?, selectionEnd: Int?, textLength: Int): Pair<Int, Int>? {
            if (selectionStart == null || selectionEnd == null) return null
            if (selectionStart < 0 || selectionEnd < 0) return null
            val start = minOf(selectionStart, selectionEnd).coerceAtMost(textLength)
            val end = maxOf(selectionStart, selectionEnd).coerceAtMost(textLength)
            return start to end
        }

        private fun spacingAwareReplacement(prefix: String, suffix: String, transcript: String): String {
            val needsLeadingSpace = prefix.isNotEmpty() && !prefix.last().isWhitespace()
            val needsTrailingSpace = suffix.isNotEmpty() && !suffix.first().isWhitespace()
            return buildString {
                if (needsLeadingSpace) append(' ')
                append(transcript)
                if (needsTrailingSpace) append(' ')
            }
        }

        private fun appendTranscript(existingText: String, transcript: String): String {
            val existing = existingText.trimEnd()
            return if (existing.isBlank()) {
                transcript
            } else {
                "$existing $transcript"
            }
        }
    }
}
