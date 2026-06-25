package dk.schulz.quiettype.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelDownloadTest {
    @Test
    fun downloadableCatalogEntriesExposeHttpsArtifactAndChecksum() {
        val model = ModelCatalog.default().recommended

        assertEquals("https", model.artifact.url.substringBefore(":"))
        assertEquals(64, model.artifact.sha256.length)
        assertTrue(model.artifact.licenseUrl.startsWith("https://"))
        assertTrue(
            model.artifact.fileName.endsWith(".zip") ||
                model.artifact.fileName.endsWith(".tar.bz2"),
        )
    }

    @Test
    fun checksumVerifierAcceptsExpectedSha256() {
        val bytes = "QuietType model bytes".encodeToByteArray()
        val checksum = "3f6a05715d17b2f05d563b2ccb806bfb1a87a910e202cd25462625cd94c7a686"

        val result = ModelArtifactVerifier.verifySha256(bytes, checksum)

        assertTrue(result)
    }

    @Test
    fun checksumVerifierRejectsWrongSha256() {
        val bytes = "QuietType model bytes".encodeToByteArray()
        val wrongChecksum = "0000000000000000000000000000000000000000000000000000000000000000"

        val result = ModelArtifactVerifier.verifySha256(bytes, wrongChecksum)

        assertFalse(result)
    }

    @Test
    fun downloadPlanRequiresUserInitiatedNetworkAndVerifiedArchiveBeforePreparation() {
        val model = ModelCatalog.default().recommended
        val plan = ModelDownloadPlan.forModel(model)

        assertTrue(plan.requiresNetworkPermission)
        assertTrue(plan.requiresExplicitUserAction)
        assertEquals(ModelDownloadStep.DownloadHttpsArtifact, plan.steps[0])
        assertEquals(ModelDownloadStep.VerifySha256, plan.steps[1])
        assertEquals(ModelDownloadStep.UnpackPrivateModelFiles, plan.steps[2])
        assertEquals(ModelDownloadStep.MarkDownloadedArchiveAfterVerification, plan.steps[3])
    }
}
