package dk.schulz.quiettype.models

import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

data class ArtifactDownloadStream(
    val inputStream: InputStream,
    val totalBytes: Long,
)

interface ArtifactByteSource {
    fun open(artifact: ModelArtifact): ArtifactDownloadStream
}

object HttpsArtifactByteSource : ArtifactByteSource {
    private const val TimeoutMillis = 30_000

    override fun open(artifact: ModelArtifact): ArtifactDownloadStream {
        require(artifact.url.startsWith("https://")) { "Model artifacts must be downloaded over HTTPS" }
        val connection = URL(artifact.url).openConnection() as HttpURLConnection
        connection.connectTimeout = TimeoutMillis
        connection.readTimeout = TimeoutMillis
        connection.instanceFollowRedirects = true
        HttpDownloadPolicy.requireSuccessfulStatus(connection.responseCode)
        return ArtifactDownloadStream(
            inputStream = object : InputStream() {
                private val delegate = connection.inputStream

                override fun read(): Int = delegate.read()

                override fun read(buffer: ByteArray, offset: Int, length: Int): Int = delegate.read(buffer, offset, length)

                override fun close() {
                    runCatching { delegate.close() }
                    connection.disconnect()
                }
            },
            totalBytes = connection.contentLengthLong,
        )
    }
}

sealed class ModelArtifactInstallResult {
    data class Installed(
        val artifactFile: File,
        val runtimeDirectory: File,
        val installState: ModelInstallState = ModelInstallState.DownloadedArchive,
    ) : ModelArtifactInstallResult()

    data class ChecksumMismatch(
        val expectedSha256: String,
        val actualSha256: String,
    ) : ModelArtifactInstallResult()

    data class InstallFailed(
        val reason: String,
    ) : ModelArtifactInstallResult()
}

data class ModelArchiveExtractionLimits(
    val maxEntries: Int = 4_096,
    val maxExtractedBytes: Long = 2L * 1024L * 1024L * 1024L,
)

