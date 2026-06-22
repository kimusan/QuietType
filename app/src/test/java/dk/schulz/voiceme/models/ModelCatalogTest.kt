package dk.schulz.voiceme.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogTest {
    @Test
    fun defaultCatalogProvidesCompactOfflineModelFirst() {
        val catalog = ModelCatalog.default()

        assertEquals("sherpa-onnx-streaming-zipformer-en-int8", catalog.recommended.id)
        assertTrue(catalog.recommended.isOfflineCapable)
        assertTrue(catalog.recommended.sizeMegabytes < 150)
    }

    @Test
    fun modelStateRequiresExplicitDownloadBeforeReady() {
        val state = ModelCatalogState.default()

        assertFalse(state.isReadyForDictation)
        assertEquals(ModelInstallState.NotDownloaded, state.selectedInstallState)
    }

    @Test
    fun downloadedSelectedModelIsStoredButNotReadyUntilPreparedAndDeleteResetsIt() {
        val selected = "sherpa-onnx-streaming-zipformer-en-int8"
        val downloaded = ModelCatalogReducer.markDownloaded(
            state = ModelCatalogState.default().selectModel(selected),
            modelId = selected,
        )

        assertFalse(downloaded.isReadyForDictation)
        assertEquals(ModelInstallState.DownloadedArchive, downloaded.selectedInstallState)

        val prepared = ModelCatalogReducer.markPrepared(downloaded, selected)

        assertTrue(prepared.isReadyForDictation)
        assertEquals(ModelInstallState.PreparedForDictation, prepared.selectedInstallState)

        val deleted = ModelCatalogReducer.deleteModel(prepared, selected)

        assertFalse(deleted.isReadyForDictation)
        assertEquals(ModelInstallState.NotDownloaded, deleted.selectedInstallState)
    }
}
