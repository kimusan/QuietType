package dk.schulz.quiettype.settings

import android.content.Context
import android.content.SharedPreferences

class AppSettingsStore(
    context: Context,
) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE,
    )

    fun load(): AppSettings = AppSettingsCodec.decode(
        mapOf(
            OnboardingComplete to preferences.getString(OnboardingComplete, null).orEmpty(),
            DictationInteractionKey to preferences.getString(DictationInteractionKey, null).orEmpty(),
            OfflineOnly to preferences.getString(OfflineOnly, null).orEmpty(),
            TranscriptHistoryEnabled to preferences.getString(TranscriptHistoryEnabled, null).orEmpty(),
            LiveSentenceInsertionEnabled to preferences.getString(LiveSentenceInsertionEnabled, null).orEmpty(),
            CorrectionModelEnabled to preferences.getString(CorrectionModelEnabled, null).orEmpty(),
            SelectedCorrectionModelId to preferences.getString(SelectedCorrectionModelId, null).orEmpty(),
            DownloadedCorrectionModelIds to preferences.getString(DownloadedCorrectionModelIds, null).orEmpty(),
            HideInSensitiveFields to preferences.getString(HideInSensitiveFields, null).orEmpty(),
            SelectedModelId to preferences.getString(SelectedModelId, null).orEmpty(),
            SelectedLanguageProfileId to preferences.getString(SelectedLanguageProfileId, null).orEmpty(),
            DownloadedModelIds to preferences.getString(DownloadedModelIds, null).orEmpty(),
            PreparedModelIds to preferences.getString(PreparedModelIds, null).orEmpty(),
            OverlayOffsetXDp to preferences.getString(OverlayOffsetXDp, null).orEmpty(),
            OverlayOffsetYDp to preferences.getString(OverlayOffsetYDp, null).orEmpty(),
            OverlayColorPresetKey to preferences.getString(OverlayColorPresetKey, null).orEmpty(),
            HiddenTargets to preferences.getString(HiddenTargets, null).orEmpty(),
        ).filterValues { it.isNotEmpty() },
    )

    fun save(settings: AppSettings) {
        preferences.edit().apply {
            AppSettingsCodec.encode(settings).forEach { (key, value) ->
                putString(key, value)
            }
        }.apply()
    }

    companion object {
        private const val PreferencesName = "quiettype_settings"
        private const val OnboardingComplete = "onboardingComplete"
        private const val DictationInteractionKey = "dictationInteraction"
        private const val OfflineOnly = "offlineOnly"
        private const val TranscriptHistoryEnabled = "transcriptHistoryEnabled"
        private const val LiveSentenceInsertionEnabled = "liveSentenceInsertionEnabled"
        private const val CorrectionModelEnabled = "correctionModelEnabled"
        private const val SelectedCorrectionModelId = "selectedCorrectionModelId"
        private const val DownloadedCorrectionModelIds = "downloadedCorrectionModelIds"
        private const val HideInSensitiveFields = "hideInSensitiveFields"
        private const val SelectedModelId = "selectedModelId"
        private const val SelectedLanguageProfileId = "selectedLanguageProfileId"
        private const val DownloadedModelIds = "downloadedModelIds"
        private const val PreparedModelIds = "preparedModelIds"
        private const val OverlayOffsetXDp = "overlayOffsetXDp"
        private const val OverlayOffsetYDp = "overlayOffsetYDp"
        private const val OverlayColorPresetKey = "overlayColorPreset"
        private const val HiddenTargets = "hiddenTargets"
    }
}
