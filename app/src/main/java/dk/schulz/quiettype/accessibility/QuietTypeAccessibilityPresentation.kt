package dk.schulz.quiettype.accessibility

enum class OverlayDictationState {
    Idle,
    Listening,
    Processing,
}

object QuietTypeAccessibilityPresentation {
    const val NotificationTitle = "QuietType floating button ready"
    const val NotificationText = "QuietType is watching for editable fields locally. Tap a text field to show the mic button."

    fun statusText(isEnabled: Boolean): String = if (isEnabled) {
        "Accessibility service enabled. Tap any editable text field to show the QuietType floating mic button."
    } else {
        "Accessibility service not enabled. Open Android Accessibility settings and enable QuietType."
    }

    fun overlayLabel(packageName: String?, state: OverlayDictationState): String {
        val prefix = when (state) {
            OverlayDictationState.Idle -> "🎙 QuietType"
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
