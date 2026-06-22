package dk.schulz.voiceme.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.InputStream

class ModelArtifactInstallerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun installerWritesVerifiedArtifactIntoModelPrivateDirectory() {
        val model = ModelCatalog.default().recommended.copy(
            artifact = ModelCatalog.default().recommended.artifact.copy(
                sha256 = "20412ad7c4f447e06bad86425bdd146b1bb05d80049d925c39ac17767a5b34dc",
            ),
        )
        val installer = ModelArtifactInstaller(
            byteSource = FakeArtifactByteSource("VoiceMe model bytes".encodeToByteArray()),
            modelRootDirectory = temporaryFolder.root,
        )

        val result = installer.install(model)

        assertTrue(result is ModelArtifactInstallResult.Installed)
        result as ModelArtifactInstallResult.Installed
        assertTrue(result.artifactFile.exists())
        assertEquals(model.artifact.fileName, result.artifactFile.name)
        assertTrue(result.artifactFile.path.contains(model.id))
        assertEquals(ModelInstallState.DownloadedArchive, result.installState)
    }

    @Test
    fun installerRejectsChecksumMismatchAndLeavesNoArtifact() {
        val model = ModelCatalog.default().recommended.copy(
            artifact = ModelCatalog.default().recommended.artifact.copy(
                sha256 = "0000000000000000000000000000000000000000000000000000000000000000",
            ),
        )
        val installer = ModelArtifactInstaller(
            byteSource = FakeArtifactByteSource("VoiceMe model bytes".encodeToByteArray()),
            modelRootDirectory = temporaryFolder.root,
        )

        val result = installer.install(model)

        assertTrue(result is ModelArtifactInstallResult.ChecksumMismatch)
        assertFalse(temporaryFolder.root.walkTopDown().any { it.isFile })
    }

    @Test
    fun deleteRemovesInstalledModelDirectory() {
        val model = ModelCatalog.default().recommended.copy(
            artifact = ModelCatalog.default().recommended.artifact.copy(
                sha256 = "20412ad7c4f447e06bad86425bdd146b1bb05d80049d925c39ac17767a5b34dc",
            ),
        )
        val installer = ModelArtifactInstaller(
            byteSource = FakeArtifactByteSource("VoiceMe model bytes".encodeToByteArray()),
            modelRootDirectory = temporaryFolder.root,
        )
        val installed = installer.install(model)

        assertTrue(installed is ModelArtifactInstallResult.Installed)

        assertTrue(installer.delete(model))
        assertFalse(temporaryFolder.root.resolve(model.id).exists())
    }

    private class FakeArtifactByteSource(
        private val bytes: ByteArray,
    ) : ArtifactByteSource {
        override fun open(artifact: ModelArtifact): InputStream = ByteArrayInputStream(bytes)
    }
}
