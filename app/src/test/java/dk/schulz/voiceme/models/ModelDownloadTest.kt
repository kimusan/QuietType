package dk.schulz.voiceme.models

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
        val bytes = "VoiceMe model bytes".encodeToByteArray()
        val checksum = "20412ad7c4f447e06bad86425bdd146b1bb05d80049d925c39ac17767a5b34dc"

        val result = ModelArtifactVerifier.verifySha256(bytes, checksum)

        assertTrue(result)
    }

    @Test
    fun checksumVerifierRejectsWrongSha256() {
        val bytes = "VoiceMe model bytes".encodeToByteArray()
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
