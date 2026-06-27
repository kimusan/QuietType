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
    WhisperCpp,
    UnsupportedMobileBenchmark,
}

data class LanguageProfile(
    val id: String,
    val displayName: String,
    val description: String,
    val preferredLanguageTags: List<String>,
    val defaultModelId: String?,
    val isCustom: Boolean = false,
)

data class ModelCatalog(
    val models: List<VoiceModel>,
    val languageProfiles: List<LanguageProfile>,
) {
    val recommended: VoiceModel = models.first()
    val defaultProfile: LanguageProfile = languageProfiles.first()

    fun modelById(modelId: String): VoiceModel? = models.firstOrNull { it.id == modelId }

    fun profileById(profileId: String): LanguageProfile? = languageProfiles.firstOrNull { it.id == profileId }

    companion object {
        fun default(): ModelCatalog = ModelCatalog(
            languageProfiles = listOf(
                LanguageProfile(
                    id = "da-multilingual",
                    displayName = "Danish + multilingual",
                    description = "Best default for Danish and mixed European-language dictation.",
                    preferredLanguageTags = listOf("da", "en", "de", "sv", "no"),
                    defaultModelId = "sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8",
                ),
                LanguageProfile(
                    id = "en-fast",
                    displayName = "English low latency",
                    description = "Small streaming English profile for faster experiments.",
                    preferredLanguageTags = listOf("en"),
                    defaultModelId = "sherpa-onnx-streaming-zipformer-en-int8",
                ),
                LanguageProfile(
                    id = "compact-multilingual",
                    displayName = "Compact multilingual",
                    description = "Smaller multilingual fallback when size matters more than Danish-first quality.",
                    preferredLanguageTags = listOf("en", "de", "fr", "es", "it", "pl", "uk"),
                    defaultModelId = "sherpa-onnx-nemo-fast-conformer-ctc-multilingual-int8",
                ),
                LanguageProfile(
                    id = "custom",
                    displayName = "Custom",
                    description = "Show all speech models and choose manually.",
                    preferredLanguageTags = emptyList(),
                    defaultModelId = null,
                    isCustom = true,
                ),
            ),
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
                    id = "whisper-cpp-ggml-tiny",
                    name = "Whisper tiny multilingual",
                    description = "Whisper.cpp tiny GGML model. Small multilingual option; planned for accelerated Android decoding with whisper.cpp OpenCL/GPU paths where available and CPU fallback elsewhere.",
                    engine = "whisper.cpp / ggml tiny",
                    language = "Multilingual",
                    sizeMegabytes = 75,
                    license = "MIT runtime; OpenAI Whisper model license/terms require release NOTICE review",
                    artifact = ModelArtifact(
                        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
                        sha256 = "be07e048e1e599ad46341c8d2a135645097a538221678b7acdd1b1919c6e1b21",
                        fileName = "ggml-tiny.bin",
                        licenseUrl = "https://huggingface.co/ggerganov/whisper.cpp",
                    ),
                    isOfflineCapable = true,
                    runtime = ModelRuntime(
                        kind = ModelRuntimeKind.WhisperCpp,
                        requiredFiles = listOf("ggml-tiny.bin"),
                    ),
                ),
                VoiceModel(
                    id = "whisper-cpp-ggml-base",
                    name = "Whisper base multilingual",
                    description = "Whisper.cpp base GGML model. Better accuracy than tiny; planned for Android hardware acceleration via whisper.cpp OpenCL where available, with NNAPI/GPU delegate investigation tracked before release.",
                    engine = "whisper.cpp / ggml base",
                    language = "Multilingual",
                    sizeMegabytes = 142,
                    license = "MIT runtime; OpenAI Whisper model license/terms require release NOTICE review",
                    artifact = ModelArtifact(
                        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
                        sha256 = "60ed5bc3dd14eea856493d334349b405782ddcaf0028d4b5df4088345fba2efe",
                        fileName = "ggml-base.bin",
                        licenseUrl = "https://huggingface.co/ggerganov/whisper.cpp",
                    ),
                    isOfflineCapable = true,
                    runtime = ModelRuntime(
                        kind = ModelRuntimeKind.WhisperCpp,
                        requiredFiles = listOf("ggml-base.bin"),
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
    val selectedLanguageProfileId: String = ModelCatalog.default().defaultProfile.id,
    val catalog: ModelCatalog = ModelCatalog.default(),
) {
    val selectedModel: VoiceModel = catalog.modelById(selectedModelId) ?: catalog.recommended
    val selectedLanguageProfile: LanguageProfile = catalog.profileById(selectedLanguageProfileId) ?: catalog.defaultProfile
    val isCustomModelSelection: Boolean = selectedLanguageProfile.isCustom
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

    fun selectLanguageProfile(profileId: String): ModelCatalogState {
        val profile = catalog.profileById(profileId) ?: return this
        if (profile.isCustom || profile.defaultModelId == null) {
            return copy(selectedLanguageProfileId = profile.id)
        }
        return copy(
            selectedLanguageProfileId = profile.id,
            selectedModelId = profile.defaultModelId,
        )
    }

    companion object {
        fun default(): ModelCatalogState {
            val catalog = ModelCatalog.default()
            return ModelCatalogState(
                selectedModelId = catalog.defaultProfile.defaultModelId ?: catalog.recommended.id,
                selectedLanguageProfileId = catalog.defaultProfile.id,
                downloadedModelIds = emptySet(),
                preparedModelIds = emptySet(),
                catalog = catalog,
            )
        }
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
