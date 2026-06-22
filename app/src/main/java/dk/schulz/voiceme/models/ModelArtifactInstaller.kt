package dk.schulz.voiceme.models

import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

interface ArtifactByteSource {
    fun open(artifact: ModelArtifact): InputStream
}

object HttpsArtifactByteSource : ArtifactByteSource {
    private const val TimeoutMillis = 30_000

    override fun open(artifact: ModelArtifact): InputStream {
        require(artifact.url.startsWith("https://")) { "Model artifacts must be downloaded over HTTPS" }
        val connection = URL(artifact.url).openConnection() as HttpURLConnection
        connection.connectTimeout = TimeoutMillis
        connection.readTimeout = TimeoutMillis
        connection.instanceFollowRedirects = true
        return connection.inputStream
    }
}

sealed class ModelArtifactInstallResult {
    data class Installed(
        val artifactFile: File,
        val installState: ModelInstallState = ModelInstallState.Downloaded,
    ) : ModelArtifactInstallResult()

    data class ChecksumMismatch(
        val expectedSha256: String,
        val actualSha256: String,
    ) : ModelArtifactInstallResult()
}

class ModelArtifactInstaller(
    private val byteSource: ArtifactByteSource = HttpsArtifactByteSource,
    private val modelRootDirectory: File,
) {
    fun install(model: VoiceModel): ModelArtifactInstallResult {
        val directory = model.directory().apply { mkdirs() }
        val tempFile = File(directory, "${model.artifact.fileName}.download")
        val artifactFile = File(directory, model.artifact.fileName)
        val digest = MessageDigest.getInstance("SHA-256")

        byteSource.open(model.artifact).use { input ->
            tempFile.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    digest.update(buffer, 0, read)
                    output.write(buffer, 0, read)
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
        tempFile.renameTo(artifactFile)
        return ModelArtifactInstallResult.Installed(artifactFile = artifactFile)
    }

    fun delete(model: VoiceModel): Boolean = model.directory().deleteRecursively()

    private fun VoiceModel.directory(): File = File(modelRootDirectory, id)

    private fun ByteArray.toHexString(): String = joinToString(separator = "") { byte -> "%02x".format(byte) }
}
