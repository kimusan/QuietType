package dk.schulz.quiettype.correction

import dk.schulz.quiettype.accessibility.PreparedTextCorrection
import java.io.Closeable
import java.io.File

interface CorrectionEngine : Closeable {
    fun canHandle(model: CorrectionModel): Boolean

    fun correct(
        prepared: PreparedTextCorrection,
        model: CorrectionModel,
        modelFile: File? = null,
    ): String

    override fun close() = Unit
}
