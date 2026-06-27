package dk.schulz.quiettype.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogTest {
    @Test
    fun defaultCatalogProvidesParakeetMultilingualOfflineModelFirst() {
        val catalog = ModelCatalog.default()

        assertEquals("sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8", catalog.recommended.id)
        assertTrue(catalog.recommended.isOfflineCapable)
        assertTrue(catalog.recommended.language.contains("Danish", ignoreCase = true))
        assertTrue(catalog.recommended.language.contains("English", ignoreCase = true))
        assertTrue(catalog.recommended.language.contains("German", ignoreCase = true))
        assertTrue(catalog.recommended.language.contains("Spanish", ignoreCase = true))
        assertTrue(catalog.recommended.sizeMegabytes < 600)
    }

    @Test
    fun modelStateRequiresExplicitDownloadBeforeReady() {
        val state = ModelCatalogState.default()

        assertFalse(state.isReadyForDictation)
        assertEquals(ModelInstallState.NotDownloaded, state.selectedInstallState)
    }

    @Test
    fun downloadedSelectedModelIsStoredButNotReadyUntilPreparedAndDeleteResetsIt() {
        val selected = ModelCatalog.default().recommended.id
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

    @Test
    fun defaultCatalogOnlyContainsDownloadableUsableModels() {
        val catalog = ModelCatalog.default()

        assertTrue(catalog.models.isNotEmpty())
        assertTrue(catalog.models.all { it.artifact.sha256.length == 64 })
        assertTrue(catalog.models.none { it.runtime.kind == ModelRuntimeKind.UnsupportedMobileBenchmark })
    }


    @Test
    fun catalogProvidesLanguageProfilesWithValidDefaultModels() {
        val catalog = ModelCatalog.default()

        assertEquals("da-multilingual", catalog.defaultProfile.id)
        assertTrue(catalog.defaultProfile.displayName.contains("Danish", ignoreCase = true))
        assertTrue(catalog.defaultProfile.preferredLanguageTags.contains("da"))
        assertTrue(catalog.languageProfiles.size >= 3)
        assertTrue(catalog.languageProfiles.all { profile -> catalog.modelById(profile.defaultModelId) != null })
    }

    @Test
    fun selectingLanguageProfileSwitchesToProfileDefaultModel() {
        val state = ModelCatalogState.default()
        val englishProfile = state.catalog.profileById("en-fast") ?: error("missing English profile")

        val updated = state.selectLanguageProfile(englishProfile.id)

        assertEquals(englishProfile.id, updated.selectedLanguageProfileId)
        assertEquals(englishProfile.defaultModelId, updated.selectedModelId)
    }

    @Test
    fun invalidLanguageProfileSelectionIsIgnored() {
        val state = ModelCatalogState.default()

        assertEquals(state, state.selectLanguageProfile("missing-profile"))
    }

}
