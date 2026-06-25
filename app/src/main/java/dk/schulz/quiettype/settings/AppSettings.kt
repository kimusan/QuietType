package dk.schulz.quiettype.settings

import dk.schulz.quiettype.accessibility.HiddenFieldTarget
import dk.schulz.quiettype.accessibility.HiddenFieldTargetScope
import dk.schulz.quiettype.accessibility.OverlayPlacementPolicy
import dk.schulz.quiettype.models.ModelCatalog

enum class DictationInteraction {
    HoldToTalk,
    TapToToggle,
}

enum class OverlayColorPreset(val displayName: String, val idleColor: Int, val listeningColor: Int) {
    Teal("Teal", 0xFF00695C.toInt(), 0xFFC62828.toInt()),
    Purple("Purple", 0xFF6A1B9A.toInt(), 0xFFC62828.toInt()),
    Blue("Blue", 0xFF1565C0.toInt(), 0xFFC62828.toInt()),
    Orange("Orange", 0xFFEF6C00.toInt(), 0xFFC62828.toInt()),
    Pink("Pink", 0xFFC2185B.toInt(), 0xFFC62828.toInt()),
}

data class AppSettings(
    val onboardingComplete: Boolean,
    val dictationInteraction: DictationInteraction,
    val offlineOnly: Boolean,
    val transcriptHistoryEnabled: Boolean,
    val liveSentenceInsertionEnabled: Boolean,
    val hideInSensitiveFields: Boolean,
    val selectedModelId: String,
    val downloadedModelIds: Set<String>,
    val preparedModelIds: Set<String>,
    val overlayOffsetXDp: Int,
    val overlayOffsetYDp: Int,
    val overlayColorPreset: OverlayColorPreset,
    val hiddenTargets: List<HiddenFieldTarget>,
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
            liveSentenceInsertionEnabled = false,
            hideInSensitiveFields = true,
            selectedModelId = ModelCatalog.default().recommended.id,
            downloadedModelIds = emptySet(),
            preparedModelIds = emptySet(),
            overlayOffsetXDp = OverlayPlacementPolicy.DefaultPosition.xDp,
            overlayOffsetYDp = OverlayPlacementPolicy.DefaultPosition.yDp,
            overlayColorPreset = OverlayColorPreset.Teal,
            hiddenTargets = emptyList(),
        )
    }
}

object AppSettingsCodec {
    private const val OnboardingComplete = "onboardingComplete"
    private const val DictationInteractionKey = "dictationInteraction"
    private const val OfflineOnly = "offlineOnly"
    private const val TranscriptHistoryEnabled = "transcriptHistoryEnabled"
    private const val LiveSentenceInsertionEnabled = "liveSentenceInsertionEnabled"
    private const val HideInSensitiveFields = "hideInSensitiveFields"
    private const val SelectedModelId = "selectedModelId"
    private const val DownloadedModelIds = "downloadedModelIds"
    private const val PreparedModelIds = "preparedModelIds"
    private const val OverlayOffsetXDp = "overlayOffsetXDp"
    private const val OverlayOffsetYDp = "overlayOffsetYDp"
    private const val OverlayColorPresetKey = "overlayColorPreset"
    private const val HiddenTargets = "hiddenTargets"

    fun encode(settings: AppSettings): Map<String, String> = mapOf(
        OnboardingComplete to settings.onboardingComplete.toString(),
        DictationInteractionKey to settings.dictationInteraction.name,
        OfflineOnly to settings.offlineOnly.toString(),
        TranscriptHistoryEnabled to settings.transcriptHistoryEnabled.toString(),
        LiveSentenceInsertionEnabled to settings.liveSentenceInsertionEnabled.toString(),
        HideInSensitiveFields to settings.hideInSensitiveFields.toString(),
        SelectedModelId to settings.selectedModelId,
        DownloadedModelIds to settings.downloadedModelIds.sorted().joinToString(","),
        PreparedModelIds to settings.preparedModelIds.sorted().joinToString(","),
        OverlayOffsetXDp to settings.overlayOffsetXDp.toString(),
        OverlayOffsetYDp to settings.overlayOffsetYDp.toString(),
        OverlayColorPresetKey to settings.overlayColorPreset.name,
        HiddenTargets to encodeHiddenTargets(settings.hiddenTargets),
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
            liveSentenceInsertionEnabled = values[LiveSentenceInsertionEnabled]?.toBooleanStrictOrNull()
                ?: defaults.liveSentenceInsertionEnabled,
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
            overlayColorPreset = values[OverlayColorPresetKey]?.let(::decodeOverlayColorPreset)
                ?: defaults.overlayColorPreset,
            hiddenTargets = values[HiddenTargets]?.let(::decodeHiddenTargets)
                ?: defaults.hiddenTargets,
        )
    }

    private fun decodeInteraction(value: String): DictationInteraction? =
        DictationInteraction.entries.firstOrNull { it.name == value }

    private fun decodeOverlayColorPreset(value: String): OverlayColorPreset? =
        OverlayColorPreset.entries.firstOrNull { it.name == value }

    private fun encodeHiddenTargets(targets: List<HiddenFieldTarget>): String = targets.joinToString("\n") { target ->
        listOf(
            target.scope.name,
            target.packageName,
            target.className.orEmpty(),
            target.viewIdResourceName.orEmpty(),
            target.label.orEmpty(),
        ).joinToString("|") { it.replace("|", " ").replace("\n", " ") }
    }

    private fun decodeHiddenTargets(value: String): List<HiddenFieldTarget> = value.lines()
        .mapNotNull { line ->
            val parts = line.split('|')
            if (parts.size < 2) return@mapNotNull null
            val scope = HiddenFieldTargetScope.entries.firstOrNull { it.name == parts[0] } ?: return@mapNotNull null
            val packageName = parts[1].takeIf { it.isNotBlank() } ?: return@mapNotNull null
            HiddenFieldTarget(
                scope = scope,
                packageName = packageName,
                className = parts.getOrNull(2)?.takeIf { it.isNotBlank() },
                viewIdResourceName = parts.getOrNull(3)?.takeIf { it.isNotBlank() },
                label = parts.getOrNull(4)?.takeIf { it.isNotBlank() },
            )
        }

}
