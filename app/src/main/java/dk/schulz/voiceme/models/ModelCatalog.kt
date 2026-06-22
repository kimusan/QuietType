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
                    id = "sherpa-onnx-nemo-fast-conformer-ctc-multilingual-int8",
                    name = "Compact multilingual dictation",
                    description = "Default on-device multilingual model candidate for broad European-language testing. It is not streaming-ready until the runtime adapter is wired and benchmarked.",
                    engine = "sherpa-onnx / NeMo FastConformer CTC int8",
                    language = "Belarusian, Croatian, English, French, German, Italian, Polish, Russian, Spanish, Ukrainian",
                    sizeMegabytes = 98,
                    license = "Apache-2.0 runtime; model artifact license requires release NOTICE review",
                    artifact = ModelArtifact(
                        url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-nemo-fast-conformer-ctc-be-de-en-es-fr-hr-it-pl-ru-uk-20k-int8.tar.bz2",
                        sha256 = "2116eebbfc923ee3332a244e8c933ccc1b7e6783070f7bf842d0b5fc64f6ae33",
                        fileName = "sherpa-onnx-nemo-fast-conformer-ctc-be-de-en-es-fr-hr-it-pl-ru-uk-20k-int8.tar.bz2",
                        licenseUrl = "https://github.com/k2-fsa/sherpa-onnx/blob/master/LICENSE",
                    ),
                    isOfflineCapable = true,
                ),
                VoiceModel(
                    id = "sherpa-onnx-streaming-zipformer-en-int8",
                    name = "Compact streaming English",
                    description = "Small streaming English model kept as a low-latency fallback candidate for runtime benchmarking.",
                    engine = "sherpa-onnx",
                    language = "English",
                    sizeMegabytes = 122,
                    license = "Apache-2.0 runtime; model artifact license requires release NOTICE review",
                    artifact = ModelArtifact(
                        url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-en-20M-2023-02-17.tar.bz2",
                        sha256 = "9c559283e8498d3fe95913c79ca1cb454bb26281ac2b102b41306c7d752765d9",
                        fileName = "sherpa-onnx-streaming-zipformer-en-20M-2023-02-17.tar.bz2",
                        licenseUrl = "https://github.com/k2-fsa/sherpa-onnx/blob/master/LICENSE",
                    ),
                    isOfflineCapable = true,
                ),
                VoiceModel(
                    id = "parakeet-tdt-ctc-android-candidate",
                    name = "Parakeet-class high accuracy English",
                    description = "Future optional high-accuracy English pack after Android export and performance validation. The small Parakeet artifacts found so far are English-only, not the multilingual default.",
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
    DownloadedArchive,
    PreparedForDictation,
}

data class ModelCatalogState(
    val selectedModelId: String,
    val downloadedModelIds: Set<String>,
    val preparedModelIds: Set<String> = emptySet(),
    val catalog: ModelCatalog = ModelCatalog.default(),
) {
    val selectedModel: VoiceModel = catalog.modelById(selectedModelId) ?: catalog.recommended
    val selectedInstallState: ModelInstallState = when {
        preparedModelIds.contains(selectedModel.id) -> ModelInstallState.PreparedForDictation
        downloadedModelIds.contains(selectedModel.id) -> ModelInstallState.DownloadedArchive
        else -> ModelInstallState.NotDownloaded
    }
    val isReadyForDictation: Boolean = selectedInstallState == ModelInstallState.PreparedForDictation

    fun selectModel(modelId: String): ModelCatalogState = if (catalog.modelById(modelId) == null) {
        this
    } else {
        copy(selectedModelId = modelId)
    }

    companion object {
        fun default(): ModelCatalogState = ModelCatalogState(
            selectedModelId = ModelCatalog.default().recommended.id,
            downloadedModelIds = emptySet(),
            preparedModelIds = emptySet(),
        )
    }
}

object ModelCatalogReducer {
    fun markDownloaded(state: ModelCatalogState, modelId: String): ModelCatalogState = if (state.catalog.modelById(modelId) == null) {
        state
    } else {
        state.copy(downloadedModelIds = state.downloadedModelIds + modelId)
    }

    fun markPrepared(state: ModelCatalogState, modelId: String): ModelCatalogState = if (
        state.catalog.modelById(modelId) == null || !state.downloadedModelIds.contains(modelId)
    ) {
        state
    } else {
        state.copy(preparedModelIds = state.preparedModelIds + modelId)
    }

    fun deleteModel(state: ModelCatalogState, modelId: String): ModelCatalogState =
        state.copy(
            downloadedModelIds = state.downloadedModelIds - modelId,
            preparedModelIds = state.preparedModelIds - modelId,
        )
}
