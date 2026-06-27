package dk.schulz.quiettype.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dk.schulz.quiettype.R
import dk.schulz.quiettype.accessibility.QuietTypeAccessibilityPresentation
import dk.schulz.quiettype.correction.CorrectionModelCatalog
import dk.schulz.quiettype.dictation.DictationBlockReason
import dk.schulz.quiettype.dictation.DictationSessionState
import dk.schulz.quiettype.history.DictationHistoryEntry
import dk.schulz.quiettype.models.LanguageProfile
import dk.schulz.quiettype.models.ModelCatalogState
import dk.schulz.quiettype.models.ModelDownloadProgress
import dk.schulz.quiettype.models.VoiceModel
import dk.schulz.quiettype.onboarding.OnboardingAction
import dk.schulz.quiettype.onboarding.OnboardingActionLabel
import dk.schulz.quiettype.onboarding.OnboardingFlow
import dk.schulz.quiettype.onboarding.OnboardingPermissionStatus
import dk.schulz.quiettype.onboarding.OnboardingStep
import dk.schulz.quiettype.settings.AppSettings
import dk.schulz.quiettype.settings.DictationInteraction
import dk.schulz.quiettype.settings.OverlayColorPreset
import dk.schulz.quiettype.ui.theme.QuietTypeTheme
import java.text.DateFormat
import java.util.Date

