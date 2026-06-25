package dk.schulz.quiettype.accessibility

import dk.schulz.quiettype.settings.AppSettings

data class FocusedFieldSnapshot(
    val packageName: String?,
    val className: String?,
    val isFocused: Boolean,
    val isEditable: Boolean,
    val isPassword: Boolean,
)

enum class FocusedFieldHideReason {
    None,
    NotFocused,
    NotEditable,
    SensitiveField,
}

data class FocusedFieldDetection(
    val shouldShowOverlay: Boolean,
    val hideReason: FocusedFieldHideReason,
    val packageName: String?,
    val className: String?,
) {
    companion object {
        fun hidden(reason: FocusedFieldHideReason, snapshot: FocusedFieldSnapshot): FocusedFieldDetection =
            FocusedFieldDetection(
                shouldShowOverlay = false,
                hideReason = reason,
                packageName = snapshot.packageName,
                className = snapshot.className,
            )
    }
}

object FocusedFieldDetector {
    fun detect(snapshot: FocusedFieldSnapshot, settings: AppSettings): FocusedFieldDetection = when {
        !snapshot.isFocused -> FocusedFieldDetection.hidden(FocusedFieldHideReason.NotFocused, snapshot)
        !snapshot.isEditable -> FocusedFieldDetection.hidden(FocusedFieldHideReason.NotEditable, snapshot)
        settings.hideInSensitiveFields && snapshot.isPassword ->
            FocusedFieldDetection.hidden(FocusedFieldHideReason.SensitiveField, snapshot)

        else -> FocusedFieldDetection(
            shouldShowOverlay = true,
            hideReason = FocusedFieldHideReason.None,
            packageName = snapshot.packageName,
            className = snapshot.className,
        )
    }
}
