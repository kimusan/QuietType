package dk.schulz.quiettype.correction

import android.content.Context
import dk.schulz.quiettype.accessibility.PreparedTextCorrection
import dk.schulz.quiettype.llama.LlamaAndroidBridge
import java.io.File

class LlamaModelCorrectionEngine(
    context: Context,
) : CorrectionEngine {
    private val bridge = LlamaAndroidBridge(context.applicationContext)

    override fun canHandle(model: CorrectionModel): Boolean = model.runtimeKind == CorrectionRuntimeKind.Llama

    override fun correct(
        prepared: PreparedTextCorrection,
        model: CorrectionModel,
        modelFile: File?,
    ): String {
        val resolvedModelFile = requireNotNull(modelFile) { "Missing local GGUF file for correction model ${model.id}." }
        require(resolvedModelFile.isFile) { "Correction model file not found: ${resolvedModelFile.absolutePath}" }

        val response = bridge.complete(
            modelFile = resolvedModelFile,
            prompt = buildPrompt(prepared.textToCorrect),
            maxTokens = 96,
            contextSize = 1024,
            temperature = 0.1f,
            topK = 20,
            topP = 0.9f,
        )
        return sanitize(response)
    }

    override fun close() {
        bridge.close()
    }

    private fun buildPrompt(text: String): String = """
        You correct dictated text.
        Return only the corrected text in the same language.
        Preserve meaning and tone.
        Fix spacing, casing, punctuation, and obvious transcription mistakes.
        Do not explain your edits.

        Text:
        $text

        Corrected text:
    """.trimIndent()

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
