package dk.schulz.quiettype.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream

class ModelArtifactInstallerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun installerWritesVerifiedArtifactIntoModelPrivateDirectory() {
        val modelBytes = "QuietType model bytes".encodeToByteArray()
        val model = ModelCatalog.default().recommended.copy(
            artifact = ModelCatalog.default().recommended.artifact.copy(
                sha256 = modelBytes.sha256Hex(),
                fileName = "fake-model.bin",
            ),
        )
        val installer = ModelArtifactInstaller(
            byteSource = FakeArtifactByteSource(modelBytes),
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
            byteSource = FakeArtifactByteSource("QuietType model bytes".encodeToByteArray()),
            modelRootDirectory = temporaryFolder.root,
        )

        val result = installer.install(model)

        assertTrue(result is ModelArtifactInstallResult.ChecksumMismatch)
        assertFalse(temporaryFolder.root.walkTopDown().any { it.isFile })
    }

    @Test
    fun installerMarksArchivePreparedWhenRuntimeFilesArePresent() {
        val archiveBytes = modelArchiveBytes(
            "sherpa-onnx/model.int8.onnx" to "fake onnx".encodeToByteArray(),
            "sherpa-onnx/tokens.txt" to "<blk>\na\nb\n".encodeToByteArray(),
        )
        val ctcModel = ModelCatalog.default().modelById("sherpa-onnx-nemo-fast-conformer-ctc-multilingual-int8")!!
        val model = ctcModel.copy(
            artifact = ctcModel.artifact.copy(
                sha256 = archiveBytes.sha256Hex(),
            ),
        )
        val installer = ModelArtifactInstaller(
            byteSource = FakeArtifactByteSource(archiveBytes),
            modelRootDirectory = temporaryFolder.root,
        )

        val result = installer.install(model)

        assertTrue(result is ModelArtifactInstallResult.Installed)
        result as ModelArtifactInstallResult.Installed
        assertEquals(ModelInstallState.PreparedForDictation, result.installState)
    }

    @Test
    fun installerKeepsArchiveDownloadedOnlyWhenRuntimeFilesAreMissing() {
        val archiveBytes = modelArchiveBytes(
            "sherpa-onnx/model.int8.onnx" to "fake onnx".encodeToByteArray(),
        )
        val ctcModel = ModelCatalog.default().modelById("sherpa-onnx-nemo-fast-conformer-ctc-multilingual-int8")!!
        val model = ctcModel.copy(
            artifact = ctcModel.artifact.copy(
                sha256 = archiveBytes.sha256Hex(),
            ),
        )
        val installer = ModelArtifactInstaller(
            byteSource = FakeArtifactByteSource(archiveBytes),
            modelRootDirectory = temporaryFolder.root,
        )

        val result = installer.install(model)

        assertTrue(result is ModelArtifactInstallResult.Installed)
        result as ModelArtifactInstallResult.Installed
        assertEquals(ModelInstallState.DownloadedArchive, result.installState)
    }

    @Test
    fun installerExtractsVerifiedArchiveIntoRuntimeDirectoryWhenRequiredFilesArePresent() {
        val archiveBytes = modelArchiveBytes(
            "sherpa-onnx/encoder.int8.onnx" to "fake encoder".encodeToByteArray(),
            "sherpa-onnx/decoder.int8.onnx" to "fake decoder".encodeToByteArray(),
            "sherpa-onnx/joiner.int8.onnx" to "fake joiner".encodeToByteArray(),
            "sherpa-onnx/tokens.txt" to "<blk>\na\nb\n".encodeToByteArray(),
        )
        val model = ModelCatalog.default().recommended.copy(
            artifact = ModelCatalog.default().recommended.artifact.copy(
                sha256 = archiveBytes.sha256Hex(),
            ),
        )
        val installer = ModelArtifactInstaller(
            byteSource = FakeArtifactByteSource(archiveBytes),
            modelRootDirectory = temporaryFolder.root,
        )

        val result = installer.install(model)

        assertTrue(result is ModelArtifactInstallResult.Installed)
        result as ModelArtifactInstallResult.Installed
        assertEquals(ModelInstallState.PreparedForDictation, result.installState)
        assertTrue(result.runtimeDirectory.resolve("encoder.int8.onnx").exists())
        assertTrue(result.runtimeDirectory.resolve("decoder.int8.onnx").exists())
        assertTrue(result.runtimeDirectory.resolve("joiner.int8.onnx").exists())
        assertTrue(result.runtimeDirectory.resolve("tokens.txt").exists())
    }

    @Test
    fun installerRejectsArchiveEntriesThatEscapeRuntimeDirectory() {
        val archiveBytes = modelArchiveBytes(
            "../escape.txt" to "bad".encodeToByteArray(),
            "sherpa-onnx/encoder.int8.onnx" to "fake encoder".encodeToByteArray(),
        )
        val model = ModelCatalog.default().recommended.copy(
            artifact = ModelCatalog.default().recommended.artifact.copy(
                sha256 = archiveBytes.sha256Hex(),
            ),
        )
        val installer = ModelArtifactInstaller(
            byteSource = FakeArtifactByteSource(archiveBytes),
            modelRootDirectory = temporaryFolder.root,
        )

        val result = installer.install(model)

        assertTrue(result is ModelArtifactInstallResult.InstallFailed)
        assertFalse(temporaryFolder.root.resolve("escape.txt").exists())
    }

    @Test
    fun installerReportsByteProgressWhileDownloading() {
        val modelBytes = "1234567890".encodeToByteArray()
        val model = ModelCatalog.default().recommended.copy(
            artifact = ModelCatalog.default().recommended.artifact.copy(
                sha256 = modelBytes.sha256Hex(),
                fileName = "fake-model.bin",
            ),
        )
        val installer = ModelArtifactInstaller(
            byteSource = FakeArtifactByteSource(modelBytes, chunkSize = 5),
            modelRootDirectory = temporaryFolder.newFolder("models"),
        )
        val progressEvents = mutableListOf<ModelDownloadProgress>()

        val result = installer.install(model, onProgress = progressEvents::add)

        assertTrue(result is ModelArtifactInstallResult.Installed)
        assertEquals(listOf(50, 100), progressEvents.map { it.percent })
        assertEquals(listOf(5L, 10L), progressEvents.map { it.bytesRead })
    }

    @Test
    fun deleteRemovesInstalledModelDirectory() {
        val modelBytes = "QuietType model bytes".encodeToByteArray()
        val model = ModelCatalog.default().recommended.copy(
            artifact = ModelCatalog.default().recommended.artifact.copy(
                sha256 = modelBytes.sha256Hex(),
                fileName = "fake-model.bin",
            ),
        )
        val installer = ModelArtifactInstaller(
            byteSource = FakeArtifactByteSource(modelBytes),
            modelRootDirectory = temporaryFolder.root,
        )
        val installed = installer.install(model)

        assertTrue(installed is ModelArtifactInstallResult.Installed)

        assertTrue(installer.delete(model))
        assertFalse(temporaryFolder.root.resolve(model.id).exists())
    }

    private fun modelArchiveBytes(vararg entries: Pair<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        BZip2CompressorOutputStream(output).use { compressed ->
            TarArchiveOutputStream(compressed).use { tar ->
                entries.forEach { (name, bytes) ->
                    val entry = TarArchiveEntry(name).apply { size = bytes.size.toLong() }
                    tar.putArchiveEntry(entry)
                    tar.write(bytes)
                    tar.closeArchiveEntry()
                }
            }
        }
        return output.toByteArray()
    }

    private fun ByteArray.sha256Hex(): String = java.security.MessageDigest
        .getInstance("SHA-256")
        .digest(this)
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

    private class FakeArtifactByteSource(
        private val bytes: ByteArray,
        private val chunkSize: Int = bytes.size.coerceAtLeast(1),
    ) : ArtifactByteSource {
        override fun open(artifact: ModelArtifact): ArtifactDownloadStream = ArtifactDownloadStream(
            inputStream = object : ByteArrayInputStream(bytes) {
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int = super.read(
                    buffer,
                    offset,
                    length.coerceAtMost(chunkSize),
                )
            },
            totalBytes = bytes.size.toLong(),
        )
    }
}
