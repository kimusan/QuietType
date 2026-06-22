package dk.schulz.voiceme.models

data class VoiceModel(
    val id: String,
    val name: String,
    val description: String,
    val engine: String,
    val language: String,
    val sizeMegabytes: Int,
    val license: String,
    val artifact: ModelArtifact,
    val isOfflineCapable: Boolean,
)

data class ModelCatalog(
    val models: List<VoiceModel>,
) {
    val recommended: VoiceModel = models.first()

    fun modelById(modelId: String): VoiceModel? = models.firstOrNull { it.id == modelId }

    companion object {
        fun default(): ModelCatalog = ModelCatalog(
            models = listOf(
                VoiceModel(
                    id = "sherpa-onnx-streaming-zipformer-en-int8",
                    name = "Compact streaming English",
                    description = "Small on-device streaming model candidate for early latency and battery testing.",
                    engine = "sherpa-onnx",
                    language = "English",
                    sizeMegabytes = 72,
                    license = "Model license to verify before release",
                    artifact = ModelArtifact(
                        url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-en-2023-06-26-mobile.tar.bz2",
                        sha256 = "0000000000000000000000000000000000000000000000000000000000000000",
                        fileName = "sherpa-onnx-streaming-zipformer-en-2023-06-26-mobile.tar.bz2",
                        licenseUrl = "https://github.com/k2-fsa/sherpa-onnx/blob/master/LICENSE",
                    ),
                    isOfflineCapable = true,
                ),
                VoiceModel(
                    id = "parakeet-tdt-ctc-android-candidate",
                    name = "Parakeet-class high accuracy",
                    description = "Future optional high-accuracy pack after Android export and performance validation.",
                    engine = "ONNX Runtime / sherpa-onnx candidate",
                    language = "English",
                    sizeMegabytes = 620,
                    license = "Model license to verify before release",
                    artifact = ModelArtifact(
                        url = "https://example.invalid/voiceme/parakeet-tdt-ctc-android-candidate.zip",
                        sha256 = "0000000000000000000000000000000000000000000000000000000000000000",
                        fileName = "parakeet-tdt-ctc-android-candidate.zip",
                        licenseUrl = "https://www.nvidia.com/en-us/agreements/",
                    ),
                    isOfflineCapable = true,
                ),
            ),
        )
    }
}

enum class ModelInstallState {
    NotDownloaded,
    Downloaded,
}

data class ModelCatalogState(
    val selectedModelId: String,
    val downloadedModelIds: Set<String>,
    val catalog: ModelCatalog = ModelCatalog.default(),
) {
    val selectedModel: VoiceModel = catalog.modelById(selectedModelId) ?: catalog.recommended
    val selectedInstallState: ModelInstallState = if (downloadedModelIds.contains(selectedModel.id)) {
        ModelInstallState.Downloaded
    } else {
        ModelInstallState.NotDownloaded
    }
    val isReadyForDictation: Boolean = selectedInstallState == ModelInstallState.Downloaded

    fun selectModel(modelId: String): ModelCatalogState = if (catalog.modelById(modelId) == null) {
        this
    } else {
        copy(selectedModelId = modelId)
    }

    companion object {
        fun default(): ModelCatalogState = ModelCatalogState(
            selectedModelId = ModelCatalog.default().recommended.id,
            downloadedModelIds = emptySet(),
        )
    }
}

object ModelCatalogReducer {
    fun markDownloaded(state: ModelCatalogState, modelId: String): ModelCatalogState = if (state.catalog.modelById(modelId) == null) {
        state
    } else {
        state.copy(downloadedModelIds = state.downloadedModelIds + modelId)
    }

    fun deleteModel(state: ModelCatalogState, modelId: String): ModelCatalogState =
        state.copy(downloadedModelIds = state.downloadedModelIds - modelId)
}
