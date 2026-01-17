package com.bmdstudios.flit.utils.model

import com.bmdstudios.flit.config.AppConfig
import com.bmdstudios.flit.domain.error.AppError
import com.bmdstudios.flit.domain.error.ErrorHandler
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builder for SenseVoice model configurations.
 * Handles creation and validation of SenseVoice model configs.
 */
@Singleton
class SenseVoiceModelConfigBuilder @Inject constructor(
    private val fileMatcher: FileMatcher
) {

    /**
     * Creates configuration for a SenseVoice model.
     * 
     * @param modelDir Directory containing the SenseVoice model files
     * @param files List of file names in the model directory
     * @param appConfig Application configuration
     * @return Result containing OfflineModelConfig or error
     */
    suspend fun createSenseVoiceModelConfig(
        modelDir: File,
        files: List<String>,
        appConfig: AppConfig
    ): Result<OfflineModelConfig> = withContext(Dispatchers.IO) {
        try {
            // SenseVoice models use model.onnx or model.int8.onnx (prefer int8 for efficiency)
            val modelFile = fileMatcher.findModelFile(
                files = files,
                mustContain = "model",
                prioritizeInt8 = true,
                extension = ModelConstants.ONNX_EXTENSION
            )
            
            val tokensFile = fileMatcher.findModelFile(
                files = files,
                mustContain = ModelConstants.TOKENS_PATTERN,
                prioritizeInt8 = false,
                extension = ModelConstants.TXT_EXTENSION
            )

            val missingFiles = listOfNotNull(
                if (modelFile == null) "model.onnx or model.int8.onnx" else null,
                if (tokensFile == null) "tokens.txt" else null
            )

            if (missingFiles.isNotEmpty()) {
                val error = AppError.ModelError.ModelFileMissingError(missingFiles)
                ErrorHandler.logError(error)
                return@withContext Result.failure(error.toException())
            }

            Timber.d("Found SenseVoice model files - model: $modelFile, tokens: $tokensFile")

            val modelConfig = OfflineModelConfig(
                senseVoice = OfflineSenseVoiceModelConfig(
                    model = File(modelDir, modelFile).absolutePath,
                    // language can be left empty for multilingual auto-detection
                    // useInverseTextNormalization defaults to true
                ),
                tokens = File(modelDir, tokensFile).absolutePath,
                modelType = ModelConstants.MODEL_TYPE_SENSE_VOICE,
                numThreads = appConfig.modelConfig.numThreads,
                debug = appConfig.modelConfig.debug,
                provider = appConfig.modelConfig.provider,
            )

            // Verify files exist
            val missingPaths = listOf(
                modelConfig.senseVoice.model,
                modelConfig.tokens
            ).filterNot { File(it).exists() }

            if (missingPaths.isNotEmpty()) {
                val error = AppError.ModelError.ModelFileMissingError(missingPaths)
                ErrorHandler.logError(error)
                return@withContext Result.failure(error.toException())
            }

            Timber.i("SenseVoice model config created successfully")
            Result.success(modelConfig)
        } catch (e: Exception) {
            val error = ErrorHandler.transform(e, "createSenseVoiceModelConfig")
            ErrorHandler.logError(error)
            Result.failure(error.toException())
        }
    }
}
