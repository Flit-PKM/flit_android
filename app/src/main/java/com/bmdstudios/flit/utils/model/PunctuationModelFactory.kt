package com.bmdstudios.flit.utils.model

import com.bmdstudios.flit.config.AppConfig
import com.bmdstudios.flit.domain.error.ErrorHandler
import com.k2fsa.sherpa.onnx.OfflinePunctuation
import com.k2fsa.sherpa.onnx.OfflinePunctuationConfig
import com.k2fsa.sherpa.onnx.OfflinePunctuationModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating punctuation models.
 * Handles punctuation model initialization with graceful degradation if model is unavailable.
 */
@Singleton
class PunctuationModelFactory @Inject constructor(
    private val modelFileFinder: ModelFileFinder
) {

    /**
     * Creates a punctuation model if available.
     * Returns null if model is not found or initialization fails (graceful degradation).
     * 
     * @param punctuationModelDir Directory containing the punctuation model
     * @param appConfig Application configuration
     * @return Result containing OfflinePunctuation or null if unavailable
     */
    suspend fun createPunctuation(
        punctuationModelDir: File,
        appConfig: AppConfig
    ): Result<OfflinePunctuation?> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Checking for punctuation model in: ${punctuationModelDir.absolutePath}")

            if (!punctuationModelDir.exists() || !punctuationModelDir.isDirectory) {
                Timber.d("Punctuation model directory does not exist: ${punctuationModelDir.absolutePath}, skipping punctuation")
                return@withContext Result.success(null)
            }

            val allFiles = punctuationModelDir.listFiles()?.toList() ?: emptyList()
            val fileNames = allFiles.map { it.name }
            Timber.d("Files found in punctuation directory (${fileNames.size} total): ${fileNames.joinToString()}")

            // Check for ASR model files (encoder/decoder) - indicates wrong model type
            val hasEncoder = allFiles.any { it.isFile && it.name.contains(ModelConstants.ENCODER_PATTERN, ignoreCase = true) && it.extension.equals(ModelConstants.ONNX_EXTENSION, ignoreCase = true) }
            val hasDecoder = allFiles.any { it.isFile && it.name.contains(ModelConstants.DECODER_PATTERN, ignoreCase = true) && it.extension.equals(ModelConstants.ONNX_EXTENSION, ignoreCase = true) }
            val hasJoiner = allFiles.any { it.isFile && it.name.contains(ModelConstants.JOINER_PATTERN, ignoreCase = true) && it.extension.equals(ModelConstants.ONNX_EXTENSION, ignoreCase = true) }
            
            if (hasEncoder || hasDecoder || hasJoiner) {
                Timber.w("Detected ASR model files (encoder/decoder/joiner) in punctuation model directory. " +
                        "This appears to be an ASR model, not a punctuation model. " +
                        "Please use a proper CT transformer punctuation model (e.g., sherpa-onnx-punct-ct-transformer-zh-en-vocab272727-2024-04-12). " +
                        "Skipping punctuation initialization to prevent crash.")
                return@withContext Result.success(null)
            }

            // Look for proper punctuation model files
            // Priority: 1) model.onnx or model.int8.onnx (standard CT transformer format)
            //           2) files containing "ct" (CT transformer indicator)
            //           3) files containing "transformer" (fallback)
            //           4) any .onnx file (last resort)
            val modelFile = allFiles.find { file ->
                file.isFile && 
                (file.name.equals(ModelConstants.MODEL_ONNX, ignoreCase = true) || 
                 file.name.equals(ModelConstants.MODEL_INT8_ONNX, ignoreCase = true)) &&
                file.length() > 0
            } ?: allFiles.find { file ->
                file.isFile && 
                file.name.contains(ModelConstants.CT_TRANSFORMER_PATTERN, ignoreCase = true) && 
                file.extension.equals(ModelConstants.ONNX_EXTENSION, ignoreCase = true) &&
                file.length() > 0
            } ?: allFiles.find { file ->
                file.isFile && 
                file.name.contains(ModelConstants.TRANSFORMER_PATTERN, ignoreCase = true) && 
                file.extension.equals(ModelConstants.ONNX_EXTENSION, ignoreCase = true) &&
                file.length() > 0
            } ?: allFiles.find { file ->
                file.isFile && 
                file.extension.equals(ModelConstants.ONNX_EXTENSION, ignoreCase = true) &&
                file.length() > 0
            }

            if (modelFile == null) {
                Timber.d("No punctuation model file found in ${punctuationModelDir.absolutePath}. Available files: ${fileNames.joinToString()}")
                return@withContext Result.success(null)
            }

            val modelPath = modelFile.absolutePath
            Timber.i("Found punctuation model file: ${modelFile.name} (${modelFile.length()} bytes)")
            Timber.i("Initializing punctuation with model: $modelPath")

            val config = OfflinePunctuationConfig(
                model = OfflinePunctuationModelConfig(
                    ctTransformer = modelPath,
                    numThreads = appConfig.modelConfig.numThreads,
                    debug = appConfig.modelConfig.debug,
                    provider = appConfig.modelConfig.provider
                )
            )

            Result.success(OfflinePunctuation(config = config))
        } catch (e: Exception) {
            Timber.w(e, "Failed to initialize punctuation, continuing without punctuation")
            Result.success(null) // Graceful degradation
        }
    }
}
