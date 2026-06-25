package dk.schulz.voiceme.dictation

import com.k2fsa.sherpa.onnx.FeatureConfig
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

    fun canRunOnline(model: VoiceModel, runtimeDirectory: File): Boolean =
        model.runtime.kind in setOf(
            ModelRuntimeKind.SherpaOnnxOfflineTransducer,
            ModelRuntimeKind.SherpaOnnxStreamingTransducer,
        ) && model.runtime.requiredFiles.all { runtimeDirectory.resolve(it).isUsableRuntimeFile() }

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
