package dk.schulz.voiceme.models

import kotlin.math.roundToInt

data class ModelDownloadProgress(
    val modelId: String,
    val bytesRead: Long,
    val totalBytes: Long,
) {
    val fraction: Float? = if (totalBytes > 0L) {
        (bytesRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        null
    }

    val percent: Int? = fraction?.let { (it * 100).roundToInt().coerceIn(0, 100) }

    fun label(modelName: String): String = if (totalBytes > 0L) {
        "Downloading $modelName: ${percent ?: 0}% (${bytesRead.toMiB()} / ${totalBytes.toMiB()} MiB)"
    } else {
        "Downloading $modelName: ${bytesRead.toMiB()} MiB"
    }

    private fun Long.toMiB(): Long = this / (1024L * 1024L)
}
