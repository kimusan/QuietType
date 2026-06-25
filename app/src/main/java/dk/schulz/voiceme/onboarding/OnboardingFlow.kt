package dk.schulz.voiceme.onboarding

enum class OnboardingStepId {
    Welcome,
    Privacy,
    InteractionMode,
    Microphone,
    OfflineModel,
}

enum class OnboardingAction {
    None,
    OpenAccessibilitySettings,
    RequestMicrophonePermission,
    OpenModels,
}

data class OnboardingStep(
    val id: OnboardingStepId,
    val eyebrow: String,
    val title: String,
    val body: String,
    val primaryAction: String = "Continue",
    val action: OnboardingAction = OnboardingAction.None,
    val actionLabel: String = "",
)

data class OnboardingPermissionStatus(
    val isAccessibilityEnabled: Boolean,
    val hasMicrophonePermission: Boolean,
    val isSelectedModelReady: Boolean,
)

object OnboardingActionLabel {
    fun forStep(step: OnboardingStep, status: OnboardingPermissionStatus): String = when (step.action) {
        OnboardingAction.OpenAccessibilitySettings -> if (status.isAccessibilityEnabled) {
            "Accessibility already enabled"
        } else {
            step.actionLabel
        }
        OnboardingAction.RequestMicrophonePermission -> if (status.hasMicrophonePermission) {
            "Microphone already allowed"
        } else {
            step.actionLabel
        }
        OnboardingAction.OpenModels -> if (status.isSelectedModelReady) {
            "Model ready"
        } else {
            step.actionLabel
        }
        OnboardingAction.None -> step.actionLabel
    }
}

data class OnboardingFlow(
    val steps: List<OnboardingStep>,
) {
    init {
        require(steps.isNotEmpty()) { "Onboarding flow must contain at least one step." }
    }

    val totalSteps: Int = steps.size
    val lastStepIndex: Int = steps.lastIndex

    fun currentStep(index: Int): OnboardingStep = steps[index.coerceIn(0, lastStepIndex)]

    fun nextIndex(index: Int): Int = (index + 1).coerceAtMost(lastStepIndex)

    fun previousIndex(index: Int): Int = (index - 1).coerceAtLeast(0)

    fun isComplete(index: Int): Boolean = index >= totalSteps

    companion object {
        fun default(): OnboardingFlow = OnboardingFlow(
            steps = listOf(
                OnboardingStep(
                    id = OnboardingStepId.Welcome,
                    eyebrow = "Step 1 of 5",
                    title = "Private voice dictation for any text field.",
                    body = "VoiceMe will appear only when you need it, next to your normal keyboard, so typing still works as usual.",
                    primaryAction = "Start setup",
                ),
                OnboardingStep(
                    id = OnboardingStepId.Privacy,
                    eyebrow = "Step 2 of 5",
                    title = "Your voice stays on this phone.",
                    body = "VoiceMe is designed for offline speech recognition, no telemetry, and no transcript history by default.",
                ),
                OnboardingStep(
                    id = OnboardingStepId.InteractionMode,
                    eyebrow = "Step 3 of 5",
                    title = "Enable the floating mic button.",
                    body = "VoiceMe uses Android Accessibility to notice focused editable fields and place a draggable mic button beside the keyboard. This permission must be enabled manually in Android settings.",
                    action = OnboardingAction.OpenAccessibilitySettings,
                    actionLabel = "Open Accessibility settings",
                ),
                OnboardingStep(
                    id = OnboardingStepId.Microphone,
                    eyebrow = "Step 4 of 5",
                    title = "Allow microphone access.",
                    body = "VoiceMe asks for microphone access only when you explicitly tap this step. Audio capture is local and starts later only while you hold or toggle the floating dictation button.",
                    action = OnboardingAction.RequestMicrophonePermission,
                    actionLabel = "Allow microphone",
                ),
                OnboardingStep(
                    id = OnboardingStepId.OfflineModel,
                    eyebrow = "Step 5 of 5",
                    title = "Choose an offline model.",
                    body = "VoiceMe defaults to a compact multilingual model candidate. Open Models to review language coverage, size, license notes, and checksum before downloading.",
                    primaryAction = "Finish setup",
                    action = OnboardingAction.OpenModels,
                    actionLabel = "Open Models",
                ),
            ),
        )
    }
}
