package dk.schulz.voiceme.dictation

import dk.schulz.voiceme.models.ModelCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SherpaRuntimeConfigTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun parakeetInt8PreparedRuntimeBuildsOfflineTransducerConfig() {
        val model = ModelCatalog.default().recommended
        val runtime = temporaryFolder.newFolder("runtime")
        listOf("encoder.int8.onnx", "decoder.int8.onnx", "joiner.int8.onnx", "tokens.txt").forEach { name ->
            runtime.resolve(name).writeText("fake")
        }

        assertTrue(SherpaRuntimeConfig.canRunDictation(model, runtime))
        assertFalse(SherpaRuntimeConfig.canRunOnline(model, runtime))

        val config = SherpaRuntimeConfig.buildOfflineRecognizerConfig(
            model = model,
            runtimeDirectory = runtime,
            numThreads = 2,
        )

        assertEquals(SherpaRuntimeConfig.SampleRateHz, config.featConfig.sampleRate)
        assertEquals(80, config.featConfig.featureDim)
        assertEquals("greedy_search", config.decodingMethod)
        assertEquals(2, config.modelConfig.numThreads)
        assertEquals("cpu", config.modelConfig.provider)
        assertEquals(runtime.resolve("encoder.int8.onnx").absolutePath, config.modelConfig.transducer.encoder)
        assertEquals(runtime.resolve("tokens.txt").absolutePath, config.modelConfig.tokens)
    }

    @Test
    fun compactCtcPreparedRuntimeBuildsOfflineNemoCtcConfig() {
        val model = ModelCatalog.default().modelById("sherpa-onnx-nemo-fast-conformer-ctc-multilingual-int8")!!
        val runtime = temporaryFolder.newFolder("runtime")
        listOf("model.int8.onnx", "tokens.txt").forEach { name ->
            runtime.resolve(name).writeText("fake")
        }

        assertTrue(SherpaRuntimeConfig.canRunDictation(model, runtime))

        val config = SherpaRuntimeConfig.buildOfflineRecognizerConfig(
            model = model,
            runtimeDirectory = runtime,
            numThreads = 1,
        )

        assertEquals(runtime.resolve("model.int8.onnx").absolutePath, config.modelConfig.nemo.model)
        assertEquals(runtime.resolve("tokens.txt").absolutePath, config.modelConfig.tokens)
    }

    @Test
    fun missingRuntimeFilesCannotRunDictation() {
        val model = ModelCatalog.default().recommended
        val runtime = temporaryFolder.newFolder("runtime")
        runtime.resolve("encoder.int8.onnx").writeText("fake")

        assertFalse(SherpaRuntimeConfig.canRunDictation(model, runtime))
    }

    @Test
    fun emptyRuntimeFilesCannotRunDictation() {
        val model = ModelCatalog.default().recommended
        val runtime = temporaryFolder.newFolder("runtime")
        listOf("encoder.int8.onnx", "decoder.int8.onnx", "joiner.int8.onnx", "tokens.txt").forEach { name ->
            runtime.resolve(name).writeText("fake")
        }
        runtime.resolve("decoder.int8.onnx").writeBytes(ByteArray(0))

        assertFalse(SherpaRuntimeConfig.canRunDictation(model, runtime))
    }
}
