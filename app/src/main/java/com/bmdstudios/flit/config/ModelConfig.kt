package com.bmdstudios.flit.config

import android.content.Context
import com.bmdstudios.flit.ui.settings.ModelSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

/**
 * Enumeration of supported ASR model types.
 * Makes it easier to switch between different model architectures.
 */
enum class AsrModelType {
    WHISPER,
    TRANSDUCER,
    SENSE_VOICE
}

/**
 * Represents the source from which a model can be downloaded.
 */
sealed class ModelDownloadSource {
    /**
     * Download from a HuggingFace repository.
     * @param repoId The repository ID (e.g., "csukuangfj/sherpa-onnx-whisper-tiny.en")
     * @param revision The git revision to download (default: "main")
     */
    data class HuggingFace(
        val repoId: String,
        val revision: String = "main"
    ) : ModelDownloadSource() {
        /**
         * Extracts the directory name from the repository ID.
         * For "csukuangfj/sherpa-onnx-whisper-tiny.en" returns "sherpa-onnx-whisper-tiny.en"
         */
        fun getDirectoryName(): String = repoId.substringAfterLast("/")
    }

    /**
     * Download from a direct URL.
     * @param url The direct download URL
     * @param filename Optional filename to save the downloaded file as. If not provided, derived from URL.
     */
    data class DirectUrl(
        val url: String,
        val filename: String? = null
    ) : ModelDownloadSource() {
        /**
         * Gets the filename, either the provided one or derived from the URL.
         */
        fun getResolvedFilename(): String {
            return filename ?: deriveFilenameFromUrl()
        }

        /**
         * Derives the filename from the URL by extracting the last path segment.
         * Handles query parameters and fragments by removing them.
         * Example: "https://example.com/file.onnx?token=abc" -> "file.onnx"
         */
        private fun deriveFilenameFromUrl(): String {
            val path = url.substringBefore("?").substringBefore("#")
            val extracted = path.substringAfterLast("/")
            return if (extracted.isNotBlank() && extracted != path) {
                extracted
            } else {
                // Fallback: use a default name if URL doesn't contain a clear filename
                "model.onnx"
            }
        }

        /**
         * Extracts the directory name from the filename (without extension).
         * For "model.onnx" returns "model"
         */
        fun getDirectoryName(): String {
            val actualFilename = getResolvedFilename()
            val nameWithoutExt = actualFilename.substringBeforeLast(".")
            return if (nameWithoutExt == actualFilename) actualFilename else nameWithoutExt
        }
    }
}

/**
 * Configuration for model management and paths.
 * Centralizes all model-related paths and settings.
 */
