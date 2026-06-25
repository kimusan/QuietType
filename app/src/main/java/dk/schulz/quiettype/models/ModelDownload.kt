package dk.schulz.quiettype.models

import java.security.MessageDigest

data class ModelArtifact(
    val url: String,
    val sha256: String,
    val fileName: String,
    val licenseUrl: String,
)

enum class ModelDownloadStep {
    DownloadHttpsArtifact,
    VerifySha256,
    UnpackPrivateModelFiles,
    MarkDownloadedArchiveAfterVerification,
}

data class ModelDownloadPlan(
    val model: VoiceModel,
    val requiresNetworkPermission: Boolean,
    val requiresExplicitUserAction: Boolean,
    val steps: List<ModelDownloadStep>,
) {
    companion object {
        fun forModel(model: VoiceModel): ModelDownloadPlan = ModelDownloadPlan(
            model = model,
            requiresNetworkPermission = model.artifact.url.startsWith("https://"),
            requiresExplicitUserAction = true,
            steps = listOf(
                ModelDownloadStep.DownloadHttpsArtifact,
                ModelDownloadStep.VerifySha256,
                ModelDownloadStep.UnpackPrivateModelFiles,
                ModelDownloadStep.MarkDownloadedArchiveAfterVerification,
            ),
        )
    }
}

object ModelArtifactVerifier {
    fun verifySha256(bytes: ByteArray, expectedSha256: String): Boolean =
        bytes.sha256Hex().equals(expectedSha256, ignoreCase = true)

    private fun ByteArray.sha256Hex(): String = MessageDigest
        .getInstance("SHA-256")
        .digest(this)
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
}
