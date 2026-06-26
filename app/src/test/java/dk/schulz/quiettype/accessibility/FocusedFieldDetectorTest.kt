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
    fun fieldsWithSensitiveMetadataHideOverlayEvenWhenPasswordFlagIsMissing() {
        val cases = listOf(
            FocusedFieldSnapshot(
                packageName = "com.example.bank",
                className = "android.widget.EditText",
                viewIdResourceName = "com.example.bank:id/card_number",
                hintText = "Card number",
                isFocused = true,
                isEditable = true,
                isPassword = false,
            ),
            FocusedFieldSnapshot(
                packageName = "com.example.auth",
                className = "android.widget.EditText",
                viewIdResourceName = "com.example.auth:id/otp_code",
                hintText = "One-time code",
                isFocused = true,
                isEditable = true,
                isPassword = false,
            ),
            FocusedFieldSnapshot(
                packageName = "com.example.login",
                className = "android.widget.EditText",
                viewIdResourceName = "com.example.login:id/pin",
                hintText = "PIN",
                isFocused = true,
                isEditable = true,
                isPassword = false,
            ),
        )

        cases.forEach { snapshot ->
            val detection = FocusedFieldDetector.detect(
                snapshot = snapshot,
                settings = AppSettings.default(),
            )

            assertFalse(detection.shouldShowOverlay)
            assertEquals(FocusedFieldHideReason.SensitiveField, detection.hideReason)
        }
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

    @Test
    fun appHideRuleSuppressesOverlayForMatchingPackage() {
        val detection = FocusedFieldDetector.detect(
            snapshot = FocusedFieldSnapshot(
                packageName = "com.example.notes",
                className = "com.example.notes.EditorActivity",
                viewIdResourceName = "com.example.notes:id/body",
                hintText = "Body",
                isFocused = true,
                isEditable = true,
                isPassword = false,
            ),
            settings = AppSettings.default().copy(
                hiddenTargets = listOf(
                    HiddenFieldTarget.forApp("com.example.notes"),
                ),
            ),
        )

        assertFalse(detection.shouldShowOverlay)
        assertEquals(FocusedFieldHideReason.UserHiddenTarget, detection.hideReason)
    }

    @Test
    fun fieldHideRuleDoesNotSuppressOtherFieldsInSameApp() {
        val detection = FocusedFieldDetector.detect(
            snapshot = FocusedFieldSnapshot(
                packageName = "com.example.notes",
                className = "com.example.notes.EditorActivity",
                viewIdResourceName = "com.example.notes:id/body",
                hintText = "Body",
                isFocused = true,
                isEditable = true,
                isPassword = false,
            ),
            settings = AppSettings.default().copy(
                hiddenTargets = listOf(
                    HiddenFieldTarget.forField(
                        packageName = "com.example.notes",
                        className = "com.example.notes.EditorActivity",
                        viewIdResourceName = "com.example.notes:id/title",
                        label = "Title",
                    ),
                ),
            ),
        )

        assertTrue(detection.shouldShowOverlay)
        assertEquals(FocusedFieldHideReason.None, detection.hideReason)
    }

}