data class ModelConfig(
    val baseDirectory: File,
    val asrModelSource: ModelDownloadSource, // Kept for backward compatibility
    val asrModelType: AsrModelType? = null, // Auto-detect if null
    val denoiserModelSource: ModelDownloadSource? = null, // Kept for backward compatibility
    val punctuationModelSource: ModelDownloadSource? = null, // Kept for backward compatibility
    val numThreads: Int = DEFAULT_NUM_THREADS,
    val debug: Boolean = false,
    val provider: String = DEFAULT_PROVIDER,
    private val modelSourcesBySize: Map<ModelSize, LoadedModelSources> = emptyMap()
) {
    init {
        require(numThreads > 0) { "Number of threads must be positive" }
        require(provider.isNotBlank()) { "Provider must not be blank" }
    }

    /**
     * Gets the ASR model name, derived from the download source.
     * ASR model source is required, so this is always available.
     */
    val asrModelName: String
        get() = when (asrModelSource) {
            is ModelDownloadSource.HuggingFace -> asrModelSource.getDirectoryName()
            is ModelDownloadSource.DirectUrl -> asrModelSource.getDirectoryName()
        }

    /**
     * Gets the denoiser model name, derived from the download source if available.
     * Returns empty string if source is null (denoiser is optional).
     */
    val denoiserModelName: String
        get() = when (val source = denoiserModelSource) {
            is ModelDownloadSource.HuggingFace -> source.getDirectoryName()
            is ModelDownloadSource.DirectUrl -> source.getDirectoryName()
            null -> ""
        }

    /**
     * Gets the punctuation model name, derived from the download source if available.
     * Returns empty string if source is null (punctuation is optional).
     */
    val punctuationModelName: String
        get() = when (val source = punctuationModelSource) {
            is ModelDownloadSource.HuggingFace -> source.getDirectoryName()
            is ModelDownloadSource.DirectUrl -> source.getDirectoryName()
            null -> ""
        }

    val asrModelDirectory: File
        get() = File(baseDirectory, asrModelName)

    val denoiserModelDirectory: File
        get() = File(baseDirectory, denoiserModelName)

    val punctuationModelDirectory: File
        get() = File(baseDirectory, punctuationModelName)


    /**
     * Gets model sources for a specific model size.
     * Returns null if ModelSize.NONE (no ASR model needed for text-only mode).
     */
    fun getModelSourcesForSize(modelSize: ModelSize): LoadedModelSources? {
        if (modelSize == ModelSize.NONE) {
            return LoadedModelSources(
                asrSource = null,
                denoiserSource = null,
                punctuationSource = null
            )
        }
        return modelSourcesBySize[modelSize]
    }

    /**
     * Gets ASR model source for a specific model size.
     */
    fun getAsrModelSourceForSize(modelSize: ModelSize): ModelDownloadSource? {
        return getModelSourcesForSize(modelSize)?.asrSource
    }

    /**
     * Gets denoiser model source for a specific model size.
     */
    fun getDenoiserModelSourceForSize(modelSize: ModelSize): ModelDownloadSource? {
        return getModelSourcesForSize(modelSize)?.denoiserSource
    }

    /**
     * Gets punctuation model source for a specific model size.
     */
    fun getPunctuationModelSourceForSize(modelSize: ModelSize): ModelDownloadSource? {
        return getModelSourcesForSize(modelSize)?.punctuationSource
    }

    /**
     * Gets ASR model name for a specific model size.
     */
    fun getAsrModelNameForSize(modelSize: ModelSize): String {
        return getAsrModelSourceForSize(modelSize)?.let { source ->
            when (source) {
                is ModelDownloadSource.HuggingFace -> source.getDirectoryName()
                is ModelDownloadSource.DirectUrl -> source.getDirectoryName()
            }
        } ?: ""
    }

    /**
     * Gets ASR model directory for a specific model size.
     */
    fun getAsrModelDirectoryForSize(modelSize: ModelSize): File? {
        val modelName = getAsrModelNameForSize(modelSize)
        return if (modelName.isNotBlank()) {
            File(baseDirectory, modelName)
        } else {
            null
        }
    }

    companion object {
        private const val DEFAULT_NUM_THREADS = 1
        private const val DEFAULT_PROVIDER = "cpu"
        private const val MODELS_SUBDIRECTORY = "sherpa-onnx/models"

        /**
         * Creates a ModelConfig with model sources loaded from JSON config file.
         * Falls back to default ASR source if JSON loading fails.
         * 
         * @param context Android context to access assets
         * @return ModelConfig with loaded or fallback sources
         * @throws IllegalStateException if ASR source cannot be determined (should not happen due to fallback)
         */
        @JvmStatic
        fun createDefault(context: Context): ModelConfig {
            val baseDir = File(context.filesDir, MODELS_SUBDIRECTORY)
            
            // Load all model size configurations from JSON config
            val modelSourcesBySize = ModelConfigLoader.loadFromAssets(context)
            
            // Fallback: use single ASR source for backward compatibility or if loading failed
            val fallbackAsrSource = ModelConfigLoader.getFallbackAsrSource()
            val fallbackSources = LoadedModelSources(
                asrSource = fallbackAsrSource,
                denoiserSource = null,
                punctuationSource = null
            )
            
            // Build model sources map with fallbacks
            val sourcesMap = mutableMapOf<ModelSize, LoadedModelSources>()
            
            // Use loaded configs or fallbacks for each size
            ModelSize.values().filter { it != ModelSize.NONE }.forEach { size ->
                sourcesMap[size] = modelSourcesBySize?.get(size) 
                    ?: fallbackSources // Fallback to default if not configured
            }
            
            // For backward compatibility, use first available source or fallback
            val firstAvailableSource = modelSourcesBySize?.values?.firstOrNull()
            val defaultAsrSource = firstAvailableSource?.asrSource ?: fallbackAsrSource
            
            return ModelConfig(
                baseDirectory = baseDir,
                asrModelSource = defaultAsrSource,
                denoiserModelSource = firstAvailableSource?.denoiserSource,
                punctuationModelSource = firstAvailableSource?.punctuationSource,
                modelSourcesBySize = sourcesMap
            )
        }
    }
}

/**
 * Data class holding loaded model sources from JSON.
 */
data class LoadedModelSources(
    val asrSource: ModelDownloadSource?,
    val denoiserSource: ModelDownloadSource?,
    val punctuationSource: ModelDownloadSource?
)

/**
 * Serializable data classes for JSON parsing.
 */

/**
 * Root structure for models config JSON.
 */
@Serializable
data class ModelsConfigJson(
    val version: String? = null,
    val light: ModelSizeConfigJson? = null,
    val medium: ModelSizeConfigJson? = null,
    val heavy: ModelSizeConfigJson? = null
)

/**
 * Configuration for a single model size.
 */
@Serializable
data class ModelSizeConfigJson(
    val asr: ModelSourceConfigJson? = null,
    val denoiser: ModelSourceConfigJson? = null,
    val punctuation: ModelSourceConfigJson? = null
)

/**
 * Sealed class for model source configurations in JSON.
 */
@Serializable
sealed class ModelSourceConfigJson {
    @Serializable
    @SerialName("huggingface")
    data class HuggingFace(
        val type: String = "huggingface",
        val repoId: String,
        val revision: String = "main"
    ) : ModelSourceConfigJson()

    @Serializable
    @SerialName("directUrl")
    data class DirectUrl(
        val type: String = "directUrl",
        val url: String,
        val filename: String? = null
    ) : ModelSourceConfigJson()
}

/**
 * Extension functions to convert JSON config to domain model.
 */
internal fun ModelSourceConfigJson.toModelDownloadSource(): ModelDownloadSource? {
    return when (this) {
        is ModelSourceConfigJson.HuggingFace -> {
            if (repoId.isBlank()) {
                null
            } else {
                ModelDownloadSource.HuggingFace(repoId = repoId, revision = revision)
            }
        }
        is ModelSourceConfigJson.DirectUrl -> {
            if (url.isBlank()) {
                null
            } else {
                ModelDownloadSource.DirectUrl(url = url, filename = filename)
            }
        }
    }
}

internal fun ModelSizeConfigJson.toLoadedModelSources(): LoadedModelSources {
    return LoadedModelSources(
        asrSource = asr?.toModelDownloadSource(),
        denoiserSource = denoiser?.toModelDownloadSource(),
        punctuationSource = punctuation?.toModelDownloadSource()
    )
}
