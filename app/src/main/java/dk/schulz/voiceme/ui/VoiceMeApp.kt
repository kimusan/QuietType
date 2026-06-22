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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import dk.schulz.voiceme.onboarding.OnboardingFlow
import dk.schulz.voiceme.onboarding.OnboardingStep
import dk.schulz.voiceme.settings.AppSettings
import dk.schulz.voiceme.settings.DictationInteraction
import dk.schulz.voiceme.ui.theme.VoiceMeTheme

@Composable
fun VoiceMeApp(
    appSettings: AppSettings = AppSettings.default(),
    onSettingsChange: (AppSettings) -> Unit = {},
    onOpenAccessibilitySettings: () -> Unit = {},
) {
    VoiceMeTheme {
        VoiceMeHomeScreen(
            appSettings = appSettings,
            onSettingsChange = onSettingsChange,
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceMeHomeScreen(
    appSettings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
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
                )

                1 -> VoiceMeStatusScreen(
                    appSettings = appSettings,
                    onReviewSetup = {
                        currentStep = 0
                        selectedSection = 0
                    },
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                )

                else -> VoiceMeSettingsPreview(
                    appSettings = appSettings,
                    onSettingsChange = onSettingsChange,
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
        listOf("Setup", "Status", "Settings").forEachIndexed { index, label ->
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
                text = "No microphone or model download is requested by this preview. Accessibility settings can be opened from the status screen so you can see where VoiceMe will be enabled.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
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
    onReviewSetup: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
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

@Preview(showBackground = true)
@Composable
private fun VoiceMeHomeScreenPreview() {
    VoiceMeTheme(dynamicColor = false) {
        VoiceMeHomeScreen(
            appSettings = AppSettings.default(),
            onSettingsChange = {},
            onOpenAccessibilitySettings = {},
        )
    }
}
