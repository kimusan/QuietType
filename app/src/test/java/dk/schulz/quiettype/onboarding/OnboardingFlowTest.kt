package dk.schulz.quiettype.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingFlowTest {
    @Test
    fun onboardingIncludesConcreteSetupActions() {
        val flow = OnboardingFlow.default()

        assertEquals(
            OnboardingAction.OpenAccessibilitySettings,
            flow.steps.first { it.id == OnboardingStepId.InteractionMode }.action,
        )
        assertEquals(
            OnboardingAction.RequestMicrophonePermission,
            flow.steps.first { it.id == OnboardingStepId.Microphone }.action,
        )
        assertEquals(
            OnboardingAction.OpenModels,
            flow.steps.first { it.id == OnboardingStepId.OfflineModel }.action,
        )
    }

    @Test
    fun onboardingActionsHaveUserFacingLabels() {
        val actionableSteps = OnboardingFlow.default().steps.filter { it.action != OnboardingAction.None }

        assertTrue(actionableSteps.all { it.actionLabel.isNotBlank() })
        assertTrue(actionableSteps.any { it.actionLabel.contains("Accessibility") })
        assertTrue(actionableSteps.any { it.actionLabel.contains("microphone", ignoreCase = true) })
        assertTrue(actionableSteps.any { it.actionLabel.contains("models", ignoreCase = true) })
    }

    @Test
    fun onboardingActionLabelsReflectAlreadyGrantedPermissions() {
        val status = OnboardingPermissionStatus(
            isAccessibilityEnabled = true,
            hasMicrophonePermission = true,
            isSelectedModelReady = true,
        )

        assertEquals(
            "Accessibility already enabled",
            OnboardingActionLabel.forStep(
                step = OnboardingFlow.default().steps.first { it.id == OnboardingStepId.InteractionMode },
                status = status,
            ),
        )
        assertEquals(
            "Microphone already allowed",
            OnboardingActionLabel.forStep(
                step = OnboardingFlow.default().steps.first { it.id == OnboardingStepId.Microphone },
                status = status,
            ),
        )
        assertEquals(
            "Model ready",
            OnboardingActionLabel.forStep(
                step = OnboardingFlow.default().steps.first { it.id == OnboardingStepId.OfflineModel },
                status = status,
            ),
        )
    }
}
