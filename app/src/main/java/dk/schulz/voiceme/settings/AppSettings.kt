package dk.schulz.voiceme.settings

import dk.schulz.voiceme.accessibility.OverlayPlacementPolicy
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
    val overlayOffsetXDp: Int,
    val overlayOffsetYDp: Int,
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
            overlayOffsetXDp = OverlayPlacementPolicy.DefaultPosition.xDp,
            overlayOffsetYDp = OverlayPlacementPolicy.DefaultPosition.yDp,
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
    private const val OverlayOffsetXDp = "overlayOffsetXDp"
    private const val OverlayOffsetYDp = "overlayOffsetYDp"

    fun encode(settings: AppSettings): Map<String, String> = mapOf(
        OnboardingComplete to settings.onboardingComplete.toString(),
        DictationInteractionKey to settings.dictationInteraction.name,
        OfflineOnly to settings.offlineOnly.toString(),
        TranscriptHistoryEnabled to settings.transcriptHistoryEnabled.toString(),
        HideInSensitiveFields to settings.hideInSensitiveFields.toString(),
        SelectedModelId to settings.selectedModelId,
        DownloadedModelIds to settings.downloadedModelIds.sorted().joinToString(","),
        PreparedModelIds to settings.preparedModelIds.sorted().joinToString(","),
        OverlayOffsetXDp to settings.overlayOffsetXDp.toString(),
        OverlayOffsetYDp to settings.overlayOffsetYDp.toString(),
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
            overlayOffsetXDp = values[OverlayOffsetXDp]?.toIntOrNull()?.coerceAtLeast(0)
                ?: defaults.overlayOffsetXDp,
            overlayOffsetYDp = values[OverlayOffsetYDp]?.toIntOrNull()?.coerceAtLeast(0)
                ?: defaults.overlayOffsetYDp,
        )
    }

    private fun decodeInteraction(value: String): DictationInteraction? =
        DictationInteraction.entries.firstOrNull { it.name == value }
}
