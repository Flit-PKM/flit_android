package com.bmdstudios.flit.config

import android.content.Context
import com.bmdstudios.flit.ui.settings.ModelSize
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Loads model configuration from a JSON file in assets.
 * Handles missing or invalid JSON gracefully with fallbacks.
 */
object ModelConfigLoader {
    private const val CONFIG_FILE_NAME = "models_config.json"
    private const val FALLBACK_ASR_REPO_ID = "csukuangfj/sherpa-onnx-whisper-tiny.en"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Loads model configuration from assets.
     * @param context Android context to access assets
     * @return Map of ModelSize to LoadedModelSources, or null if loading failed
     */
    fun loadFromAssets(context: Context): Map<ModelSize, LoadedModelSources>? {
        return try {
            val inputStream = context.assets.open(CONFIG_FILE_NAME)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            parseJson(jsonString)
        } catch (e: Exception) {
            Timber.w(e, "Failed to load models config from assets, using fallback")
            null
        }
    }

    /**
     * Parses JSON string into a map of ModelSize to LoadedModelSources.
     */
    private fun parseJson(jsonString: String): Map<ModelSize, LoadedModelSources>? {
        return try {
            val config = json.decodeFromString<ModelsConfigJson>(jsonString)
            Timber.d("Parsing model configuration from JSON")

            // Log version if present
            config.version?.let {
                Timber.i("Config file version: $it")
            }

            val sourcesMap = mutableMapOf<ModelSize, LoadedModelSources>()

            // Parse each model size configuration
            config.light?.let { sizeConfig ->
                val loaded = sizeConfig.toLoadedModelSources()
                sourcesMap[ModelSize.LIGHT] = loaded
                logModelSources("LIGHT", loaded)
            }

            config.medium?.let { sizeConfig ->
                val loaded = sizeConfig.toLoadedModelSources()
                sourcesMap[ModelSize.MEDIUM] = loaded
                logModelSources("MEDIUM", loaded)
            }

            config.heavy?.let { sizeConfig ->
                val loaded = sizeConfig.toLoadedModelSources()
                sourcesMap[ModelSize.HEAVY] = loaded
                logModelSources("HEAVY", loaded)
            }

            // Config is valid if at least one model size is loaded
            if (sourcesMap.isEmpty()) {
                Timber.e("No model size configurations found in config file")
                return null
            }

            // Log summary
            Timber.i("Model configuration loaded successfully:")
            sourcesMap.forEach { (size, sources) ->
                val asrInfo = sources.asrSource?.let {
                    when (it) {
                        is ModelDownloadSource.HuggingFace -> it.repoId
                        is ModelDownloadSource.DirectUrl -> it.url.substringAfterLast("/")
                    }
                } ?: "not configured"
                val denoiserInfo = sources.denoiserSource?.let {
                    when (it) {
                        is ModelDownloadSource.HuggingFace -> it.repoId
                        is ModelDownloadSource.DirectUrl -> it.url.substringAfterLast("/")
                    }
                } ?: "not configured"
                val punctuationInfo = sources.punctuationSource?.let {
                    when (it) {
                        is ModelDownloadSource.HuggingFace -> it.repoId
                        is ModelDownloadSource.DirectUrl -> it.url.substringAfterLast("/")
                    }
                } ?: "not configured"
                Timber.i("  ${size.name}: ASR=$asrInfo, Denoiser=$denoiserInfo, Punctuation=$punctuationInfo")
            }
            if (config.version != null) {
                Timber.i("  Config version: ${config.version}")
            }
            Timber.i("Note: If these values don't match your local models_config.json, you may need to clean rebuild and reinstall the app (assets are bundled at build time)")

            sourcesMap
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse models config JSON")
            null
        }
    }

    /**
     * Logs model sources for a specific size.
     */
    private fun logModelSources(size: String, sources: LoadedModelSources) {
        sources.asrSource?.let {
            val sourceInfo = when (it) {
                is ModelDownloadSource.HuggingFace -> "HuggingFace: ${it.repoId}"
                is ModelDownloadSource.DirectUrl -> "DirectUrl: ${it.url}"
            }
            Timber.i("$size ASR model source loaded: $sourceInfo")
        } ?: Timber.w("$size ASR model source not configured")

        sources.denoiserSource?.let {
            val sourceInfo = when (it) {
                is ModelDownloadSource.HuggingFace -> "HuggingFace: ${it.repoId}"
                is ModelDownloadSource.DirectUrl -> "DirectUrl: ${it.url}"
            }
            Timber.i("$size Denoiser model source loaded: $sourceInfo")
        } ?: Timber.d("$size Denoiser model source not configured")

        sources.punctuationSource?.let {
            val sourceInfo = when (it) {
                is ModelDownloadSource.HuggingFace -> "HuggingFace: ${it.repoId}"
                is ModelDownloadSource.DirectUrl -> "DirectUrl: ${it.url}"
            }
            Timber.i("$size Punctuation model source loaded: $sourceInfo")
        } ?: Timber.d("$size Punctuation model source not configured")
    }

    /**
     * Gets the fallback ASR model source if JSON loading fails.
     */
    fun getFallbackAsrSource(): ModelDownloadSource {
        return ModelDownloadSource.HuggingFace(repoId = FALLBACK_ASR_REPO_ID)
    }
}
