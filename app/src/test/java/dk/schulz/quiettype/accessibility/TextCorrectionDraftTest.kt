package dk.schulz.quiettype.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextCorrectionDraftTest {
    @Test
    fun correctionPreparationExtractsSelectedTextInActiveEditableField() {
        val preparation = TextCorrectionDraft.prepare(
            TextCorrectionRequest(
                focusedField = editableField(),
                existingText = "Please send HELLO   WORLD today",
                selectionStart = 12,
                selectionEnd = 25,
            ),
        )

        val prepared = (preparation as TextCorrectionPreparationResult.Ready).prepared
        assertEquals("HELLO   WORLD", prepared.textToCorrect)
        assertEquals(12, prepared.replacementStart)
        assertEquals(25, prepared.replacementEnd)
    }

    @Test
    fun fromPreparedRebuildsWholeFieldWhenNoSelectionExists() {
        val preparation = TextCorrectionDraft.prepare(
            TextCorrectionRequest(
                focusedField = editableField(),
                existingText = "  HELLO   WORLD  ",
                selectionStart = 7,
                selectionEnd = 7,
            ),
        )

        val prepared = (preparation as TextCorrectionPreparationResult.Ready).prepared
        val draft = TextCorrectionDraft.fromPrepared(prepared, "Hello world.")
        assertTrue(draft.canCorrect)
        assertEquals("Hello world.", draft.textToSet)
        assertEquals(12, draft.cursorPosition)
    }

    @Test
    fun correctionDoesNotTouchSensitiveFields() {
        val preparation = TextCorrectionDraft.prepare(
            TextCorrectionRequest(
                focusedField = editableField(isPassword = true),
                existingText = "HELLO WORLD",
                selectionStart = 0,
                selectionEnd = 11,
            ),
        )

        val blocked = (preparation as TextCorrectionPreparationResult.Blocked).draft
        assertFalse(blocked.canCorrect)
        assertEquals(TextCorrectionBlockReason.SensitiveField, blocked.blockReason)
    }

    private fun editableField(isPassword: Boolean = false) = FocusedFieldSnapshot(
        packageName = "com.example.notes",
        className = "android.widget.EditText",
        isFocused = true,
        isEditable = true,
        isPassword = isPassword,
    )
}
