package dk.schulz.quiettype.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextInsertionDraftTest {
    @Test
    fun finalTranscriptCanBeInsertedWhenEditableNodeIsFocused() {
        val request = TextInsertionRequest(
            focusedField = FocusedFieldSnapshot(
                packageName = "com.example.notes",
                className = "android.widget.EditText",
                isFocused = true,
                isEditable = true,
                isPassword = false,
            ),
            existingText = "hello",
            transcript = "world",
        )

        val draft = TextInsertionDraft.from(request)

        assertTrue(draft.canInsert)
        assertEquals("hello world", draft.textToSet)
    }

    @Test
    fun placeholderHintTextIsNotPrependedToTranscript() {
        val draft = TextInsertionDraft.from(
            TextInsertionRequest(
                focusedField = FocusedFieldSnapshot(
                    packageName = "com.example.search",
                    className = "android.widget.EditText",
                    isFocused = true,
                    isEditable = true,
                    isPassword = false,
                ),
                existingText = "Search…",
                hintText = "Search…",
                transcript = "coffee nearby",
            ),
        )

        assertTrue(draft.canInsert)
        assertEquals("coffee nearby", draft.textToSet)
    }

    @Test
    fun emptyTranscriptDoesNotInsert() {
        val draft = TextInsertionDraft.from(
            TextInsertionRequest(
                focusedField = FocusedFieldSnapshot(
                    packageName = "com.example.notes",
                    className = "android.widget.EditText",
                    isFocused = true,
                    isEditable = true,
                    isPassword = false,
                ),
                existingText = "hello",
                transcript = "   ",
            ),
        )

        assertFalse(draft.canInsert)
        assertEquals(TextInsertionBlockReason.EmptyTranscript, draft.blockReason)
    }

    @Test
    fun passwordFieldsDoNotReceiveTextInsertion() {
        val draft = TextInsertionDraft.from(
            TextInsertionRequest(
                focusedField = FocusedFieldSnapshot(
                    packageName = "com.example.passwords",
                    className = "android.widget.EditText",
                    isFocused = true,
                    isEditable = true,
                    isPassword = true,
                ),
                existingText = "",
                transcript = "secret",
            ),
        )

        assertFalse(draft.canInsert)
        assertEquals(TextInsertionBlockReason.SensitiveField, draft.blockReason)
    }
}
