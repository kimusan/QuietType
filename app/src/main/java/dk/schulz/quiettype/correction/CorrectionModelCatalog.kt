package dk.schulz.quiettype.correction

import dk.schulz.quiettype.models.ModelArtifact

data class CorrectionModel(
    val id: String,
    val name: String,
    val description: String,
    val engine: String,
    val sizeMegabytes: Int,
    val license: String,
    val artifact: ModelArtifact? = null,
    val isDeterministic: Boolean = false,
) {
    val isDownloadable: Boolean = artifact != null
}

data class CorrectionModelCatalog(
    val models: List<CorrectionModel>,
) {
    val defaultModel: CorrectionModel = models.first()

    fun modelById(modelId: String): CorrectionModel? = models.firstOrNull { it.id == modelId }

    companion object {
        fun default(): CorrectionModelCatalog = CorrectionModelCatalog(
            models = listOf(
                CorrectionModel(
                    id = "deterministic-cleanup",
                    name = "Fast local cleanup",
                    description = "No model download. Normalizes spacing, casing, and punctuation directly on-device.",
                    engine = "Built-in deterministic policy",
                    sizeMegabytes = 0,
                    license = "Built into QuietType",
                    isDeterministic = true,
                ),
                CorrectionModel(
                    id = "smollm2-360m-instruct-q4-k-m",
                    name = "SmolLM2 360M Instruct Q4_K_M",
                    description = "Local correction candidate for mobile devices. Downloadable now; runtime wiring still falls back to built-in cleanup until local LLM inference is integrated.",
                    engine = "GGUF / local LLM candidate",
                    sizeMegabytes = 259,
                    license = "Apache-2.0 model family; GGUF artifact from Hugging Face",
                    artifact = ModelArtifact(
                        url = "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q4_K_M.gguf",
                        sha256 = "2fa3f013dcdd7b99f9b237717fa0b12d75bbb89984cc1274be1471a465bac9c2",
                        fileName = "SmolLM2-360M-Instruct-Q4_K_M.gguf",
                        licenseUrl = "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF",
                    ),
                ),
            ),
        )
    }
}
