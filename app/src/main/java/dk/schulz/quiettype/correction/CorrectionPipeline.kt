package dk.schulz.quiettype.correction

import dk.schulz.quiettype.accessibility.PreparedTextCorrection
import dk.schulz.quiettype.accessibility.TextCorrectionDraft
import dk.schulz.quiettype.accessibility.TextCorrectionPreparationResult
import dk.schulz.quiettype.accessibility.TextCorrectionRequest
import dk.schulz.quiettype.settings.AppSettings
import java.io.Closeable
import java.io.File

sealed class CorrectionExecutionResult {
    data class Blocked(val draft: TextCorrectionDraft) : CorrectionExecutionResult()

    data class Corrected(
        val draft: TextCorrectionDraft,
        val backendLabel: String,
        val usedFallback: Boolean,
        val attemptedModelName: String? = null,
        val failureDetail: String? = null,
    ) : CorrectionExecutionResult()
}

class CorrectionPipeline(
    private val modelRootDirectory: File,
    private val deterministicEngine: CorrectionEngine = DeterministicCorrectionEngine(),
    private val llamaEngine: CorrectionEngine,
) : Closeable {
    fun correct(request: TextCorrectionRequest, settings: AppSettings): CorrectionExecutionResult {
        return when (val preparation = TextCorrectionDraft.prepare(request)) {
            is TextCorrectionPreparationResult.Blocked -> CorrectionExecutionResult.Blocked(preparation.draft)
            is TextCorrectionPreparationResult.Ready -> correctPrepared(preparation.prepared, settings)
        }
    }

    private fun correctPrepared(
        prepared: PreparedTextCorrection,
        settings: AppSettings,
    ): CorrectionExecutionResult {
        val catalog = CorrectionModelCatalog.default()
        val selectedModel = catalog.modelById(settings.selectedCorrectionModelId) ?: catalog.defaultModel
        if (settings.correctionModelEnabled && !selectedModel.isDeterministic) {
            val result = runCatching {
                val modelFile = resolvedModelFile(selectedModel)
                val corrected = llamaEngine.correct(prepared, selectedModel, modelFile)
                require(corrected.isNotBlank()) { "Correction model returned empty text." }
                TextCorrectionDraft.fromPrepared(prepared, corrected)
            }
            val draft = result.getOrNull()
            if (draft != null && draft.canCorrect) {
                return CorrectionExecutionResult.Corrected(
                    draft = draft,
                    backendLabel = selectedModel.name,
                    usedFallback = false,
                )
            }
            val fallbackDraft = TextCorrectionDraft.fromPrepared(
                prepared,
                deterministicEngine.correct(prepared, catalog.defaultModel),
            )
            return CorrectionExecutionResult.Corrected(
                draft = fallbackDraft,
                backendLabel = catalog.defaultModel.name,
                usedFallback = true,
                attemptedModelName = selectedModel.name,
                failureDetail = result.exceptionOrNull()?.message,
            )
        }

        return CorrectionExecutionResult.Corrected(
            draft = TextCorrectionDraft.fromPrepared(
                prepared,
                deterministicEngine.correct(prepared, catalog.defaultModel),
            ),
            backendLabel = catalog.defaultModel.name,
            usedFallback = false,
        )
    }

    private fun resolvedModelFile(model: CorrectionModel): File {
        require(model.isDownloadable) { "Correction model ${model.id} has no downloadable artifact." }
        val artifact = requireNotNull(model.artifact)
        val modelDirectory = modelRootDirectory.resolve(model.id)
        val directFile = modelDirectory.resolve(artifact.fileName)
        if (directFile.isFile) return directFile
        val fallback = modelDirectory.walkTopDown().firstOrNull { it.isFile && it.name == artifact.fileName }
        return requireNotNull(fallback) {
            "Downloaded correction model file ${artifact.fileName} was not found under ${modelDirectory.absolutePath}."
        }
    }

    override fun close() {
        deterministicEngine.close()
        llamaEngine.close()
    }
}
