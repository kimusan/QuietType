package dk.schulz.quiettype

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
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
import android.widget.Toast
import androidx.core.content.ContextCompat
import dk.schulz.quiettype.accessibility.QuietTypeAccessibilityService
import dk.schulz.quiettype.dictation.DictationSessionReducer
import dk.schulz.quiettype.dictation.DictationSessionState
import dk.schulz.quiettype.dictation.QuietTypeRecordingService
import dk.schulz.quiettype.history.DictationHistoryEntry
import dk.schulz.quiettype.history.DictationHistoryStore
import dk.schulz.quiettype.models.ModelArtifactInstallResult
import dk.schulz.quiettype.models.ModelArtifactInstaller
import dk.schulz.quiettype.models.ModelCatalogReducer
import dk.schulz.quiettype.models.ModelCatalogState
import dk.schulz.quiettype.models.ModelDownloadDecision
import dk.schulz.quiettype.models.ModelDownloadPolicy
import dk.schulz.quiettype.models.ModelDownloadProgress
import dk.schulz.quiettype.models.ModelInstallState
import dk.schulz.quiettype.settings.AppSettings
import dk.schulz.quiettype.settings.AppSettingsStore
import dk.schulz.quiettype.ui.QuietTypeApp

class MainActivity : ComponentActivity() {
    private var appSettings by mutableStateOf(AppSettings.default())
    private var dictationState by mutableStateOf(
        DictationSessionState.idle(hasMicrophonePermission = false),
    )
    private var isAccessibilityEnabled by mutableStateOf(false)
    private var modelDownloadStatus by mutableStateOf<String?>(null)
    private var modelDownloadProgress by mutableStateOf<ModelDownloadProgress?>(null)
    private var activeModelDownloadId by mutableStateOf<String?>(null)
    private var historyEntries by mutableStateOf<List<DictationHistoryEntry>>(emptyList())

    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        dictationState = dictationState.copy(hasMicrophonePermission = granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsStore = AppSettingsStore(this)
        val historyStore = DictationHistoryStore(this)
        appSettings = settingsStore.load()
        historyEntries = historyStore.load()
        refreshRuntimeStatus()

        fun saveSettings(settings: AppSettings) {
            appSettings = settings
            settingsStore.save(settings)
        }

        setContent {
            QuietTypeApp(
                appSettings = appSettings,
                dictationState = dictationState,
                modelCatalogState = modelCatalogState(),
                modelDownloadStatus = modelDownloadStatus,
                modelDownloadProgress = modelDownloadProgress,
                isModelDownloadActive = activeModelDownloadId != null,
                isAccessibilityEnabled = isAccessibilityEnabled,
                historyEntries = historyEntries,
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
                onSelectLanguageProfile = { profileId ->
                    modelCatalogState().catalog.profileById(profileId)?.let { profile ->
                        saveSettings(
                            appSettings.copy(
                                selectedLanguageProfileId = profile.id,
                                selectedModelId = profile.defaultModelId,
                            ),
                        )
                    }
                },
                onDownloadModel = { modelId ->
                    startModelDownload(
                        modelId = modelId,
                        onSettingsReady = ::saveSettings,
                    )
                },
                onCopyHistoryEntry = { entry ->
                    copyHistoryEntry(entry)
                },
                onDeleteHistoryEntry = { entryId ->
                    historyStore.delete(entryId)
                    historyEntries = historyStore.load()
                },
                onClearHistory = {
                    historyStore.clear()
                    historyEntries = emptyList()
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
        historyEntries = DictationHistoryStore(this).load()
    }

    private fun startRecordingShell() {
        dictationState = DictationSessionReducer.startRecording(
            dictationState.copy(hasMicrophonePermission = hasMicrophonePermission()),
        )
        if (dictationState.isRecording) {
            runCatching {
                ContextCompat.startForegroundService(
                    this,
                    QuietTypeRecordingService.startIntent(this),
                )
            }.onFailure { error ->
                dictationState = DictationSessionReducer.stopRecording(dictationState)
                modelDownloadStatus = "QuietType could not start dictation: ${error.message ?: error::class.java.simpleName}."
            }
        }
    }

    private fun stopRecordingShell() {
        stopService(QuietTypeRecordingService.stopIntent(this))
        dictationState = DictationSessionReducer.stopRecording(dictationState)
    }

    private fun copyHistoryEntry(entry: DictationHistoryEntry) {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("QuietType dictation", entry.text))
        Toast.makeText(this, "Copied dictation history entry.", Toast.LENGTH_SHORT).show()
    }

    private fun hasMicrophonePermission(): Boolean = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    private fun refreshRuntimeStatus() {
        val hasMic = hasMicrophonePermission()
        dictationState = dictationState.copy(hasMicrophonePermission = hasMic)
        isAccessibilityEnabled = isQuietTypeAccessibilityEnabled()
    }

    private fun isQuietTypeAccessibilityEnabled(): Boolean {
        val expected = ComponentName(this, QuietTypeAccessibilityService::class.java).flattenToString()
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
        selectedLanguageProfileId = appSettings.selectedLanguageProfileId,
        downloadedModelIds = appSettings.downloadedModelIds,
        preparedModelIds = appSettings.preparedModelIds,
    )

    private fun startModelDownload(
        modelId: String,
        onSettingsReady: (AppSettings) -> Unit,
    ) {
        val model = modelCatalogState().catalog.modelById(modelId) ?: return
        when (
            val decision = ModelDownloadPolicy.canDownload(
                model = model,
                offlineOnly = appSettings.offlineOnly,
                userInitiated = true,
                activeDownloadId = activeModelDownloadId,
            )
        ) {
            ModelDownloadDecision.Allowed -> Unit
            is ModelDownloadDecision.Blocked -> {
                modelDownloadStatus = decision.reason
                modelDownloadProgress = null
                return
            }
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
                            "Verified and prepared ${model.name}. QuietType can use it for offline dictation."
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
