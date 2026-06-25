package dk.schulz.quiettype.accessibility

import dk.schulz.quiettype.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusedFieldDetectorTest {
    @Test
    fun editableFocusedTextFieldShowsOverlayWithoutReadingText() {
        val detection = FocusedFieldDetector.detect(
            snapshot = FocusedFieldSnapshot(
                packageName = "com.example.notes",
                className = "android.widget.EditText",
                isFocused = true,
                isEditable = true,
                isPassword = false,
            ),
            settings = AppSettings.default(),
        )

        assertTrue(detection.shouldShowOverlay)
        assertEquals(FocusedFieldHideReason.None, detection.hideReason)
        assertEquals("com.example.notes", detection.packageName)
        assertEquals("android.widget.EditText", detection.className)
    }

    @Test
    fun passwordFieldsHideOverlayWhenSensitiveFieldProtectionIsEnabled() {
        val detection = FocusedFieldDetector.detect(
            snapshot = FocusedFieldSnapshot(
                packageName = "com.example.passwords",
                className = "android.widget.EditText",
                isFocused = true,
                isEditable = true,
                isPassword = true,
            ),
            settings = AppSettings.default(),
        )

        assertFalse(detection.shouldShowOverlay)
        assertEquals(FocusedFieldHideReason.SensitiveField, detection.hideReason)
    }

    @Test
    fun passwordFieldsCanShowOverlayWhenSensitiveFieldProtectionIsDisabled() {
        val detection = FocusedFieldDetector.detect(
            snapshot = FocusedFieldSnapshot(
                packageName = "com.example.passwords",
                className = "android.widget.EditText",
                isFocused = true,
                isEditable = true,
                isPassword = true,
            ),
            settings = AppSettings.default().copy(hideInSensitiveFields = false),
        )

        assertTrue(detection.shouldShowOverlay)
        assertEquals(FocusedFieldHideReason.None, detection.hideReason)
    }

    @Test
    fun nonEditableFocusedViewsDoNotShowOverlay() {
        val detection = FocusedFieldDetector.detect(
            snapshot = FocusedFieldSnapshot(
                packageName = "com.example.reader",
                className = "android.widget.TextView",
                isFocused = true,
                isEditable = false,
                isPassword = false,
            ),
            settings = AppSettings.default(),
        )

        assertFalse(detection.shouldShowOverlay)
        assertEquals(FocusedFieldHideReason.NotEditable, detection.hideReason)
    }
}
