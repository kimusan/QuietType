package dk.schulz.quiettype.correction

import dk.schulz.quiettype.accessibility.FocusedFieldSnapshot
import dk.schulz.quiettype.accessibility.PreparedTextCorrection
import dk.schulz.quiettype.accessibility.TextCorrectionBlockReason
import dk.schulz.quiettype.accessibility.TextCorrectionRequest
import dk.schulz.quiettype.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class CorrectionPipelineTest {
    @Test
    fun deterministicPathRemainsDefaultWhenModelCorrectionDisabled() {
        val pipeline = CorrectionPipeline(
            modelRootDirectory = tempDir(),
            deterministicEngine = FakeDeterministicEngine("Hello world."),
            llamaEngine = FakeLlamaEngine("Ignored"),
        )

        val result = pipeline.correct(request("  HELLO   WORLD  "), AppSettings.default())

        val corrected = result as CorrectionExecutionResult.Corrected
        assertEquals("Hello world.", corrected.draft.textToSet)
        assertEquals("Fast local cleanup", corrected.backendLabel)
        assertFalse(corrected.usedFallback)
    }

    @Test
    fun selectedDownloadedModelIsUsedWhenEnabled() {
        val modelRoot = tempDir()
        val model = CorrectionModelCatalog.default().modelById("smollm2-360m-instruct-q4-k-m") ?: error("missing model")
        val modelDir = modelRoot.resolve(model.id).apply { mkdirs() }
        modelDir.resolve(model.artifact!!.fileName).writeText("placeholder")

        val pipeline = CorrectionPipeline(
            modelRootDirectory = modelRoot,
            deterministicEngine = FakeDeterministicEngine("Deterministic fallback."),
            llamaEngine = FakeLlamaEngine("Hello improved world."),
        )
        val settings = AppSettings.default().copy(
            correctionModelEnabled = true,
            selectedCorrectionModelId = model.id,
            downloadedCorrectionModelIds = setOf(model.id),
        )

        val result = pipeline.correct(request("HELLO WORLD"), settings)

        val corrected = result as CorrectionExecutionResult.Corrected
        assertEquals("Hello improved world.", corrected.draft.textToSet)
        assertEquals(model.name, corrected.backendLabel)
        assertFalse(corrected.usedFallback)
    }

    @Test
    fun deterministicFallbackIsUsedWhenModelFails() {
        val modelRoot = tempDir()
        val model = CorrectionModelCatalog.default().modelById("smollm2-360m-instruct-q4-k-m") ?: error("missing model")
        val modelDir = modelRoot.resolve(model.id).apply { mkdirs() }
        modelDir.resolve(model.artifact!!.fileName).writeText("placeholder")

        val pipeline = CorrectionPipeline(
            modelRootDirectory = modelRoot,
            deterministicEngine = FakeDeterministicEngine("Hello world."),
            llamaEngine = ThrowingLlamaEngine(),
        )
        val settings = AppSettings.default().copy(
            correctionModelEnabled = true,
            selectedCorrectionModelId = model.id,
            downloadedCorrectionModelIds = setOf(model.id),
        )

        val result = pipeline.correct(request("HELLO WORLD"), settings)

        val corrected = result as CorrectionExecutionResult.Corrected
        assertTrue(corrected.usedFallback)
        assertEquals(model.name, corrected.attemptedModelName)
        assertEquals("Fast local cleanup", corrected.backendLabel)
        assertEquals("Hello world.", corrected.draft.textToSet)
    }

    @Test
    fun sensitiveFieldStillBlocksBeforeEngineSelection() {
        val pipeline = CorrectionPipeline(
            modelRootDirectory = tempDir(),
            deterministicEngine = FakeDeterministicEngine("Hello world."),
            llamaEngine = FakeLlamaEngine("Hello world."),
        )

        val result = pipeline.correct(
            TextCorrectionRequest(
                focusedField = FocusedFieldSnapshot(
                    packageName = "com.example.notes",
                    className = "android.widget.EditText",
                    isFocused = true,
                    isEditable = true,
                    isPassword = true,
                ),
                existingText = "HELLO WORLD",
                selectionStart = 0,
                selectionEnd = 11,
            ),
            AppSettings.default().copy(correctionModelEnabled = true),
        )

        val blocked = result as CorrectionExecutionResult.Blocked
        assertFalse(blocked.draft.canCorrect)
        assertEquals(TextCorrectionBlockReason.SensitiveField, blocked.draft.blockReason)
    }

    private fun request(text: String) = TextCorrectionRequest(
        focusedField = FocusedFieldSnapshot(
            packageName = "com.example.notes",
            className = "android.widget.EditText",
            isFocused = true,
            isEditable = true,
            isPassword = false,
        ),
        existingText = text,
        selectionStart = 0,
        selectionEnd = text.length,
    )
}

private fun tempDir(): File = createTempDirectory("quiettype-correction-test-").toFile()

private class FakeDeterministicEngine(private val response: String) : CorrectionEngine {
    override fun canHandle(model: CorrectionModel): Boolean = true

    override fun correct(prepared: PreparedTextCorrection, model: CorrectionModel, modelFile: File?): String = response
}

private class FakeLlamaEngine(private val response: String) : CorrectionEngine {
    override fun canHandle(model: CorrectionModel): Boolean = true

    override fun correct(prepared: PreparedTextCorrection, model: CorrectionModel, modelFile: File?): String = response
}

private class ThrowingLlamaEngine : CorrectionEngine {
    override fun canHandle(model: CorrectionModel): Boolean = true

    override fun correct(prepared: PreparedTextCorrection, model: CorrectionModel, modelFile: File?): String {
        error("boom")
    }
}