@Composable
fun QuietTypeApp(
    appSettings: AppSettings = AppSettings.default(),
    dictationState: DictationSessionState = DictationSessionState.idle(hasMicrophonePermission = false),
    modelCatalogState: ModelCatalogState = ModelCatalogState.default(),
    modelDownloadStatus: String? = null,
    modelDownloadProgress: ModelDownloadProgress? = null,
    isModelDownloadActive: Boolean = false,
    isAccessibilityEnabled: Boolean = false,
    historyEntries: List<DictationHistoryEntry> = emptyList(),
    onSettingsChange: (AppSettings) -> Unit = {},
    onOpenAccessibilitySettings: () -> Unit = {},
    onRequestMicrophonePermission: () -> Unit = {},
    onStartRecordingShell: () -> Unit = {},
    onStopRecordingShell: () -> Unit = {},
    onSelectModel: (String) -> Unit = {},
    onSelectLanguageProfile: (String) -> Unit = {},
    onDownloadModel: (String) -> Unit = {},
    onDeleteModel: (String) -> Unit = {},
    onSelectCorrectionModel: (String) -> Unit = {},
    onDeleteCorrectionModel: (String) -> Unit = {},
    onCopyHistoryEntry: (DictationHistoryEntry) -> Unit = {},
    onDeleteHistoryEntry: (String) -> Unit = {},
    onClearHistory: () -> Unit = {},
) {
    QuietTypeTheme {
        QuietTypeHomeScreen(
            appSettings = appSettings,
            dictationState = dictationState,
            modelCatalogState = modelCatalogState,
            modelDownloadStatus = modelDownloadStatus,
            modelDownloadProgress = modelDownloadProgress,
            isModelDownloadActive = isModelDownloadActive,
            isAccessibilityEnabled = isAccessibilityEnabled,
            historyEntries = historyEntries,
            onSettingsChange = onSettingsChange,
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            onRequestMicrophonePermission = onRequestMicrophonePermission,
            onStartRecordingShell = onStartRecordingShell,
            onStopRecordingShell = onStopRecordingShell,
            onSelectModel = onSelectModel,
            onSelectLanguageProfile = onSelectLanguageProfile,
            onDownloadModel = onDownloadModel,
            onDeleteModel = onDeleteModel,
            onSelectCorrectionModel = onSelectCorrectionModel,
            onDeleteCorrectionModel = onDeleteCorrectionModel,
            onCopyHistoryEntry = onCopyHistoryEntry,
            onDeleteHistoryEntry = onDeleteHistoryEntry,
            onClearHistory = onClearHistory,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuietTypeHomeScreen(
    appSettings: AppSettings,
    dictationState: DictationSessionState,
    modelCatalogState: ModelCatalogState,
    modelDownloadStatus: String?,
    modelDownloadProgress: ModelDownloadProgress?,
    isModelDownloadActive: Boolean,
    isAccessibilityEnabled: Boolean,
    historyEntries: List<DictationHistoryEntry>,
    onSettingsChange: (AppSettings) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onRequestMicrophonePermission: () -> Unit,
    onStartRecordingShell: () -> Unit,
    onStopRecordingShell: () -> Unit,
    onSelectModel: (String) -> Unit,
    onSelectLanguageProfile: (String) -> Unit,
    onDownloadModel: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
    onSelectCorrectionModel: (String) -> Unit,
    onDeleteCorrectionModel: (String) -> Unit,
    onCopyHistoryEntry: (DictationHistoryEntry) -> Unit,
    onDeleteHistoryEntry: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onboardingFlow = remember { OnboardingFlow.default() }
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var selectedSection by rememberSaveable {
        mutableIntStateOf(if (appSettings.onboardingComplete) 1 else 0)
    }
    val onboardingPermissionStatus = OnboardingPermissionStatus(
        isAccessibilityEnabled = isAccessibilityEnabled,
        hasMicrophonePermission = dictationState.hasMicrophonePermission,
        isSelectedModelReady = modelCatalogState.isReadyForDictation,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
        bottomBar = {
            SectionNavigationBar(
                selectedSection = selectedSection,
                onSelectedSectionChange = { selectedSection = it },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (selectedSection == 2) {
                QuietTypeModelsScreen(
                    appSettings = appSettings,
                    modelCatalogState = modelCatalogState,
                    modelDownloadStatus = modelDownloadStatus,
                    modelDownloadProgress = modelDownloadProgress,
                    isModelDownloadActive = isModelDownloadActive,
                    onSelectModel = onSelectModel,
                    onSelectLanguageProfile = onSelectLanguageProfile,
                    onDownloadModel = onDownloadModel,
                    onDeleteModel = onDeleteModel,
                    onSelectCorrectionModel = onSelectCorrectionModel,
                    onDeleteCorrectionModel = onDeleteCorrectionModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    when (selectedSection) {
                        0 -> QuietTypeOnboardingScreen(
                            step = onboardingFlow.currentStep(currentStep),
                            currentStep = currentStep,
                            totalSteps = onboardingFlow.totalSteps,
                            permissionStatus = onboardingPermissionStatus,
                            canContinue = onboardingFlow.canContinueFrom(currentStep, onboardingPermissionStatus),
                            blockedReason = onboardingFlow.blockedReason(currentStep, onboardingPermissionStatus),
                            onBack = { currentStep = onboardingFlow.previousIndex(currentStep) },
                            onNext = {
                                if (currentStep == onboardingFlow.lastStepIndex) {
                                    onSettingsChange(appSettings.completeOnboarding())
                                    selectedSection = 1
                                } else {
                                    currentStep = onboardingFlow.nextIndex(currentStep)
                                }
                            },
                            onStepAction = { action ->
                                when (action) {
                                    OnboardingAction.OpenAccessibilitySettings -> onOpenAccessibilitySettings()
                                    OnboardingAction.RequestMicrophonePermission -> onRequestMicrophonePermission()
                                    OnboardingAction.OpenModels -> selectedSection = 2
                                    OnboardingAction.None -> Unit
                                }
                            },
                        )

                        1 -> QuietTypeSettingsPreview(
                            appSettings = appSettings,
                            isAccessibilityEnabled = isAccessibilityEnabled,
                            dictationState = dictationState,
                            onSettingsChange = onSettingsChange,
                            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                            onRequestMicrophonePermission = onRequestMicrophonePermission,
                            onStartRecordingShell = onStartRecordingShell,
                            onStopRecordingShell = onStopRecordingShell,
                        )

                        3 -> QuietTypeHistoryScreen(
                            historyEntries = historyEntries,
                            historyEnabled = appSettings.transcriptHistoryEnabled,
                            onCopyHistoryEntry = onCopyHistoryEntry,
                                            onDeleteHistoryEntry = onDeleteHistoryEntry,
                            onClearHistory = onClearHistory,
                            onEnableHistory = { onSettingsChange(appSettings.copy(transcriptHistoryEnabled = true)) },
                        )

                        else -> QuietTypeAboutScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionNavigationBar(
    selectedSection: Int,
    onSelectedSectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier.fillMaxWidth()) {
        listOf("Setup", "Settings", "Models", "History", "About").forEachIndexed { index, label ->
            NavigationBarItem(
                selected = selectedSection == index,
                onClick = { onSelectedSectionChange(index) },
                label = { Text(label) },
                icon = { Text(sectionIcon(index)) },
            )
        }
    }
}

private fun sectionIcon(index: Int): String = when (index) {
    0 -> "①"
    1 -> "⚙"
    2 -> "↓"
    3 -> "↺"
    else -> "ℹ"
}

@Composable
private fun QuietTypeOnboardingScreen(
    step: OnboardingStep,
    currentStep: Int,
    totalSteps: Int,
    permissionStatus: OnboardingPermissionStatus,
    canContinue: Boolean,
    blockedReason: String?,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onStepAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LinearProgressIndicator(
            progress = { (currentStep + 1).toFloat() / totalSteps.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = step.eyebrow,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = step.body,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        ) {
            Text(
                text = onboardingHelpText(step),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (step.action != OnboardingAction.None) {
            Button(
                onClick = { onStepAction(step.action) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(OnboardingActionLabel.forStep(step, permissionStatus))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = currentStep > 0,
                modifier = Modifier.weight(1f),
            ) {
                Text("Back")
            }
            Button(
                onClick = onNext,
                enabled = canContinue,
                modifier = Modifier.weight(1f),
            ) {
                Text(step.primaryAction)
            }
        }
        if (!canContinue && blockedReason != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = blockedReason,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun QuietTypeStatusScreen(
    appSettings: AppSettings,
    dictationState: DictationSessionState,
    modelCatalogState: ModelCatalogState,
    isAccessibilityEnabled: Boolean,
    onReviewSetup: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onRequestMicrophonePermission: () -> Unit,
    onStartRecordingShell: () -> Unit,
    onStopRecordingShell: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (appSettings.onboardingComplete) {
                "QuietType setup preview complete"
            } else {
                "QuietType setup preview"
            },
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "When the Accessibility service is enabled, QuietType detects focused editable fields and shows a draggable microphone overlay. Download a prepared local model, then hold or toggle the overlay button to dictate into the focused field.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        StatusCard(
            title = "Privacy default",
            body = "Offline-only: ${yesNo(appSettings.offlineOnly)}. Transcript history: ${if (appSettings.transcriptHistoryEnabled) "on" else "off"}.",
        )
        StatusCard(
            title = "Input strategy",
            body = QuietTypeAccessibilityPresentation.statusText(isAccessibilityEnabled),
        )
        StatusCard(
            title = "Current dictation interaction",
            body = when (appSettings.dictationInteraction) {
                DictationInteraction.HoldToTalk -> "Hold-to-talk: safest default for accidental dictation."
                DictationInteraction.TapToToggle -> "Tap-to-toggle: easier for longer dictation once recording exists."
            },
        )
        StatusCard(
            title = "Microphone dictation",
            body = microphoneStatusText(dictationState),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = if (dictationState.hasMicrophonePermission) {
                    onStartRecordingShell
                } else {
                    onRequestMicrophonePermission
                },
                enabled = !dictationState.isRecording,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (dictationState.hasMicrophonePermission) "Start dictation" else "Allow microphone")
            }
            OutlinedButton(
                onClick = onStopRecordingShell,
                enabled = dictationState.isRecording,
                modifier = Modifier.weight(1f),
            ) {
                Text("Stop")
            }
        }
        if (dictationState.latestFinalTranscript.isNotBlank()) {
            StatusCard(
                title = "Latest dictation output",
                body = "Latest final segment: ${dictationState.latestFinalTranscript}.",
            )
        }
        StatusCard(
            title = "Selected local model",
            body = "${modelCatalogState.selectedModel.name}: ${modelCatalogState.selectedInstallState}. Open Models to prepare or delete the local model marker.",
        )
        OverlayPreviewCard()
        Button(
            onClick = onOpenAccessibilitySettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open Android Accessibility settings")
        }
        OutlinedButton(onClick = onReviewSetup) {
            Text("Review setup again")
        }
    }
}

@Composable
private fun OverlayPreviewCard(
    modifier: Modifier = Modifier,
) {
    var previewText by rememberSaveable { mutableStateOf("Tap here after enabling Accessibility to make the real QuietType overlay appear.") }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Floating button test",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Enable QuietType in Android Accessibility settings, then tap this editable field. The actual accessibility overlay should appear near the lower-right edge and can be dragged.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = previewText,
                onValueChange = { previewText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Overlay test input") },
                minLines = 2,
            )
        }
    }
}

private fun onboardingHelpText(step: OnboardingStep): String = when (step.action) {
    OnboardingAction.OpenAccessibilitySettings ->
        "Android will open system settings. Choose QuietType, enable the service, then return here and use the Status screen test field to see the real floating button."
    OnboardingAction.RequestMicrophonePermission ->
        "Android will show the microphone permission dialog now. Accepting it only grants access; QuietType uses the microphone later when you hold or toggle the floating dictation button."
    OnboardingAction.OpenModels ->
        "The Models screen shows the default multilingual Parakeet model, checksum, size, and download action. Verified sherpa archives are unpacked into private runtime files and marked prepared for offline dictation."
    OnboardingAction.None ->
        "No permission is requested on this step. Continue when you are ready."
}

@Composable
private fun QuietTypeSettingsPreview(
    appSettings: AppSettings,
    isAccessibilityEnabled: Boolean,
    dictationState: DictationSessionState,
    onSettingsChange: (AppSettings) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onRequestMicrophonePermission: () -> Unit,
    onStartRecordingShell: () -> Unit,
    onStopRecordingShell: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Settings preview",
            style = MaterialTheme.typography.headlineMedium,
        )
        SettingSwitchCard(
            title = "Tap to toggle dictation",
            body = "Off means hold-to-talk. This choice is saved locally now and will control recording behavior later.",
            checked = appSettings.dictationInteraction == DictationInteraction.TapToToggle,
            onCheckedChange = { enabled ->
                onSettingsChange(
                    appSettings.withDictationInteraction(
                        if (enabled) DictationInteraction.TapToToggle else DictationInteraction.HoldToTalk,
                    ),
                )
            },
        )
        SettingSwitchCard(
            title = "Experimental live insertion",
            body = "Insert stable words or phrases while a streaming recognizer is still listening. Experimental: offline Danish models still insert final text after stop.",
            checked = appSettings.liveSentenceInsertionEnabled,
            onCheckedChange = { enabled ->
                onSettingsChange(appSettings.copy(liveSentenceInsertionEnabled = enabled))
            },
        )
        SettingsSectionCard(title = "Fix text model") {
            SettingSwitchRow(
                title = "Use local correction model",
                body = "When enabled, Fix will use the selected correction model after its runtime is integrated. For now QuietType still falls back to built-in cleanup.",
                checked = appSettings.correctionModelEnabled,
                onCheckedChange = { enabled ->
                    onSettingsChange(appSettings.copy(correctionModelEnabled = enabled))
                },
            )
            val selectedCorrectionModel = CorrectionModelCatalog.default().modelById(appSettings.selectedCorrectionModelId)
            Text(
                text = selectedCorrectionModel?.let { "Selected: ${it.name}" } ?: "Selected: Fast local cleanup",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Download, switch, or delete correction models from the Models screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SettingSwitchCard(
            title = "Save local dictation history",
            body = "Off by default. When enabled, successful final dictations are saved only in private app storage so you can copy or delete them from History.",
            checked = appSettings.transcriptHistoryEnabled,
            onCheckedChange = { enabled ->
                onSettingsChange(appSettings.copy(transcriptHistoryEnabled = enabled))
            },
        )
        SettingSwitchCard(
            title = "Offline dictation only",
            body = "Dictation stays on this device. Explicit model downloads can still use HTTPS when you tap Download on the Models screen.",
            checked = appSettings.offlineOnly,
            onCheckedChange = { enabled ->
                onSettingsChange(appSettings.copy(offlineOnly = enabled))
            },
        )

        SettingsSectionCard(title = "Floating button color") {
            Text(
                text = "Choose a preset color for the QuietType floating button. Listening still uses red so recording is obvious.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverlayColorPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = appSettings.overlayColorPreset == preset,
                        onClick = { onSettingsChange(appSettings.copy(overlayColorPreset = preset)) },
                        label = { Text(preset.displayName) },
                    )
                }
            }
        }

        SettingsSectionCard(title = "Hidden fields and apps") {
            Text(
                text = "Use the × button on the floating control to hide QuietType for a detected app, screen, or field. Remove entries here to show it again.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (appSettings.hiddenTargets.isEmpty()) {
                Text(
                    text = "No hidden targets yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                appSettings.hiddenTargets.forEach { target ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = target.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Scope: ${target.scope.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                onSettingsChange(
                                    appSettings.copy(hiddenTargets = appSettings.hiddenTargets - target),
                                )
                            },
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }
        }


        SettingsSectionCard(title = "Floating button test") {
            Text(
                text = QuietTypeAccessibilityPresentation.statusText(isAccessibilityEnabled),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = onOpenAccessibilitySettings, modifier = Modifier.fillMaxWidth()) {
                Text(if (isAccessibilityEnabled) "Accessibility settings" else "Enable Accessibility service")
            }
            if (!dictationState.hasMicrophonePermission) {
                OutlinedButton(onClick = onRequestMicrophonePermission, modifier = Modifier.fillMaxWidth()) {
                    Text("Allow microphone")
                }
            }
            QuietTypeOverlayTestField(
                dictationState = dictationState,
                onStartRecordingShell = onStartRecordingShell,
                onStopRecordingShell = onStopRecordingShell,
            )
        }

    }
}

@Composable
private fun QuietTypeModelsScreen(
    appSettings: AppSettings,
    modelCatalogState: ModelCatalogState,
    modelDownloadStatus: String?,
    modelDownloadProgress: ModelDownloadProgress?,
    isModelDownloadActive: Boolean,
    onSelectModel: (String) -> Unit,
    onSelectLanguageProfile: (String) -> Unit,
    onDownloadModel: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
    onSelectCorrectionModel: (String) -> Unit,
    onDeleteCorrectionModel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var modelPendingDelete by remember { mutableStateOf<VoiceModel?>(null) }
    modelPendingDelete?.let { model ->
        AlertDialog(
            onDismissRequest = { modelPendingDelete = null },
            title = { Text("Delete downloaded model?") },
            text = { Text("This removes ${model.name} from private app storage. You can download it again later.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        modelPendingDelete = null
                        onDeleteModel(model.id)
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { modelPendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Local models",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Pick a speech profile, or choose Custom for the full list.",
            style = MaterialTheme.typography.bodyLarge,
        )
        SettingsSectionCard(title = "Language profile") {
            modelCatalogState.catalog.languageProfiles.forEach { profile ->
                val recommendedModel = profile.defaultModelId?.let { modelCatalogState.catalog.modelById(it) }
                val selected = profile.id == modelCatalogState.selectedLanguageProfile.id
                LanguageProfileRow(
                    profile = profile,
                    selected = selected,
                    recommendedModelName = recommendedModel?.name ?: "Manual selection",
                    installLabel = recommendedModel?.let { model ->
                        val downloaded = modelCatalogState.downloadedModelIds.contains(model.id)
                        val prepared = modelCatalogState.preparedModelIds.contains(model.id)
                        model.statusLabel(downloaded = downloaded, prepared = prepared, downloadable = model.isOfflineCapable)
                    } ?: "shows full list",
                    enabled = !isModelDownloadActive,
                    onSelect = { onSelectLanguageProfile(profile.id) },
                )
            }
        }
        modelDownloadStatus?.let { status ->
            StatusCard(
                title = "Model download status",
                body = status,
            )
        }
        modelDownloadProgress?.let { progress ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val fraction = progress.fraction
                if (fraction != null) {
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Text(
                    text = progress.percent?.let { "Download progress: $it%" } ?: "Download progress: receiving data…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (modelCatalogState.isCustomModelSelection) {
            Text(
                text = "Custom speech models",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                modelCatalogState.catalog.models.forEach { model ->
                    val selected = model.id == modelCatalogState.selectedModel.id
                    val downloaded = modelCatalogState.downloadedModelIds.contains(model.id)
                    val prepared = modelCatalogState.preparedModelIds.contains(model.id)
                    val downloadable = model.isOfflineCapable && model.runtime.requiredFiles.isNotEmpty()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = model.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = model.description,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "${model.engine} · ${model.language} · ~${model.sizeMegabytes} MB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (selected) StatusChip("Selected")
                                StatusChip(model.statusLabel(downloaded = downloaded, prepared = prepared, downloadable = downloadable))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { onSelectModel(model.id) },
                                    enabled = !selected && !isModelDownloadActive,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(if (selected) "Selected" else "Select")
                                }
                                if (downloaded) {
                                    OutlinedButton(
                                        onClick = { modelPendingDelete = model },
                                        enabled = !isModelDownloadActive,
                                        modifier = Modifier.weight(1f),
                                    ) { Text("Delete") }
                                } else {
                                    Button(
                                        onClick = { onDownloadModel(model.id) },
                                        enabled = !isModelDownloadActive && downloadable,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(
                                            when {
                                                isModelDownloadActive -> "Busy"
                                                downloadable -> "Download"
                                                else -> "Unavailable"
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "Correction models",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Selecting a correction model saves it immediately and downloads it if needed. Fix still falls back to built-in cleanup until local LLM runtime wiring lands.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CorrectionModelCatalog.default().models.forEach { model ->
                val selected = appSettings.selectedCorrectionModelId == model.id
                val downloaded = appSettings.downloadedCorrectionModelIds.contains(model.id)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = model.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = model.description,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "${model.engine} · ~${model.sizeMegabytes} MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (selected) StatusChip("Selected")
                            StatusChip(
                                when {
                                    model.isDeterministic -> "built in"
                                    downloaded -> "downloaded"
                                    model.isDownloadable -> "not downloaded"
                                    else -> "not available"
                                },
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Button(
                                onClick = { onSelectCorrectionModel(model.id) },
                                enabled = !isModelDownloadActive && (!selected || !downloaded || model.isDeterministic),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    when {
                                        model.isDeterministic && selected -> "Selected"
                                        model.isDeterministic -> "Use"
                                        selected && downloaded -> "Selected"
                                        model.isDownloadable -> "Use + download"
                                        else -> "Unavailable"
                                    },
                                )
                            }
                            if (!model.isDeterministic && downloaded) {
                                OutlinedButton(
                                    onClick = { onDeleteCorrectionModel(model.id) },
                                    enabled = !isModelDownloadActive,
                                    modifier = Modifier.weight(1f),
                                ) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageProfileRow(
    profile: LanguageProfile,
    selected: Boolean,
    recommendedModelName: String,
    installLabel: String,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && !selected) { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = profile.description,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "Recommended: $recommendedModelName",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = installLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                StatusChip("Active")
            } else {
                OutlinedButton(
                    onClick = onSelect,
                    enabled = enabled,
                ) { Text("Use") }
            }
        }
    }
}

@Composable
private fun StatusChip(label: String) {
    AssistChip(
        onClick = {},
        label = { Text(label) },
    )
}

private fun VoiceModel.statusLabel(downloaded: Boolean, prepared: Boolean, downloadable: Boolean): String = when {
    prepared -> "prepared for dictation"
    downloaded -> "downloaded archive · preparation pending"
    !downloadable -> "benchmark/reference only · not downloadable in app"
    else -> "not downloaded"
}

@Composable
private fun StatusCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}


@Composable
private fun SettingsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun QuietTypeOverlayTestField(
    dictationState: DictationSessionState,
    onStartRecordingShell: () -> Unit,
    onStopRecordingShell: () -> Unit,
) {
    var testText by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(
        value = testText,
        onValueChange = { testText = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Floating button test field") },
        placeholder = { Text("Tap here to show the Accessibility floating button") },
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onStartRecordingShell,
            enabled = dictationState.hasMicrophonePermission && !dictationState.isRecording,
            modifier = Modifier.weight(1f),
        ) {
            Text("Start test")
        }
        OutlinedButton(
            onClick = onStopRecordingShell,
            enabled = dictationState.isRecording,
            modifier = Modifier.weight(1f),
        ) {
            Text("Stop")
        }
    }
    Text(
        text = microphoneStatusText(dictationState),
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SettingSwitchCard(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onCheckedChange(!checked) },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

private fun yesNo(value: Boolean): String = if (value) "yes" else "no"

private fun microphoneStatusText(state: DictationSessionState): String = when {
    state.isRecording -> "Local dictation is active with a foreground microphone notification. Audio capture stays on-device."
    !state.hasMicrophonePermission && state.blockReason == DictationBlockReason.MicrophonePermissionMissing ->
        "Microphone permission is required before local dictation can start."
    !state.hasMicrophonePermission -> "Microphone permission has not been granted yet."
    else -> "Microphone permission granted. Dictation is idle until you hold or toggle the mic button."
}

@Composable
private fun QuietTypeHistoryScreen(
    historyEntries: List<DictationHistoryEntry>,
    historyEnabled: Boolean,
    onCopyHistoryEntry: (DictationHistoryEntry) -> Unit,
    onDeleteHistoryEntry: (String) -> Unit,
    onClearHistory: () -> Unit,
    onEnableHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Dictation history",
            style = MaterialTheme.typography.headlineMedium,
        )
        SettingsSectionCard(title = "Privacy-first history") {
            Text(
                text = if (historyEnabled) {
                    "History is on. QuietType saves successful final dictations locally on this device only. Raw audio is never stored."
                } else {
                    "History is off by default. Enable it only if you want local copies of successful final dictations for copying later."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!historyEnabled) {
                Button(onClick = onEnableHistory, modifier = Modifier.fillMaxWidth()) {
                    Text("Enable local history")
                }
            }
        }
        if (historyEntries.isEmpty()) {
            StatusCard(
                title = "No saved dictations",
                body = if (historyEnabled) {
                    "New successful final dictations will appear here."
                } else {
                    "Turn on local history to save future dictations."
                },
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(onClick = onClearHistory) { Text("Clear all") }
            }
            historyEntries.forEach { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = formatHistoryTimestamp(entry.createdAtEpochMillis),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = entry.text,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = { onCopyHistoryEntry(entry) },
                                modifier = Modifier.weight(1f),
                            ) { Text("Copy") }
                            OutlinedButton(
                                onClick = { onDeleteHistoryEntry(entry.id) },
                                modifier = Modifier.weight(1f),
                            ) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}

private fun formatHistoryTimestamp(epochMillis: Long): String = DateFormat.getDateTimeInstance(
    DateFormat.MEDIUM,
    DateFormat.SHORT,
).format(Date(epochMillis))

@Composable
private fun QuietTypeAboutScreen(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SettingsSectionCard(title = "🎙️ What QuietType does") {
            Text(
                text = "QuietType is a privacy-first Android dictation app by Kim Schulz. It stays out of the way until an editable field is focused, then offers a compact floating microphone control for on-device dictation.",
                style = MaterialTheme.typography.bodyMedium,
            )
            AboutBullet("Shows only when an editable field can receive text.")
            AboutBullet("Uses local ASR models for dictation instead of cloud transcription.")
            AboutBullet("Keeps model downloads explicit and user-started.")
        }

        SettingsSectionCard(title = "🛡️ Privacy posture") {
            AboutBullet("No telemetry or analytics are built into this app.")
            AboutBullet("Audio capture runs through Android foreground recording controls.")
            AboutBullet("Downloaded models live in private app storage and can be deleted from Models.")
        }

        SettingsSectionCard(title = "📚 Project resources") {
            AboutLink(
                icon = "⌂",
                label = "Project source",
                url = "https://github.com/kimusan/VoiceMe",
                onOpen = uriHandler::openUri,
            )
            AboutLink(
                icon = "🛡",
                label = "Privacy policy",
                url = "https://github.com/kimusan/VoiceMe/blob/main/PRIVACY.md",
                onOpen = uriHandler::openUri,
            )
            AboutLink(
                icon = "⚖",
                label = "License",
                url = "https://github.com/kimusan/VoiceMe/blob/main/LICENSE",
                onOpen = uriHandler::openUri,
            )
            AboutLink(
                icon = "📄",
                label = "Third-party notices",
                url = "https://github.com/kimusan/VoiceMe/blob/main/THIRD_PARTY_NOTICES.md",
                onOpen = uriHandler::openUri,
            )
        }

        SettingsSectionCard(title = "ℹ️ Version") {
            Text(
                text = "0.1.0-dev",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Current focus: reliable local ASR, clear onboarding, transparent model downloads, and a minimal keyboard-adjacent workflow.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun AboutBullet(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = "•", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AboutLink(icon: String, label: String, url: String, onOpen: (String) -> Unit) {
    TextButton(onClick = { onOpen(url) }, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = icon, style = MaterialTheme.typography.titleMedium)
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelLarge)
                Text(url, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuietTypeHomeScreenPreview() {
    QuietTypeTheme(dynamicColor = false) {
        QuietTypeHomeScreen(
            appSettings = AppSettings.default(),
            dictationState = DictationSessionState.idle(hasMicrophonePermission = false),
            modelCatalogState = ModelCatalogState.default(),
            modelDownloadStatus = null,
            modelDownloadProgress = null,
            isModelDownloadActive = false,
            isAccessibilityEnabled = false,
            historyEntries = emptyList(),
            onSettingsChange = {},
            onOpenAccessibilitySettings = {},
            onRequestMicrophonePermission = {},
            onStartRecordingShell = {},
            onStopRecordingShell = {},
            onSelectModel = {},
            onSelectLanguageProfile = {},
            onDownloadModel = {},
            onDeleteModel = {},
            onSelectCorrectionModel = {},
            onDeleteCorrectionModel = {},
            onCopyHistoryEntry = {},
            onDeleteHistoryEntry = {},
            onClearHistory = {},
        )
    }
}
