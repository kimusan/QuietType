package dk.schulz.voiceme.dictation

import dk.schulz.voiceme.settings.DictationInteraction

object DictationInteractionController {
    fun shouldStartOnPermissionGrant(): Boolean = false

    fun onButtonDown(interaction: DictationInteraction, isRecording: Boolean): DictationCommand = when (interaction) {
        DictationInteraction.HoldToTalk -> if (isRecording) DictationCommand.None else DictationCommand.StartRecording
        DictationInteraction.TapToToggle -> DictationCommand.None
    }

    fun onButtonUp(interaction: DictationInteraction, isRecording: Boolean, wasDragging: Boolean): DictationCommand = when (interaction) {
        DictationInteraction.HoldToTalk -> if (isRecording) DictationCommand.StopRecording else DictationCommand.None
        DictationInteraction.TapToToggle -> if (!wasDragging) DictationCommand.ToggleRecording else DictationCommand.None
    }
}

enum class DictationCommand {
    None,
    StartRecording,
    StopRecording,
    ToggleRecording,
}
