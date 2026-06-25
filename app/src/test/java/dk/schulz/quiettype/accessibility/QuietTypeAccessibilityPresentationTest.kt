package dk.schulz.quiettype.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietTypeAccessibilityPresentationTest {
    @Test
    fun notificationCopyExplainsFloatingButtonReadiness() {
        assertEquals("QuietType floating button ready", QuietTypeAccessibilityPresentation.NotificationTitle)
        assertTrue(QuietTypeAccessibilityPresentation.NotificationText.contains("editable fields"))
        assertTrue(QuietTypeAccessibilityPresentation.NotificationText.contains("mic button"))
    }

    @Test
    fun statusCopyReflectsAccessibilityEnablement() {
        assertTrue(QuietTypeAccessibilityPresentation.statusText(isEnabled = true).contains("enabled"))
        assertTrue(QuietTypeAccessibilityPresentation.statusText(isEnabled = true).contains("floating mic button"))
        assertTrue(QuietTypeAccessibilityPresentation.statusText(isEnabled = false).contains("not enabled"))
        assertTrue(QuietTypeAccessibilityPresentation.statusText(isEnabled = false).contains("Accessibility settings"))
    }

    @Test
    fun overlayLabelShowsOnlyDictationStateWithoutAppOrFieldName() {
        assertEquals(
            "🎙 QuietType",
            QuietTypeAccessibilityPresentation.overlayLabel(state = OverlayDictationState.Idle),
        )
        assertEquals(
            "● Listening",
            QuietTypeAccessibilityPresentation.overlayLabel(state = OverlayDictationState.Listening),
        )
        assertEquals(
            "⏳ Thinking",
            QuietTypeAccessibilityPresentation.overlayLabel(state = OverlayDictationState.Processing),
        )
    }

    @Test
    fun stopRequestSwitchesButtonToProcessingImmediately() {
        assertEquals(
            OverlayDictationState.Processing,
            QuietTypeAccessibilityPresentation.stateAfterStopRequested(wasRecording = true),
        )
        assertEquals(
            OverlayDictationState.Idle,
            QuietTypeAccessibilityPresentation.stateAfterStopRequested(wasRecording = false),
        )
    }
}
