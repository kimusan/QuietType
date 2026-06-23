package dk.schulz.voiceme.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    @Test
    fun defaultSettingsKeepDictationPrivateAndNonIntrusive() {
        val settings = AppSettings.default()

        assertFalse(settings.onboardingComplete)
        assertEquals(DictationInteraction.HoldToTalk, settings.dictationInteraction)
        assertTrue(settings.offlineOnly)
        assertFalse(settings.transcriptHistoryEnabled)
        assertTrue(settings.hideInSensitiveFields)
        assertEquals(16, settings.overlayOffsetXDp)
        assertTrue(settings.overlayOffsetYDp >= 320)
    }

    @Test
    fun settingsCanBeUpdatedImmutablyAfterOnboarding() {
        val updated = AppSettings.default()
            .completeOnboarding()
            .withDictationInteraction(DictationInteraction.TapToToggle)

        assertTrue(updated.onboardingComplete)
        assertEquals(DictationInteraction.TapToToggle, updated.dictationInteraction)
        assertTrue(updated.offlineOnly)
    }

    @Test
    fun settingsCodecRoundTripsKnownValuesAndRejectsUnsafeTranscriptHistoryDefault() {
        val original = AppSettings(
            onboardingComplete = true,
            dictationInteraction = DictationInteraction.TapToToggle,
            offlineOnly = true,
            transcriptHistoryEnabled = false,
            hideInSensitiveFields = true,
            selectedModelId = AppSettings.default().selectedModelId,
            downloadedModelIds = setOf("sherpa-onnx-streaming-zipformer-en-int8"),
            preparedModelIds = setOf("sherpa-onnx-streaming-zipformer-en-int8"),
            overlayOffsetXDp = 42,
            overlayOffsetYDp = 360,
        )

        val restored = AppSettingsCodec.decode(AppSettingsCodec.encode(original))

        assertEquals(original, restored)
        assertFalse(AppSettingsCodec.decode(emptyMap()).transcriptHistoryEnabled)
    }
}
