package dk.schulz.quiettype.accessibility

import dk.schulz.quiettype.correction.QuickCorrectionPolicy

data class TextCorrectionRequest(
    val focusedField: FocusedFieldSnapshot,
    val existingText: String,
    val hintText: String? = null,
    val selectionStart: Int? = null,
    val selectionEnd: Int? = null,
)

enum class TextCorrectionBlockReason {
    None,
    NotEditable,
    SensitiveField,
    EmptyText,
    NoChange,
}

data class PreparedTextCorrection(
    val baseText: String,
    val textToCorrect: String,
    val replacementStart: Int,
    val replacementEnd: Int,
)

sealed class TextCorrectionPreparationResult {
    data class Ready(val prepared: PreparedTextCorrection) : TextCorrectionPreparationResult()
    data class Blocked(val draft: TextCorrectionDraft) : TextCorrectionPreparationResult()
}

data class TextCorrectionDraft(
    val canCorrect: Boolean,
    val textToSet: String,
    val blockReason: TextCorrectionBlockReason,
    val cursorPosition: Int? = null,
) {
    companion object {
        fun from(request: TextCorrectionRequest): TextCorrectionDraft = when (val preparation = prepare(request)) {
            is TextCorrectionPreparationResult.Blocked -> preparation.draft
            is TextCorrectionPreparationResult.Ready -> fromPrepared(
                prepared = preparation.prepared,
                corrected = QuickCorrectionPolicy.autoCorrect(preparation.prepared.textToCorrect),
            )
        }

        fun prepare(request: TextCorrectionRequest): TextCorrectionPreparationResult {
            if (!request.focusedField.isFocused || !request.focusedField.isEditable) {
                return TextCorrectionPreparationResult.Blocked(blocked(TextCorrectionBlockReason.NotEditable))
            }
            if (FocusedFieldSensitivity.isSensitive(request.focusedField)) {
                return TextCorrectionPreparationResult.Blocked(blocked(TextCorrectionBlockReason.SensitiveField))
            }

            val hint = request.hintText?.trim()
            val fieldLooksEmpty = hint != null && request.existingText.trim() == hint
            val baseText = if (fieldLooksEmpty) "" else request.existingText
            if (baseText.isBlank()) {
                return TextCorrectionPreparationResult.Blocked(blocked(TextCorrectionBlockReason.EmptyText))
            }

            val range = normalizedRange(
                selectionStart = request.selectionStart,
                selectionEnd = request.selectionEnd,
                textLength = baseText.length,
            )
            val selectedRange = range?.takeIf { it.first != it.second }
            val (textToCorrect, replacementRange) = if (selectedRange != null) {
                baseText.substring(selectedRange.first, selectedRange.second) to selectedRange
            } else {
                baseText to (0 to baseText.length)
            }

            return TextCorrectionPreparationResult.Ready(
                PreparedTextCorrection(
                    baseText = baseText,
                    textToCorrect = textToCorrect,
                    replacementStart = replacementRange.first,
                    replacementEnd = replacementRange.second,
                ),
            )
        }

        fun fromPrepared(prepared: PreparedTextCorrection, corrected: String): TextCorrectionDraft {
            if (corrected == prepared.textToCorrect) return blocked(TextCorrectionBlockReason.NoChange)
            val replacementText = prepared.baseText.substring(0, prepared.replacementStart) +
                corrected +
                prepared.baseText.substring(prepared.replacementEnd)
            return TextCorrectionDraft(
                canCorrect = true,
                textToSet = replacementText,
                blockReason = TextCorrectionBlockReason.None,
                cursorPosition = prepared.replacementStart + corrected.length,
            )
        }

        private fun blocked(reason: TextCorrectionBlockReason): TextCorrectionDraft = TextCorrectionDraft(
            canCorrect = false,
            textToSet = "",
            blockReason = reason,
        )

        private fun normalizedRange(selectionStart: Int?, selectionEnd: Int?, textLength: Int): Pair<Int, Int>? {
            if (selectionStart == null || selectionEnd == null) return null
            if (selectionStart < 0 || selectionEnd < 0) return null
            val start = minOf(selectionStart, selectionEnd).coerceAtMost(textLength)
            val end = maxOf(selectionStart, selectionEnd).coerceAtMost(textLength)
            return start to end
        }
    }
}
