package dk.schulz.quiettype.llama

import android.content.Context
import java.io.Closeable
import java.io.File

class LlamaAndroidBridge(
    context: Context,
) : Closeable {
    private val nativeLibDir: String = context.applicationInfo.nativeLibraryDir
    private var loadedModelPath: String? = null

    init {
        System.loadLibrary("quiettype-llama")
        nativeInit(nativeLibDir)
    }

    @Synchronized
    fun complete(
        modelFile: File,
        prompt: String,
        maxTokens: Int = 96,
        contextSize: Int = 1024,
        temperature: Float = 0.1f,
        topK: Int = 20,
        topP: Float = 0.9f,
    ): String {
        require(modelFile.isFile) { "Model file not found: ${modelFile.absolutePath}" }
        ensureLoaded(modelFile)
        return nativeComplete(prompt, maxTokens, contextSize, temperature, topK, topP)
    }

    @Synchronized
    override fun close() {
        loadedModelPath = null
        nativeUnloadModel()
        nativeShutdown()
    }

    @Synchronized
    private fun ensureLoaded(modelFile: File) {
        val targetPath = modelFile.absolutePath
        if (loadedModelPath == targetPath) return
        nativeUnloadModel()
        check(nativeLoadModel(targetPath) == 0) { "Could not load correction model: $targetPath" }
        loadedModelPath = targetPath
    }

    private external fun nativeInit(nativeLibDir: String)
    private external fun nativeLoadModel(modelPath: String): Int
    private external fun nativeComplete(
        prompt: String,
        maxTokens: Int,
        contextSize: Int,
        temperature: Float,
        topK: Int,
        topP: Float,
    ): String
    private external fun nativeUnloadModel()
    private external fun nativeShutdown()
}
