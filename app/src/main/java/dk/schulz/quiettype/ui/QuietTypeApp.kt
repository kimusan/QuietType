package dk.schulz.quiettype.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dk.schulz.quiettype.R
import dk.schulz.quiettype.dictation.DictationBlockReason
import dk.schulz.quiettype.accessibility.QuietTypeAccessibilityPresentation
import dk.schulz.quiettype.dictation.DictationSessionState
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

@Composable
fun QuietTypeApp(
    appSettings: AppSettings = AppSettings.default(),
    dictationState: DictationSessionState = DictationSessionState.idle(hasMicrophonePermission = false),
    modelCatalogState: ModelCatalogState = ModelCatalogState.default(),
    modelDownloadStatus: String? = null,
    modelDownloadProgress: ModelDownloadProgress? = null,
    isModelDownloadActive: Boolean = false,
    isAccessibilityEnabled: Boolean = false,
    onSettingsChange: (AppSettings) -> Unit = {},
    onOpenAccessibilitySettings: () -> Unit = {},
    onRequestMicrophonePermission: () -> Unit = {},
    onStartRecordingShell: () -> Unit = {},
    onStopRecordingShell: () -> Unit = {},
    onSelectModel: (String) -> Unit = {},
    onDownloadModel: (String) -> Unit = {},
    onDeleteModel: (String) -> Unit = {},
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
            onSettingsChange = onSettingsChange,
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            onRequestMicrophonePermission = onRequestMicrophonePermission,
            onStartRecordingShell = onStartRecordingShell,
            onStopRecordingShell = onStopRecordingShell,
            onSelectModel = onSelectModel,
            onDownloadModel = onDownloadModel,
            onDeleteModel = onDeleteModel,
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
    onSettingsChange: (AppSettings) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onRequestMicrophonePermission: () -> Unit,
    onStartRecordingShell: () -> Unit,
    onStopRecordingShell: () -> Unit,
    onSelectModel: (String) -> Unit,
    onDownloadModel: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionChips(
                selectedSection = selectedSection,
                onSelectedSectionChange = { selectedSection = it },
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
            if (selectedSection == 2) {
                QuietTypeModelsScreen(
                    modelCatalogState = modelCatalogState,
                    modelDownloadStatus = modelDownloadStatus,
                    modelDownloadProgress = modelDownloadProgress,
                    isModelDownloadActive = isModelDownloadActive,
                    onSelectModel = onSelectModel,
                    onDownloadModel = onDownloadModel,
                    onDeleteModel = onDeleteModel,
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

                        else -> QuietTypeAboutScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionChips(
    selectedSection: Int,
    onSelectedSectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("Setup", "Settings", "Models", "About").forEachIndexed { index, label ->
            FilterChip(
                selected = selectedSection == index,
                onClick = { onSelectedSectionChange(index) },
                label = { Text(label) },
            )
        }
    }
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
                modifier = Modifier.weight(1f),
            ) {
                Text(step.primaryAction)
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
        SettingSwitchCard(
            title = "Offline-only mode",
            body = "Keep QuietType from using network features except explicit future model-download flows.",
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
    modelCatalogState: ModelCatalogState,
    modelDownloadStatus: String?,
    modelDownloadProgress: ModelDownloadProgress?,
    isModelDownloadActive: Boolean,
    onSelectModel: (String) -> Unit,
    onDownloadModel: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Local models",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "This screen starts explicit HTTPS model downloads, verifies SHA-256 before storing downloaded archives, and deletes private model files on request. Downloaded archives are not dictation-ready until runtime preparation succeeds.",
            style = MaterialTheme.typography.bodyLarge,
        )
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            modelCatalogState.catalog.models.forEach { model ->
                val selected = model.id == modelCatalogState.selectedModel.id
                val downloaded = modelCatalogState.downloadedModelIds.contains(model.id)
                val prepared = modelCatalogState.preparedModelIds.contains(model.id)
                val downloadable = model.isOfflineCapable && model.runtime.requiredFiles.isNotEmpty()
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = model.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = model.description,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "${model.engine} · ${model.language} · ~${model.sizeMegabytes} MB · ${model.license}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "Status: ${model.statusLabel(downloaded = downloaded, prepared = prepared, downloadable = downloadable)}${if (selected) " · selected" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
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
                            Button(
                                onClick = {
                                    if (downloaded) onDeleteModel(model.id) else onDownloadModel(model.id)
                                },
                                enabled = !isModelDownloadActive && (downloaded || downloadable),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    when {
                                        isModelDownloadActive -> "Busy"
                                        downloaded -> "Delete"
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
private fun SettingSwitchCard(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
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
private fun QuietTypeAboutScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SettingsSectionCard(title = "About QuietType") {
            Text(
                text = "QuietType is a privacy-first Android dictation app by Kim Schulz. It stays out of the way until an editable field is focused, then offers a small floating microphone button for on-device dictation.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Project: https://github.com/kimusan/quiettype",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Current focus: reliable local ASR, clear onboarding, and transparent model downloads without telemetry or cloud transcription.",
                style = MaterialTheme.typography.bodyMedium,
            )
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
            onSettingsChange = {},
            onOpenAccessibilitySettings = {},
            onRequestMicrophonePermission = {},
            onStartRecordingShell = {},
            onStopRecordingShell = {},
            onSelectModel = {},
            onDownloadModel = {},
            onDeleteModel = {},
        )
    }
}
