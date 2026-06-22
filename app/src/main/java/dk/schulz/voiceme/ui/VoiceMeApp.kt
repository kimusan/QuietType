package dk.schulz.voiceme.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import dk.schulz.voiceme.R
import dk.schulz.voiceme.dictation.DictationBlockReason
import dk.schulz.voiceme.dictation.DictationSessionState
import dk.schulz.voiceme.models.ModelCatalogState
import dk.schulz.voiceme.models.VoiceModel
import dk.schulz.voiceme.onboarding.OnboardingAction
import dk.schulz.voiceme.onboarding.OnboardingFlow
import dk.schulz.voiceme.onboarding.OnboardingStep
import dk.schulz.voiceme.settings.AppSettings
import dk.schulz.voiceme.settings.DictationInteraction
import dk.schulz.voiceme.ui.theme.VoiceMeTheme

@Composable
fun VoiceMeApp(
    appSettings: AppSettings = AppSettings.default(),
    dictationState: DictationSessionState = DictationSessionState.idle(hasMicrophonePermission = false),
    modelCatalogState: ModelCatalogState = ModelCatalogState.default(),
    modelDownloadStatus: String? = null,
    onSettingsChange: (AppSettings) -> Unit = {},
    onOpenAccessibilitySettings: () -> Unit = {},
    onRequestMicrophonePermission: () -> Unit = {},
    onStartRecordingShell: () -> Unit = {},
    onStopRecordingShell: () -> Unit = {},
    onSelectModel: (String) -> Unit = {},
    onDownloadModel: (String) -> Unit = {},
    onDeleteModel: (String) -> Unit = {},
) {
    VoiceMeTheme {
        VoiceMeHomeScreen(
            appSettings = appSettings,
            dictationState = dictationState,
            modelCatalogState = modelCatalogState,
            modelDownloadStatus = modelDownloadStatus,
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
fun VoiceMeHomeScreen(
    appSettings: AppSettings,
    dictationState: DictationSessionState,
    modelCatalogState: ModelCatalogState,
    modelDownloadStatus: String?,
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SectionChips(
                selectedSection = selectedSection,
                onSelectedSectionChange = { selectedSection = it },
            )
            when (selectedSection) {
                0 -> VoiceMeOnboardingScreen(
                    step = onboardingFlow.currentStep(currentStep),
                    currentStep = currentStep,
                    totalSteps = onboardingFlow.totalSteps,
                    onBack = {
                        currentStep = onboardingFlow.previousIndex(currentStep)
                    },
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
                            OnboardingAction.OpenModels -> selectedSection = 3
                            OnboardingAction.None -> Unit
                        }
                    },
                )

                1 -> VoiceMeStatusScreen(
                    appSettings = appSettings,
                    dictationState = dictationState,
                    modelCatalogState = modelCatalogState,
                    onReviewSetup = {
                        currentStep = 0
                        selectedSection = 0
                    },
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    onRequestMicrophonePermission = onRequestMicrophonePermission,
                    onStartRecordingShell = onStartRecordingShell,
                    onStopRecordingShell = onStopRecordingShell,
                )

                2 -> VoiceMeSettingsPreview(
                    appSettings = appSettings,
                    onSettingsChange = onSettingsChange,
                )

                else -> VoiceMeModelsScreen(
                    modelCatalogState = modelCatalogState,
                    modelDownloadStatus = modelDownloadStatus,
                    onSelectModel = onSelectModel,
                    onDownloadModel = onDownloadModel,
                    onDeleteModel = onDeleteModel,
                )
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
        listOf("Setup", "Status", "Settings", "Models").forEachIndexed { index, label ->
            FilterChip(
                selected = selectedSection == index,
                onClick = { onSelectedSectionChange(index) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun VoiceMeOnboardingScreen(
    step: OnboardingStep,
    currentStep: Int,
    totalSteps: Int,
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
                Text(step.actionLabel)
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
private fun VoiceMeStatusScreen(
    appSettings: AppSettings,
    dictationState: DictationSessionState,
    modelCatalogState: ModelCatalogState,
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
                "VoiceMe setup preview complete"
            } else {
                "VoiceMe setup preview"
            },
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "The app shell is installed and interactive. When the Accessibility service is enabled, VoiceMe now detects focused editable fields and shows a draggable microphone preview overlay. Dictation, recording, and model downloads remain future milestones.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        StatusCard(
            title = "Privacy default",
            body = "Offline-only: ${yesNo(appSettings.offlineOnly)}. Transcript history: ${if (appSettings.transcriptHistoryEnabled) "on" else "off"}.",
        )
        StatusCard(
            title = "Input strategy",
            body = "VoiceMe is registered as an accessibility service candidate. Android grants window-content capability so the service can identify editable focused fields and place a draggable preview mic. It does not inspect field text, record audio, or insert dictated text yet.",
        )
        StatusCard(
            title = "Current dictation interaction",
            body = when (appSettings.dictationInteraction) {
                DictationInteraction.HoldToTalk -> "Hold-to-talk: safest default for accidental dictation."
                DictationInteraction.TapToToggle -> "Tap-to-toggle: easier for longer dictation once recording exists."
            },
        )
        StatusCard(
            title = "Microphone shell",
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
                Text(if (dictationState.hasMicrophonePermission) "Start mic shell" else "Allow microphone")
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
                title = "ASR stub output",
                body = "Latest final segment: ${dictationState.latestFinalTranscript}. The Accessibility overlay uses this same stub text for insertion testing.",
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
    var previewText by rememberSaveable { mutableStateOf("Tap here after enabling Accessibility to make the real VoiceMe overlay appear.") }

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
                text = "Enable VoiceMe in Android Accessibility settings, then tap this editable field. The actual accessibility overlay should appear near the lower-right edge and can be dragged.",
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
        "Android will open system settings. Choose VoiceMe, enable the service, then return here and use the Status screen test field to see the real floating button."
    OnboardingAction.RequestMicrophonePermission ->
        "Android will show the microphone permission dialog now. If accepted, VoiceMe starts its local foreground recording shell so the permission path is exercised."
    OnboardingAction.OpenModels ->
        "The Models screen shows the default multilingual model, checksum, size, and download action. Downloaded archives are still gated until runtime preparation is implemented."
    OnboardingAction.None ->
        "No permission is requested on this step. Continue when you are ready."
}

@Composable
private fun VoiceMeSettingsPreview(
    appSettings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
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
            title = "Offline-only mode",
            body = "Keep VoiceMe from using network features except explicit future model-download flows.",
            checked = appSettings.offlineOnly,
            onCheckedChange = { enabled ->
                onSettingsChange(appSettings.copy(offlineOnly = enabled))
            },
        )
        SettingSwitchCard(
            title = "Hide in sensitive fields",
            body = "Planned behavior: pause or hide dictation for password and other sensitive input fields.",
            checked = appSettings.hideInSensitiveFields,
            onCheckedChange = { enabled ->
                onSettingsChange(appSettings.copy(hideInSensitiveFields = enabled))
            },
        )
        StatusCard(
            title = "Transcript history locked off",
            body = "Transcript history remains disabled in this build. A future release may expose it only as an explicit opt-in setting.",
        )
    }
}

@Composable
private fun VoiceMeModelsScreen(
    modelCatalogState: ModelCatalogState,
    modelDownloadStatus: String?,
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
        modelCatalogState.catalog.models.forEach { model ->
            val selected = model.id == modelCatalogState.selectedModel.id
            val downloaded = modelCatalogState.downloadedModelIds.contains(model.id)
            val prepared = modelCatalogState.preparedModelIds.contains(model.id)
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
                        text = "Status: ${model.statusLabel(downloaded = downloaded, prepared = prepared)}${if (selected) " · selected" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { onSelectModel(model.id) },
                            enabled = !selected,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (selected) "Selected" else "Select")
                        }
                        Button(
                            onClick = {
                                if (downloaded) onDeleteModel(model.id) else onDownloadModel(model.id)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (downloaded) "Delete" else "Download")
                        }
                    }
                }
            }
        }
    }
}

private fun VoiceModel.statusLabel(downloaded: Boolean, prepared: Boolean): String = when {
    prepared -> "prepared for dictation"
    downloaded -> "downloaded archive · preparation pending"
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
    state.isRecording -> "Recording shell active with a foreground microphone notification. Audio capture is local and ASR is still stubbed."
    !state.hasMicrophonePermission && state.blockReason == DictationBlockReason.MicrophonePermissionMissing ->
        "Microphone permission is required before the foreground recording shell can start."
    !state.hasMicrophonePermission -> "Microphone permission has not been granted yet."
    else -> "Microphone permission granted. Recording shell is idle."
}

@Preview(showBackground = true)
@Composable
private fun VoiceMeHomeScreenPreview() {
    VoiceMeTheme(dynamicColor = false) {
        VoiceMeHomeScreen(
            appSettings = AppSettings.default(),
            dictationState = DictationSessionState.idle(hasMicrophonePermission = false),
            modelCatalogState = ModelCatalogState.default(),
            modelDownloadStatus = null,
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
