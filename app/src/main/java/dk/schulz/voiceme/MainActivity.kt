package dk.schulz.voiceme

import android.Manifest
import android.content.ComponentName
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
import dk.schulz.voiceme.accessibility.VoiceMeAccessibilityService
import dk.schulz.voiceme.dictation.DictationSessionReducer
import dk.schulz.voiceme.dictation.DictationSessionState
import dk.schulz.voiceme.dictation.VoiceMeRecordingService
import dk.schulz.voiceme.models.ModelArtifactInstallResult
import dk.schulz.voiceme.models.ModelArtifactInstaller
import dk.schulz.voiceme.models.ModelCatalogReducer
import dk.schulz.voiceme.models.ModelCatalogState
import dk.schulz.voiceme.models.ModelDownloadProgress
import dk.schulz.voiceme.models.ModelInstallState
import dk.schulz.voiceme.settings.AppSettings
import dk.schulz.voiceme.settings.AppSettingsStore
import dk.schulz.voiceme.ui.VoiceMeApp

class MainActivity : ComponentActivity() {
    private var appSettings by mutableStateOf(AppSettings.default())
    private var dictationState by mutableStateOf(
        DictationSessionState.idle(hasMicrophonePermission = false),
    )
    private var isAccessibilityEnabled by mutableStateOf(false)
    private var modelDownloadStatus by mutableStateOf<String?>(null)
    private var modelDownloadProgress by mutableStateOf<ModelDownloadProgress?>(null)
    private var activeModelDownloadId by mutableStateOf<String?>(null)

    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        dictationState = dictationState.copy(hasMicrophonePermission = granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsStore = AppSettingsStore(this)
        appSettings = settingsStore.load()
        refreshRuntimeStatus()

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
                modelDownloadProgress = modelDownloadProgress,
                isModelDownloadActive = activeModelDownloadId != null,
                isAccessibilityEnabled = isAccessibilityEnabled,
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
                    modelDownloadProgress = null
                    val updated = ModelCatalogReducer.deleteModel(modelCatalogState(), modelId)
                    saveSettings(
                        appSettings.copy(
                            downloadedModelIds = updated.downloadedModelIds,
                            preparedModelIds = updated.preparedModelIds,
                        ),
                    )
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshRuntimeStatus()
    }

    private fun startRecordingShell() {
        dictationState = DictationSessionReducer.startRecording(
            dictationState.copy(hasMicrophonePermission = hasMicrophonePermission()),
        )
        if (dictationState.isRecording) {
            runCatching {
                ContextCompat.startForegroundService(
                    this,
                    VoiceMeRecordingService.startIntent(this),
                )
            }.onFailure { error ->
                dictationState = DictationSessionReducer.stopRecording(dictationState)
                modelDownloadStatus = "VoiceMe could not start dictation: ${error.message ?: error::class.java.simpleName}."
            }
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

    private fun refreshRuntimeStatus() {
        val hasMic = hasMicrophonePermission()
        dictationState = dictationState.copy(hasMicrophonePermission = hasMic)
        isAccessibilityEnabled = isVoiceMeAccessibilityEnabled()
    }

    private fun isVoiceMeAccessibilityEnabled(): Boolean {
        val expected = ComponentName(this, VoiceMeAccessibilityService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return enabledServices.split(':').any { service ->
            service.equals(expected, ignoreCase = true)
        }
    }

    private fun modelCatalogState(): ModelCatalogState = ModelCatalogState(
        selectedModelId = appSettings.selectedModelId,
        downloadedModelIds = appSettings.downloadedModelIds,
        preparedModelIds = appSettings.preparedModelIds,
    )

    private fun startModelDownload(
        modelId: String,
        onSettingsReady: (AppSettings) -> Unit,
    ) {
        val model = modelCatalogState().catalog.modelById(modelId) ?: return
        if (activeModelDownloadId != null) {
            modelDownloadStatus = "A model download is already running. Wait for it to finish before starting another."
            return
        }
        if (!model.isOfflineCapable || model.runtime.requiredFiles.isEmpty()) {
            modelDownloadStatus = "${model.name} is a benchmark/reference entry and is not downloadable in the app yet. Choose a mobile-ready model instead."
            modelDownloadProgress = null
            return
        }
        activeModelDownloadId = model.id
        modelDownloadStatus = "Starting download for ${model.name}…"
        modelDownloadProgress = ModelDownloadProgress(
            modelId = model.id,
            bytesRead = 0L,
            totalBytes = model.sizeMegabytes.toLong() * 1024L * 1024L,
        )
        Thread {
            val result = runCatching {
                ModelArtifactInstaller(
                    modelRootDirectory = filesDir.resolve("models"),
                ).install(model) { progress ->
                    runOnUiThread {
                        modelDownloadProgress = progress
                        modelDownloadStatus = progress.label(model.name)
                    }
                }
            }.getOrElse { error ->
                runOnUiThread {
                    activeModelDownloadId = null
                    modelDownloadProgress = null
                    modelDownloadStatus = "Download failed for ${model.name}: ${error.message ?: error::class.java.simpleName}."
                }
                return@Thread
            }

            runOnUiThread {
                activeModelDownloadId = null
                modelDownloadProgress = null
                when (result) {
                    is ModelArtifactInstallResult.Installed -> {
                        val currentState = modelCatalogState()
                        val downloaded = ModelCatalogReducer.markDownloaded(currentState, modelId)
                        val updated = if (result.installState == ModelInstallState.PreparedForDictation) {
                            ModelCatalogReducer.markPrepared(downloaded, modelId)
                        } else {
                            downloaded
                        }
                        modelDownloadStatus = if (result.installState == ModelInstallState.PreparedForDictation) {
                            "Verified and prepared ${model.name}. VoiceMe can use it for offline dictation."
                        } else {
                            "Verified and stored ${model.name}. Runtime preparation is still required before dictation."
                        }
                        onSettingsReady(
                            appSettings.copy(
                                selectedModelId = appSettings.selectedModelId,
                                downloadedModelIds = updated.downloadedModelIds,
                                preparedModelIds = updated.preparedModelIds,
                            ),
                        )
                    }

                    is ModelArtifactInstallResult.ChecksumMismatch -> {
                        modelDownloadStatus = "Checksum mismatch for ${model.name}; deleted the downloaded file and did not mark it downloaded."
                    }

                    is ModelArtifactInstallResult.InstallFailed -> {
                        modelDownloadStatus = "Could not prepare ${model.name}: ${result.reason}."
                    }
                }
            }
        }.start()
    }
}
