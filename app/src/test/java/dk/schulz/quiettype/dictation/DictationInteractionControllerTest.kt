package dk.schulz.quiettype.dictation

import dk.schulz.quiettype.settings.DictationInteraction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DictationInteractionControllerTest {
    @Test
    fun permissionGrantDoesNotStartMicrophoneCapture() {
        assertFalse(DictationInteractionController.shouldStartOnPermissionGrant())
    }

    @Test
    fun holdToTalkStartsOnButtonDownAndStopsOnButtonUp() {
        assertEquals(
            DictationCommand.StartRecording,
            DictationInteractionController.onButtonDown(
                interaction = DictationInteraction.HoldToTalk,
                isRecording = false,
            ),
        )
        assertEquals(
            DictationCommand.StopRecording,
            DictationInteractionController.onButtonUp(
                interaction = DictationInteraction.HoldToTalk,
                isRecording = true,
                wasDragging = false,
            ),
        )
    }

    @Test
    fun tapToToggleTogglesOnlyOnButtonReleaseWithoutDrag() {
        assertEquals(
            DictationCommand.None,
            DictationInteractionController.onButtonDown(
                interaction = DictationInteraction.TapToToggle,
                isRecording = false,
            ),
        )
        assertEquals(
            DictationCommand.ToggleRecording,
            DictationInteractionController.onButtonUp(
                interaction = DictationInteraction.TapToToggle,
                isRecording = false,
                wasDragging = false,
            ),
        )
        assertEquals(
            DictationCommand.None,
            DictationInteractionController.onButtonUp(
                interaction = DictationInteraction.TapToToggle,
                isRecording = false,
                wasDragging = true,
            ),
        )
    }
}
