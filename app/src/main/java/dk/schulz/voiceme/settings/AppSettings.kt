package dk.schulz.voiceme.settings

import dk.schulz.voiceme.models.ModelCatalog

enum class DictationInteraction {
    HoldToTalk,
    TapToToggle,
}

data class AppSettings(
    val onboardingComplete: Boolean,
    val dictationInteraction: DictationInteraction,
    val offlineOnly: Boolean,
    val transcriptHistoryEnabled: Boolean,
    val hideInSensitiveFields: Boolean,
    val selectedModelId: String,
    val downloadedModelIds: Set<String>,
    val preparedModelIds: Set<String>,
) {
    fun completeOnboarding(): AppSettings = copy(onboardingComplete = true)

    fun withDictationInteraction(interaction: DictationInteraction): AppSettings =
        copy(dictationInteraction = interaction)

    companion object {
        fun default(): AppSettings = AppSettings(
            onboardingComplete = false,
            dictationInteraction = DictationInteraction.HoldToTalk,
            offlineOnly = true,
            transcriptHistoryEnabled = false,
            hideInSensitiveFields = true,
            selectedModelId = ModelCatalog.default().recommended.id,
            downloadedModelIds = emptySet(),
            preparedModelIds = emptySet(),
        )
    }
}

object AppSettingsCodec {
    private const val OnboardingComplete = "onboardingComplete"
    private const val DictationInteractionKey = "dictationInteraction"
    private const val OfflineOnly = "offlineOnly"
    private const val TranscriptHistoryEnabled = "transcriptHistoryEnabled"
    private const val HideInSensitiveFields = "hideInSensitiveFields"
    private const val SelectedModelId = "selectedModelId"
    private const val DownloadedModelIds = "downloadedModelIds"
    private const val PreparedModelIds = "preparedModelIds"

    fun encode(settings: AppSettings): Map<String, String> = mapOf(
        OnboardingComplete to settings.onboardingComplete.toString(),
        DictationInteractionKey to settings.dictationInteraction.name,
        OfflineOnly to settings.offlineOnly.toString(),
        TranscriptHistoryEnabled to settings.transcriptHistoryEnabled.toString(),
        HideInSensitiveFields to settings.hideInSensitiveFields.toString(),
        SelectedModelId to settings.selectedModelId,
        DownloadedModelIds to settings.downloadedModelIds.sorted().joinToString(","),
        PreparedModelIds to settings.preparedModelIds.sorted().joinToString(","),
    )

    fun decode(values: Map<String, String>): AppSettings {
        val defaults = AppSettings.default()
        return AppSettings(
            onboardingComplete = values[OnboardingComplete]?.toBooleanStrictOrNull()
                ?: defaults.onboardingComplete,
            dictationInteraction = values[DictationInteractionKey]?.let(::decodeInteraction)
                ?: defaults.dictationInteraction,
            offlineOnly = values[OfflineOnly]?.toBooleanStrictOrNull()
                ?: defaults.offlineOnly,
            transcriptHistoryEnabled = values[TranscriptHistoryEnabled]?.toBooleanStrictOrNull()
                ?: defaults.transcriptHistoryEnabled,
            hideInSensitiveFields = values[HideInSensitiveFields]?.toBooleanStrictOrNull()
                ?: defaults.hideInSensitiveFields,
            selectedModelId = values[SelectedModelId]?.takeIf { it.isNotBlank() }
                ?: defaults.selectedModelId,
            downloadedModelIds = values[DownloadedModelIds]?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: defaults.downloadedModelIds,
            preparedModelIds = values[PreparedModelIds]?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: defaults.preparedModelIds,
        )
    }

    private fun decodeInteraction(value: String): DictationInteraction? =
        DictationInteraction.entries.firstOrNull { it.name == value }
}
