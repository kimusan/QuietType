package dk.schulz.voiceme.settings

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
            HideInSensitiveFields to preferences.getString(HideInSensitiveFields, null).orEmpty(),
            SelectedModelId to preferences.getString(SelectedModelId, null).orEmpty(),
            DownloadedModelIds to preferences.getString(DownloadedModelIds, null).orEmpty(),
            PreparedModelIds to preferences.getString(PreparedModelIds, null).orEmpty(),
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
        private const val PreferencesName = "voiceme_settings"
        private const val OnboardingComplete = "onboardingComplete"
        private const val DictationInteractionKey = "dictationInteraction"
        private const val OfflineOnly = "offlineOnly"
        private const val TranscriptHistoryEnabled = "transcriptHistoryEnabled"
        private const val HideInSensitiveFields = "hideInSensitiveFields"
        private const val SelectedModelId = "selectedModelId"
        private const val DownloadedModelIds = "downloadedModelIds"
        private const val PreparedModelIds = "preparedModelIds"
    }
}
