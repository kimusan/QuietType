package dk.schulz.voiceme

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import dk.schulz.voiceme.dictation.DictationSessionReducer
import dk.schulz.voiceme.dictation.DictationSessionState
import dk.schulz.voiceme.dictation.VoiceMeRecordingService
import dk.schulz.voiceme.models.ModelArtifactInstallResult
import dk.schulz.voiceme.models.ModelArtifactInstaller
import dk.schulz.voiceme.models.ModelCatalogReducer
import dk.schulz.voiceme.models.ModelCatalogState
import dk.schulz.voiceme.settings.AppSettings
import dk.schulz.voiceme.settings.AppSettingsStore
import dk.schulz.voiceme.ui.VoiceMeApp

class MainActivity : ComponentActivity() {
    private var appSettings by mutableStateOf(AppSettings.default())
    private var dictationState by mutableStateOf(
        DictationSessionState.idle(hasMicrophonePermission = false),
    )
    private var modelDownloadStatus by mutableStateOf<String?>(null)

    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        dictationState = dictationState.copy(hasMicrophonePermission = granted)
        if (granted) {
            startRecordingShell()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsStore = AppSettingsStore(this)
        appSettings = settingsStore.load()
        dictationState = DictationSessionState.idle(hasMicrophonePermission = hasMicrophonePermission())

        fun saveSettings(settings: AppSettings) {
            appSettings = settings
            settingsStore.save(settings)
        }

        setContent {
            VoiceMeApp(
                appSettings = appSettings,
                dictationState = dictationState,
                modelCatalogState = modelCatalogState(),
                modelDownloadStatus = modelDownloadStatus,
                onSettingsChange = ::saveSettings,
                onOpenAccessibilitySettings = {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                onRequestMicrophonePermission = {
                    microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onStartRecordingShell = {
                    if (hasMicrophonePermission()) {
                        startRecordingShell()
                    } else {
                        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopRecordingShell = ::stopRecordingShell,
                onSelectModel = { modelId ->
                    saveSettings(appSettings.copy(selectedModelId = modelId))
                },
                onDownloadModel = { modelId ->
                    startModelDownload(
                        modelId = modelId,
                        onSettingsReady = ::saveSettings,
                    )
                },
                onDeleteModel = { modelId ->
                    modelCatalogState().catalog.modelById(modelId)?.let { model ->
                        ModelArtifactInstaller(
                            modelRootDirectory = filesDir.resolve("models"),
                        ).delete(model)
                    }
                    modelDownloadStatus = "Deleted local files for $modelId."
                    val updated = ModelCatalogReducer.deleteModel(modelCatalogState(), modelId)
                    saveSettings(appSettings.copy(downloadedModelIds = updated.downloadedModelIds))
                },
            )
        }
    }

    private fun startRecordingShell() {
        dictationState = DictationSessionReducer.startRecording(
            dictationState.copy(hasMicrophonePermission = hasMicrophonePermission()),
        )
        if (dictationState.isRecording) {
            ContextCompat.startForegroundService(
                this,
                VoiceMeRecordingService.startIntent(this),
            )
        }
    }

    private fun stopRecordingShell() {
        stopService(VoiceMeRecordingService.stopIntent(this))
        dictationState = DictationSessionReducer.stopRecording(dictationState)
    }

    private fun hasMicrophonePermission(): Boolean = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    private fun modelCatalogState(): ModelCatalogState = ModelCatalogState(
        selectedModelId = appSettings.selectedModelId,
        downloadedModelIds = appSettings.downloadedModelIds,
    )

    private fun startModelDownload(
        modelId: String,
        onSettingsReady: (AppSettings) -> Unit,
    ) {
        val model = modelCatalogState().catalog.modelById(modelId) ?: return
        modelDownloadStatus = "Downloading ${model.name}…"
        Thread {
            val result = runCatching {
                ModelArtifactInstaller(
                    modelRootDirectory = filesDir.resolve("models"),
                ).install(model)
            }.getOrElse { error ->
                runOnUiThread {
                    modelDownloadStatus = "Download failed for ${model.name}: ${error.message ?: error::class.java.simpleName}."
                }
                return@Thread
            }

            runOnUiThread {
                when (result) {
                    is ModelArtifactInstallResult.Installed -> {
                        val currentState = modelCatalogState()
                        val updated = ModelCatalogReducer.markDownloaded(currentState, modelId)
                        modelDownloadStatus = "Verified and stored ${model.name}."
                        onSettingsReady(
                            appSettings.copy(
                                selectedModelId = appSettings.selectedModelId,
                                downloadedModelIds = updated.downloadedModelIds,
                            ),
                        )
                    }

                    is ModelArtifactInstallResult.ChecksumMismatch -> {
                        modelDownloadStatus = "Checksum mismatch for ${model.name}; deleted the downloaded file and did not mark it ready."
                    }
                }
            }
        }.start()
    }
}
