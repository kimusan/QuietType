package dk.schulz.voiceme.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelReadinessTest {
    @Test
    fun verifiedArchiveIsNotReadyForDictationUntilPrepared() {
        val selected = ModelCatalog.default().recommended.id
        val downloaded = ModelCatalogReducer.markDownloaded(
            state = ModelCatalogState.default().selectModel(selected),
            modelId = selected,
        )

        assertEquals(ModelInstallState.DownloadedArchive, downloaded.selectedInstallState)
        assertFalse(downloaded.isReadyForDictation)

        val prepared = ModelCatalogReducer.markPrepared(downloaded, selected)

        assertEquals(ModelInstallState.PreparedForDictation, prepared.selectedInstallState)
        assertTrue(prepared.isReadyForDictation)
    }

    @Test
    fun deletingModelClearsBothDownloadedAndPreparedMarkers() {
        val selected = ModelCatalog.default().recommended.id
        val prepared = ModelCatalogReducer.markPrepared(
            state = ModelCatalogReducer.markDownloaded(ModelCatalogState.default(), selected),
            modelId = selected,
        )

        val deleted = ModelCatalogReducer.deleteModel(prepared, selected)

        assertEquals(ModelInstallState.NotDownloaded, deleted.selectedInstallState)
        assertFalse(deleted.downloadedModelIds.contains(selected))
        assertFalse(deleted.preparedModelIds.contains(selected))
        assertFalse(deleted.isReadyForDictation)
    }
}
