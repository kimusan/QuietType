package dk.schulz.quiettype.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogParakeetTest {
    @Test
    fun recommendedModelIsParakeetV3Int8MultilingualSherpaArtifact() {
        val recommended = ModelCatalog.default().recommended

        assertEquals("sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8", recommended.id)
        assertTrue(recommended.language.contains("Danish"))
        assertTrue(recommended.description.contains("multilingual"))
        assertTrue(recommended.artifact.url.endsWith("sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8.tar.bz2"))
        assertEquals(ModelRuntimeKind.SherpaOnnxOfflineTransducer, recommended.runtime.kind)
        assertEquals(
            setOf("encoder.int8.onnx", "decoder.int8.onnx", "joiner.int8.onnx", "tokens.txt"),
            recommended.runtime.requiredFiles.toSet(),
        )
    }

    @Test
    fun fp32ParakeetIsListedAsUnsupportedForMobileRuntime() {
        val fp32 = ModelCatalog.default().modelById("sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-fp32")

        requireNotNull(fp32)
        assertFalse(fp32.isOfflineCapable)
        assertEquals(ModelRuntimeKind.UnsupportedMobileBenchmark, fp32.runtime.kind)
        assertTrue(fp32.description.contains("not recommended"))
    }
}
