package dk.schulz.quiettype.correction

import dk.schulz.quiettype.accessibility.PreparedTextCorrection
import java.io.File

class DeterministicCorrectionEngine : CorrectionEngine {
    override fun canHandle(model: CorrectionModel): Boolean = model.runtimeKind == CorrectionRuntimeKind.Deterministic

    override fun correct(
        prepared: PreparedTextCorrection,
        model: CorrectionModel,
        modelFile: File?,
    ): String = QuickCorrectionPolicy.autoCorrect(prepared.textToCorrect)
}
