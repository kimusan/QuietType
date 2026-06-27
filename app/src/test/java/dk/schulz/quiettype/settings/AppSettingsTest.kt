package dk.schulz.quiettype.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import dk.schulz.quiettype.accessibility.HiddenFieldTarget
import org.junit.Test

class AppSettingsTest {
    @Test
    fun defaultSettingsKeepDictationPrivateAndNonIntrusive() {
        val settings = AppSettings.default()

        assertFalse(settings.onboardingComplete)
        assertEquals(DictationInteraction.HoldToTalk, settings.dictationInteraction)
        assertTrue(settings.offlineOnly)
        assertFalse(settings.transcriptHistoryEnabled)
        assertFalse(settings.liveSentenceInsertionEnabled)
        assertFalse(settings.correctionModelEnabled)
        assertEquals("deterministic-cleanup", settings.selectedCorrectionModelId)
        assertTrue(settings.downloadedCorrectionModelIds.isEmpty())
        assertTrue(settings.hideInSensitiveFields)
        assertEquals("da-multilingual", settings.selectedLanguageProfileId)
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
            liveSentenceInsertionEnabled = true,
            correctionModelEnabled = true,
            selectedCorrectionModelId = "smollm2-360m-instruct-q4-k-m",
            downloadedCorrectionModelIds = setOf("smollm2-360m-instruct-q4-k-m"),
            hideInSensitiveFields = true,
            selectedModelId = AppSettings.default().selectedModelId,
            selectedLanguageProfileId = "en-fast",
            downloadedModelIds = setOf("sherpa-onnx-streaming-zipformer-en-int8"),
            preparedModelIds = setOf("sherpa-onnx-streaming-zipformer-en-int8"),
            overlayOffsetXDp = 42,
            overlayOffsetYDp = 360,
            overlayColorPreset = OverlayColorPreset.Teal,
            hiddenTargets = emptyList(),
        )

        val restored = AppSettingsCodec.decode(AppSettingsCodec.encode(original))

        assertEquals(original, restored)
        assertFalse(AppSettingsCodec.decode(emptyMap()).transcriptHistoryEnabled)
        assertFalse(AppSettingsCodec.decode(emptyMap()).liveSentenceInsertionEnabled)
    }

    @Test
    fun defaultOverlayColorPresetIsPersisted() {
        val settings = AppSettings.default().copy(overlayColorPreset = OverlayColorPreset.Purple)

        val restored = AppSettingsCodec.decode(AppSettingsCodec.encode(settings))

        assertEquals(OverlayColorPreset.Purple, restored.overlayColorPreset)
    }


    @Test
    fun hiddenTargetsRoundTrip() {
        val settings = AppSettings.default().copy(
            hiddenTargets = listOf(
                HiddenFieldTarget.forField(
                    packageName = "com.example.notes",
                    className = "com.example.notes.EditorActivity",
                    viewIdResourceName = "com.example.notes:id/body",
                    label = "Body",
                ),
            ),
        )

        val restored = AppSettingsCodec.decode(AppSettingsCodec.encode(settings))

        assertEquals(settings.hiddenTargets, restored.hiddenTargets)
    }
}
