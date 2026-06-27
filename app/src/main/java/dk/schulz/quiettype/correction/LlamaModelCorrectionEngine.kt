package dk.schulz.quiettype.correction

import de.kherud.llama.InferenceParameters
import de.kherud.llama.LlamaModel
import de.kherud.llama.ModelParameters
import dk.schulz.quiettype.accessibility.PreparedTextCorrection
import java.io.File

class LlamaModelCorrectionEngine : CorrectionEngine {
    private var loadedModelPath: String? = null
    private var loadedModel: LlamaModel? = null

    override fun canHandle(model: CorrectionModel): Boolean = model.runtimeKind == CorrectionRuntimeKind.Llama

    override fun correct(
        prepared: PreparedTextCorrection,
        model: CorrectionModel,
        modelFile: File?,
    ): String {
        val resolvedModelFile = requireNotNull(modelFile) { "Missing local GGUF file for correction model ${model.id}." }
        require(resolvedModelFile.isFile) { "Correction model file not found: ${resolvedModelFile.absolutePath}" }

        val llm = synchronized(this) {
            if (loadedModelPath != resolvedModelFile.absolutePath || loadedModel == null) {
                loadedModel?.close()
                loadedModel = LlamaModel(
                    ModelParameters()
                        .setModel(resolvedModelFile.absolutePath)
                        .setCtxSize(1024)
                        .setThreads(maxOf(1, Runtime.getRuntime().availableProcessors() / 2))
                        .setGpuLayers(0)
                        .disableLog(),
                )
                loadedModelPath = resolvedModelFile.absolutePath
            }
            requireNotNull(loadedModel)
        }

        val response = llm.complete(
            InferenceParameters("")
                .setMessages(
                    "You correct dictated text. Return only the corrected text. Keep the same language. Preserve meaning and tone. Fix spacing, casing, punctuation, and obvious transcription mistakes. Do not explain your edits.",
                    listOf(de.kherud.llama.Pair("user", prepared.textToCorrect)),
                )
                .setUseChatTemplate(true)
                .setTemperature(0.1f)
                .setTopK(20)
                .setTopP(0.9f)
                .setNPredict(96)
                .setStopStrings("\n\n", "<|im_end|>", "<|endoftext|>"),
        )
        return sanitize(response)
    }

    override fun close() {
        synchronized(this) {
            loadedModel?.close()
            loadedModel = null
            loadedModelPath = null
        }
    }

    private fun sanitize(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```") && cleaned.endsWith("```") && cleaned.length > 6) {
            cleaned = cleaned.removePrefix("```").removeSuffix("```").trim()
        }
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length > 1) {
            cleaned = cleaned.substring(1, cleaned.length - 1).trim()
        }
        cleaned = cleaned
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            .orEmpty()
        if (cleaned.length < 2 && cleaned.none { it.isLetterOrDigit() }) {
            return ""
        }
        if (cleaned.count { it.isLetterOrDigit() } < 2) {
            return ""
        }
        return cleaned.ifBlank { raw.trim() }
    }
}

