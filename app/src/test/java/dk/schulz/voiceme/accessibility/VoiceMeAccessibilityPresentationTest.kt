package dk.schulz.voiceme.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceMeAccessibilityPresentationTest {
    @Test
    fun notificationCopyExplainsFloatingButtonReadiness() {
        assertEquals("VoiceMe floating button ready", VoiceMeAccessibilityPresentation.NotificationTitle)
        assertTrue(VoiceMeAccessibilityPresentation.NotificationText.contains("editable fields"))
        assertTrue(VoiceMeAccessibilityPresentation.NotificationText.contains("mic button"))
    }

    @Test
    fun statusCopyReflectsAccessibilityEnablement() {
        assertTrue(VoiceMeAccessibilityPresentation.statusText(isEnabled = true).contains("enabled"))
        assertTrue(VoiceMeAccessibilityPresentation.statusText(isEnabled = true).contains("floating mic button"))
        assertTrue(VoiceMeAccessibilityPresentation.statusText(isEnabled = false).contains("not enabled"))
        assertTrue(VoiceMeAccessibilityPresentation.statusText(isEnabled = false).contains("Accessibility settings"))
    }

    @Test
    fun overlayLabelShowsIdleListeningAndProcessingStateAndAppName() {
        assertEquals(
            "🎙 VoiceMe · notes",
            VoiceMeAccessibilityPresentation.overlayLabel(
                packageName = "com.example.notes",
                state = OverlayDictationState.Idle,
            ),
        )
        assertEquals(
            "● Listening · notes",
            VoiceMeAccessibilityPresentation.overlayLabel(
                packageName = "com.example.notes",
                state = OverlayDictationState.Listening,
            ),
        )
        assertEquals(
            "⏳ Thinking · notes",
            VoiceMeAccessibilityPresentation.overlayLabel(
                packageName = "com.example.notes",
                state = OverlayDictationState.Processing,
            ),
        )
    }

    @Test
    fun stopRequestSwitchesButtonToProcessingImmediately() {
        assertEquals(
            OverlayDictationState.Processing,
            VoiceMeAccessibilityPresentation.stateAfterStopRequested(wasRecording = true),
        )
        assertEquals(
            OverlayDictationState.Idle,
            VoiceMeAccessibilityPresentation.stateAfterStopRequested(wasRecording = false),
        )
    }
}
