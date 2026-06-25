package dk.schulz.voiceme.dictation

import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineNemoEncDecCtcModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import dk.schulz.voiceme.models.ModelRuntimeKind
import dk.schulz.voiceme.models.VoiceModel
import java.io.File

object SherpaRuntimeConfig {
    const val SampleRateHz = 16_000
    private const val FeatureDim = 80
    private const val Provider = "cpu"

    fun canRunDictation(model: VoiceModel, runtimeDirectory: File): Boolean =
        model.runtime.kind in setOf(
            ModelRuntimeKind.SherpaOnnxOfflineTransducer,
            ModelRuntimeKind.SherpaOnnxOfflineCtc,
            ModelRuntimeKind.SherpaOnnxStreamingTransducer,
        ) && model.runtime.requiredFiles.all { runtimeDirectory.resolve(it).isUsableRuntimeFile() }

    fun canRunOnline(model: VoiceModel, runtimeDirectory: File): Boolean =
        model.runtime.kind == ModelRuntimeKind.SherpaOnnxStreamingTransducer &&
            model.runtime.requiredFiles.all { runtimeDirectory.resolve(it).isUsableRuntimeFile() }

    fun buildOfflineRecognizerConfig(
        model: VoiceModel,
        runtimeDirectory: File,
        numThreads: Int = Runtime.getRuntime().availableProcessors().coerceIn(1, 2),
    ): OfflineRecognizerConfig {
        require(model.runtime.kind in setOf(ModelRuntimeKind.SherpaOnnxOfflineTransducer, ModelRuntimeKind.SherpaOnnxOfflineCtc)) {
            "Model ${model.id} is not an offline sherpa-onnx dictation model"
        }
        require(model.runtime.requiredFiles.all { runtimeDirectory.resolve(it).isUsableRuntimeFile() }) {
            "Model ${model.id} is not prepared for offline sherpa-onnx dictation"
        }
        val tokensFile = model.requiredRuntimeFile(exact = "tokens.txt")
        val offlineModelConfig = when (model.runtime.kind) {
            ModelRuntimeKind.SherpaOnnxOfflineTransducer -> {
                val encoderFile = model.requiredRuntimeFile(prefix = "encoder")
                val decoderFile = model.requiredRuntimeFile(prefix = "decoder")
                val joinerFile = model.requiredRuntimeFile(prefix = "joiner")
                OfflineModelConfig(
                    transducer = OfflineTransducerModelConfig(
                        encoder = runtimeDirectory.resolve(encoderFile).absolutePath,
                        decoder = runtimeDirectory.resolve(decoderFile).absolutePath,
                        joiner = runtimeDirectory.resolve(joinerFile).absolutePath,
                    ),
                    tokens = runtimeDirectory.resolve(tokensFile).absolutePath,
                    numThreads = numThreads,
                    provider = Provider,
                    debug = false,
                )
            }
            ModelRuntimeKind.SherpaOnnxOfflineCtc -> {
                val modelFile = model.requiredRuntimeFile(prefix = "model")
                OfflineModelConfig(
                    nemo = OfflineNemoEncDecCtcModelConfig(
                        model = runtimeDirectory.resolve(modelFile).absolutePath,
                    ),
                    tokens = runtimeDirectory.resolve(tokensFile).absolutePath,
                    numThreads = numThreads,
                    provider = Provider,
                    debug = false,
                )
            }
            else -> error("Unsupported offline sherpa model ${model.id}")
        }

        return OfflineRecognizerConfig(
            featConfig = FeatureConfig(
                sampleRate = SampleRateHz,
                featureDim = FeatureDim,
                dither = 0.0f,
            ),
            modelConfig = offlineModelConfig,
            decodingMethod = "greedy_search",
        )
    }

    fun buildOnlineRecognizerConfig(
        model: VoiceModel,
        runtimeDirectory: File,
        numThreads: Int = Runtime.getRuntime().availableProcessors().coerceIn(1, 4),
    ): OnlineRecognizerConfig {
        require(canRunOnline(model, runtimeDirectory)) {
            "Model ${model.id} is not prepared for online sherpa-onnx dictation"
        }
        val encoderFile = model.requiredRuntimeFile(prefix = "encoder")
        val decoderFile = model.requiredRuntimeFile(prefix = "decoder")
        val joinerFile = model.requiredRuntimeFile(prefix = "joiner")
        val tokensFile = model.requiredRuntimeFile(exact = "tokens.txt")

        return OnlineRecognizerConfig(
            featConfig = FeatureConfig(
                sampleRate = SampleRateHz,
                featureDim = FeatureDim,
                dither = 0.0f,
            ),
            modelConfig = OnlineModelConfig(
                transducer = OnlineTransducerModelConfig(
                    encoder = runtimeDirectory.resolve(encoderFile).absolutePath,
                    decoder = runtimeDirectory.resolve(decoderFile).absolutePath,
                    joiner = runtimeDirectory.resolve(joinerFile).absolutePath,
                ),
                tokens = runtimeDirectory.resolve(tokensFile).absolutePath,
                numThreads = numThreads,
                provider = Provider,
                debug = false,
            ),
            enableEndpoint = true,
            decodingMethod = "greedy_search",
        )
    }

    private fun File.isUsableRuntimeFile(): Boolean = isFile && length() > 0L

    private fun VoiceModel.requiredRuntimeFile(prefix: String? = null, exact: String? = null): String =
        runtime.requiredFiles.firstOrNull { file ->
            (exact != null && file == exact) || (prefix != null && file.startsWith(prefix))
        } ?: error("Model $id is missing required $prefix$exact runtime file metadata")
}
