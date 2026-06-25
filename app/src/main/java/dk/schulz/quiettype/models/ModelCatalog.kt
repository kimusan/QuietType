package dk.schulz.quiettype.models

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
    val runtime: ModelRuntime,
)

data class ModelRuntime(
    val kind: ModelRuntimeKind,
    val requiredFiles: List<String>,
)

enum class ModelRuntimeKind {
    SherpaOnnxOfflineTransducer,
    SherpaOnnxOfflineCtc,
    SherpaOnnxStreamingTransducer,
    UnsupportedMobileBenchmark,
}

data class ModelCatalog(
    val models: List<VoiceModel>,
) {
    val recommended: VoiceModel = models.first()

    fun modelById(modelId: String): VoiceModel? = models.firstOrNull { it.id == modelId }

    companion object {
        fun default(): ModelCatalog = ModelCatalog(
            models = listOf(
                VoiceModel(
                    id = "sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8",
                    name = "Parakeet TDT v3 multilingual int8",
                    description = "Recommended offline multilingual Parakeet model for Android testing. Large download, but supports Danish and other European languages through sherpa-onnx.",
                    engine = "sherpa-onnx / NeMo Parakeet TDT 0.6B v3 int8",
                    language = "Bulgarian, Croatian, Czech, Danish, Dutch, English, Estonian, Finnish, French, German, Greek, Hungarian, Italian, Latvian, Lithuanian, Maltese, Polish, Portuguese, Romanian, Russian, Slovak, Slovenian, Spanish, Swedish, Ukrainian",
                    sizeMegabytes = 465,
                    license = "CC-BY-4.0 model; Apache-2.0 sherpa-onnx runtime",
                    artifact = ModelArtifact(
                        url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8.tar.bz2",
                        sha256 = "5793d0fd397c5778d2cf2126994d58e9d56b1be7c04d13c7a15bb1b4eafb16bf",
                        fileName = "sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8.tar.bz2",
                        licenseUrl = "https://huggingface.co/nvidia/parakeet-tdt-0.6b-v3",
                    ),
                    isOfflineCapable = true,
                    runtime = ModelRuntime(
                        kind = ModelRuntimeKind.SherpaOnnxOfflineTransducer,
                        requiredFiles = listOf("encoder.int8.onnx", "decoder.int8.onnx", "joiner.int8.onnx", "tokens.txt"),
                    ),
                ),
                VoiceModel(
                    id = "sherpa-onnx-nemo-parakeet-tdt-0.6b-v2-int8",
                    name = "Parakeet TDT v2 English int8",
                    description = "English-only Parakeet fallback. Similar size to v3 but useful for benchmarking against the multilingual model.",
                    engine = "sherpa-onnx / NeMo Parakeet TDT 0.6B v2 int8",
                    language = "English",
                    sizeMegabytes = 460,
                    license = "CC-BY-4.0 model; Apache-2.0 sherpa-onnx runtime",
                    artifact = ModelArtifact(
                        url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-nemo-parakeet-tdt-0.6b-v2-int8.tar.bz2",
                        sha256 = "157c157bc51155e03e37d2466522a3a737dd9c72bb25f36eb18912964161e1ad",
                        fileName = "sherpa-onnx-nemo-parakeet-tdt-0.6b-v2-int8.tar.bz2",
                        licenseUrl = "https://huggingface.co/nvidia/parakeet-tdt-0.6b-v2",
                    ),
                    isOfflineCapable = true,
                    runtime = ModelRuntime(
                        kind = ModelRuntimeKind.SherpaOnnxOfflineTransducer,
                        requiredFiles = listOf("encoder.int8.onnx", "decoder.int8.onnx", "joiner.int8.onnx", "tokens.txt"),
                    ),
                ),
                VoiceModel(
                    id = "sherpa-onnx-nemo-fast-conformer-ctc-multilingual-int8",
                    name = "Compact multilingual fallback",
                    description = "Smaller multilingual sherpa-onnx fallback for runtime smoke tests if Parakeet is too heavy on the device.",
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
                    runtime = ModelRuntime(
                        kind = ModelRuntimeKind.SherpaOnnxOfflineCtc,
                        requiredFiles = listOf("model.int8.onnx", "tokens.txt"),
                    ),
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
                    runtime = ModelRuntime(
                        kind = ModelRuntimeKind.SherpaOnnxStreamingTransducer,
                        requiredFiles = listOf("encoder-epoch-99-avg-1.onnx", "decoder-epoch-99-avg-1.onnx", "joiner-epoch-99-avg-1.onnx", "tokens.txt"),
                    ),
                ),
                VoiceModel(
                    id = "sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-fp32",
                    name = "Parakeet TDT v3 fp32 benchmark",
                    description = "High-accuracy fp32 Parakeet artifact is not recommended for mobile runtime: it is about 2.4 GiB unpacked and likely too memory-heavy for normal Android dictation.",
                    engine = "sherpa-onnx / NeMo Parakeet TDT 0.6B v3 fp32",
                    language = "Same 25-language set as v3 int8",
                    sizeMegabytes = 2430,
                    license = "CC-BY-4.0 model; Apache-2.0 sherpa-onnx runtime",
                    artifact = ModelArtifact(
                        url = "https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-v3",
                        sha256 = "unsupported-mobile-benchmark",
                        fileName = "sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-fp32",
                        licenseUrl = "https://huggingface.co/nvidia/parakeet-tdt-0.6b-v3",
                    ),
                    isOfflineCapable = false,
                    runtime = ModelRuntime(
                        kind = ModelRuntimeKind.UnsupportedMobileBenchmark,
                        requiredFiles = emptyList(),
                    ),
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