class ModelArtifactInstaller(
    private val byteSource: ArtifactByteSource = HttpsArtifactByteSource,
    private val modelRootDirectory: File,
    private val extractionLimits: ModelArchiveExtractionLimits = ModelArchiveExtractionLimits(),
    private val availableBytes: () -> Long = { modelRootDirectory.usableSpace },
) {
    fun install(
        model: VoiceModel,
        onProgress: (ModelDownloadProgress) -> Unit = {},
    ): ModelArtifactInstallResult {
        val directory = model.directory().apply { mkdirs() }
        val tempFile = File(directory, "${model.artifact.fileName}.download")
        val artifactFile = File(directory, model.artifact.fileName)
        val runtimeDirectory = model.runtimeDirectory()
        val digest = MessageDigest.getInstance("SHA-256")
        val fallbackTotalBytes = model.sizeMegabytes.toLong() * 1024L * 1024L
        val requiredFreeBytes = fallbackTotalBytes * 2L
        if (availableBytes() < requiredFreeBytes) {
            model.directory().deleteRecursively()
            return ModelArtifactInstallResult.InstallFailed(
                "Not enough free space to download and prepare ${model.name}. Need at least ${requiredFreeBytes / (1024L * 1024L)} MB available.",
            )
        }

        val download = byteSource.open(model.artifact)
        val totalBytes = if (download.totalBytes > 0L) download.totalBytes else fallbackTotalBytes
        var bytesRead = 0L
        download.inputStream.use { input ->
            tempFile.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    bytesRead += read.toLong()
                    digest.update(buffer, 0, read)
                    output.write(buffer, 0, read)
                    onProgress(
                        ModelDownloadProgress(
                            modelId = model.id,
                            bytesRead = bytesRead,
                            totalBytes = totalBytes,
                        ),
                    )
                }
            }
        }

        val actualSha256 = digest.digest().toHexString()
        if (!actualSha256.equals(model.artifact.sha256, ignoreCase = true)) {
            model.directory().deleteRecursively()
            return ModelArtifactInstallResult.ChecksumMismatch(
                expectedSha256 = model.artifact.sha256,
                actualSha256 = actualSha256,
            )
        }

        if (artifactFile.exists()) artifactFile.delete()
        if (!tempFile.renameTo(artifactFile)) {
            model.directory().deleteRecursively()
            return ModelArtifactInstallResult.InstallFailed("Could not move verified download into model directory")
        }

        val installState = when (val extraction = extractRuntimeFiles(model, artifactFile, runtimeDirectory)) {
            RuntimeExtractionResult.Prepared -> ModelInstallState.PreparedForDictation
            RuntimeExtractionResult.NotPrepared -> ModelInstallState.DownloadedArchive
            is RuntimeExtractionResult.Failed -> {
                model.directory().deleteRecursively()
                return ModelArtifactInstallResult.InstallFailed(extraction.reason)
            }
        }

        return ModelArtifactInstallResult.Installed(
            artifactFile = artifactFile,
            runtimeDirectory = runtimeDirectory,
            installState = installState,
        )
    }

    fun delete(model: VoiceModel): Boolean = model.directory().deleteRecursively()

    private fun extractRuntimeFiles(
        model: VoiceModel,
        artifactFile: File,
        runtimeDirectory: File,
    ): RuntimeExtractionResult {
        if (!artifactFile.name.endsWith(".tar.bz2")) return RuntimeExtractionResult.NotPrepared
        if (model.runtime.requiredFiles.isEmpty()) return RuntimeExtractionResult.NotPrepared

        runtimeDirectory.deleteRecursively()
        runtimeDirectory.mkdirs()
        return runCatching {
            var extractedEntries = 0
            var extractedBytes = 0L
            TarArchiveInputStream(BZip2CompressorInputStream(BufferedInputStream(artifactFile.inputStream()))).use { tar ->
                while (true) {
                    val entry = tar.nextEntry ?: break
                    if (entry.isDirectory) continue
                    extractedEntries += 1
                    if (extractedEntries > extractionLimits.maxEntries) {
                        return RuntimeExtractionResult.Failed("Model archive contains too many files")
                    }

                    if (entry.name.startsWith("/") || entry.name.split('/').any { it == ".." }) {
                        return RuntimeExtractionResult.Failed("Model archive entry escapes runtime directory: ${entry.name}")
                    }
                    val outputName = entry.name.substringAfterLast('/')
                    if (outputName.isBlank()) continue
                    val outputFile = File(runtimeDirectory, outputName)
                    val canonicalRuntime = runtimeDirectory.canonicalFile
                    val canonicalOutput = outputFile.canonicalFile
                    if (!canonicalOutput.path.startsWith(canonicalRuntime.path + File.separator)) {
                        return RuntimeExtractionResult.Failed("Model archive entry escapes runtime directory: ${entry.name}")
                    }
                    outputFile.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = tar.read(buffer)
                            if (read == -1) break
                            extractedBytes += read.toLong()
                            if (extractedBytes > extractionLimits.maxExtractedBytes) {
                                return RuntimeExtractionResult.Failed("Model archive is too large to extract safely")
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                }
            }
            if (model.runtime.requiredFiles.all { runtimeDirectory.resolve(it).isFile }) {
                RuntimeExtractionResult.Prepared
            } else {
                runtimeDirectory.deleteRecursively()
                RuntimeExtractionResult.NotPrepared
            }
        }.getOrElse { error ->
            runtimeDirectory.deleteRecursively()
            RuntimeExtractionResult.Failed(error.message ?: "Could not extract model runtime files")
        }
    }

    private fun VoiceModel.directory(): File = File(modelRootDirectory, id)

    private fun VoiceModel.runtimeDirectory(): File = File(directory(), "runtime")

    private fun ByteArray.toHexString(): String = joinToString(separator = "") { byte -> "%02x".format(byte) }

    private sealed class RuntimeExtractionResult {
        data object Prepared : RuntimeExtractionResult()
        data object NotPrepared : RuntimeExtractionResult()
        data class Failed(val reason: String) : RuntimeExtractionResult()
    }
}
