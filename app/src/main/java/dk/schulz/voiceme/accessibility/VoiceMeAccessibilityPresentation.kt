package dk.schulz.voiceme.accessibility

enum class OverlayDictationState {
    Idle,
    Listening,
    Processing,
}

object VoiceMeAccessibilityPresentation {
    const val NotificationTitle = "VoiceMe floating button ready"
    const val NotificationText = "VoiceMe is watching for editable fields locally. Tap a text field to show the mic button."

    fun statusText(isEnabled: Boolean): String = if (isEnabled) {
        "Accessibility service enabled. Tap any editable text field to show the VoiceMe floating mic button."
    } else {
        "Accessibility service not enabled. Open Android Accessibility settings and enable VoiceMe."
    }

    fun overlayLabel(packageName: String?, state: OverlayDictationState): String {
        val prefix = when (state) {
            OverlayDictationState.Idle -> "🎙 VoiceMe"
            OverlayDictationState.Listening -> "● Listening"
            OverlayDictationState.Processing -> "⏳ Thinking"
        }
        val appLabel = packageName?.substringAfterLast('.')?.takeIf { it.isNotBlank() }
        return if (appLabel == null) prefix else "$prefix · $appLabel"
    }

    fun stateAfterStopRequested(wasRecording: Boolean): OverlayDictationState = if (wasRecording) {
        OverlayDictationState.Processing
    } else {
        OverlayDictationState.Idle
    }
}
