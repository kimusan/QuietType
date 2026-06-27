package dk.schulz.quiettype.correction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectionModelCatalogTest {
    @Test
    fun defaultCorrectionModelCatalogOffersDeterministicAndDownloadableMobileOption() {
        val catalog = CorrectionModelCatalog.default()
        val deterministic = catalog.modelById("deterministic-cleanup") ?: error("missing deterministic model")
        val smollm = catalog.modelById("smollm2-360m-instruct-q4-k-m") ?: error("missing SmolLM2 model")

        assertEquals("deterministic-cleanup", catalog.defaultModel.id)
        assertTrue(deterministic.isDeterministic)
        assertFalse(deterministic.isDownloadable)
        assertTrue(smollm.isDownloadable)
        assertEquals("SmolLM2-360M-Instruct-Q4_K_M.gguf", smollm.artifact?.fileName)
        assertEquals("2fa3f013dcdd7b99f9b237717fa0b12d75bbb89984cc1274be1471a465bac9c2", smollm.artifact?.sha256)
    }
}
