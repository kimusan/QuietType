package dk.schulz.quiettype.accessibility

import dk.schulz.quiettype.settings.AppSettings

data class FocusedFieldSnapshot(
    val packageName: String?,
    val className: String?,
    val isFocused: Boolean,
    val isEditable: Boolean,
    val isPassword: Boolean,
    val viewIdResourceName: String? = null,
    val hintText: String? = null,
)

enum class FocusedFieldHideReason {
    None,
    NotFocused,
    NotEditable,
    SensitiveField,
    UserHiddenTarget,
}

enum class HiddenFieldTargetScope(val label: String) {
    App("app"),
    Screen("screen"),
    Field("field"),
}

data class HiddenFieldTarget(
    val scope: HiddenFieldTargetScope,
    val packageName: String,
    val className: String? = null,
    val viewIdResourceName: String? = null,
    val label: String? = null,
) {
    fun matches(snapshot: FocusedFieldSnapshot): Boolean {
        if (snapshot.packageName != packageName) return false
        return when (scope) {
            HiddenFieldTargetScope.App -> true
            HiddenFieldTargetScope.Screen -> className != null && snapshot.className == className
            HiddenFieldTargetScope.Field -> className != null &&
                snapshot.className == className &&
                viewIdResourceName != null &&
                snapshot.viewIdResourceName == viewIdResourceName
        }
    }

    val displayName: String
        get() = when (scope) {
            HiddenFieldTargetScope.App -> packageName
            HiddenFieldTargetScope.Screen -> "$packageName · ${className.orEmpty()}"
            HiddenFieldTargetScope.Field -> "$packageName · ${label ?: viewIdResourceName.orEmpty()}"
        }

    companion object {
        fun forApp(packageName: String): HiddenFieldTarget = HiddenFieldTarget(
            scope = HiddenFieldTargetScope.App,
            packageName = packageName,
        )

        fun forScreen(packageName: String, className: String, label: String? = null): HiddenFieldTarget = HiddenFieldTarget(
            scope = HiddenFieldTargetScope.Screen,
            packageName = packageName,
            className = className,
            label = label,
        )

        fun forField(
            packageName: String,
            className: String,
            viewIdResourceName: String,
            label: String? = null,
        ): HiddenFieldTarget = HiddenFieldTarget(
            scope = HiddenFieldTargetScope.Field,
            packageName = packageName,
            className = className,
            viewIdResourceName = viewIdResourceName,
            label = label,
        )

        fun bestFor(snapshot: FocusedFieldSnapshot): HiddenFieldTarget? {
            val packageName = snapshot.packageName?.takeIf { it.isNotBlank() } ?: return null
            val className = snapshot.className?.takeIf { it.isNotBlank() }
            val viewId = snapshot.viewIdResourceName?.takeIf { it.isNotBlank() }
            val label = snapshot.hintText?.takeIf { it.isNotBlank() } ?: viewId
            return when {
                className != null && viewId != null -> forField(packageName, className, viewId, label)
                className != null -> forScreen(packageName, className, label)
                else -> forApp(packageName)
            }
        }
    }
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
        settings.hideInSensitiveFields && FocusedFieldSensitivity.isSensitive(snapshot) ->
            FocusedFieldDetection.hidden(FocusedFieldHideReason.SensitiveField, snapshot)
        settings.hiddenTargets.any { it.matches(snapshot) } ->
            FocusedFieldDetection.hidden(FocusedFieldHideReason.UserHiddenTarget, snapshot)

        else -> FocusedFieldDetection(
            shouldShowOverlay = true,
            hideReason = FocusedFieldHideReason.None,
            packageName = snapshot.packageName,
            className = snapshot.className,
        )
    }
}
