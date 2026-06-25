package dk.schulz.quiettype.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun setupStepsBlockContinueUntilRequiredTaskIsComplete() {
        val flow = OnboardingFlow.default()
        val missingAll = OnboardingPermissionStatus(
            isAccessibilityEnabled = false,
            hasMicrophonePermission = false,
            isSelectedModelReady = false,
        )
        val completeAll = OnboardingPermissionStatus(
            isAccessibilityEnabled = true,
            hasMicrophonePermission = true,
            isSelectedModelReady = true,
        )

        assertFalse(flow.canContinueFrom(flow.indexOf(OnboardingStepId.InteractionMode), missingAll))
        assertFalse(flow.canContinueFrom(flow.indexOf(OnboardingStepId.Microphone), missingAll))
        assertFalse(flow.canContinueFrom(flow.indexOf(OnboardingStepId.OfflineModel), missingAll))
        assertTrue(flow.canContinueFrom(flow.indexOf(OnboardingStepId.InteractionMode), completeAll))
        assertTrue(flow.canContinueFrom(flow.indexOf(OnboardingStepId.Microphone), completeAll))
        assertTrue(flow.canContinueFrom(flow.indexOf(OnboardingStepId.OfflineModel), completeAll))
    }

    @Test
    fun setupStepsExplainBlockedContinueReason() {
        val flow = OnboardingFlow.default()
        val status = OnboardingPermissionStatus(
            isAccessibilityEnabled = false,
            hasMicrophonePermission = false,
            isSelectedModelReady = false,
        )

        assertEquals("Enable QuietType in Android Accessibility settings before continuing.", flow.blockedReason(flow.indexOf(OnboardingStepId.InteractionMode), status))
        assertEquals("Allow microphone access before continuing.", flow.blockedReason(flow.indexOf(OnboardingStepId.Microphone), status))
        assertEquals("Download and prepare a dictation model before finishing setup.", flow.blockedReason(flow.indexOf(OnboardingStepId.OfflineModel), status))
    }

}
