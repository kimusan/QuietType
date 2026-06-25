package dk.schulz.voiceme.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelDownloadProgressTest {
    @Test
    fun computesPercentageAndHumanReadableMiBLabel() {
        val progress = ModelDownloadProgress(
            modelId = "model",
            bytesRead = 128L * 1024L * 1024L,
            totalBytes = 512L * 1024L * 1024L,
        )

        assertEquals(25, progress.percent)
        assertEquals(0.25f, progress.fraction!!, 0.001f)
        assertEquals("Downloading Parakeet: 25% (128 / 512 MiB)", progress.label("Parakeet"))
    }

    @Test
    fun supportsUnknownTotalDownloadSize() {
        val progress = ModelDownloadProgress(
            modelId = "model",
            bytesRead = 42L * 1024L * 1024L,
            totalBytes = -1L,
        )

        assertNull(progress.percent)
        assertNull(progress.fraction)
        assertEquals("Downloading Parakeet: 42 MiB", progress.label("Parakeet"))
    }
}
