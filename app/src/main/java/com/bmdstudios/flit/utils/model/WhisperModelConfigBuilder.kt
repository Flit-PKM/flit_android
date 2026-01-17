package com.bmdstudios.flit.utils.model

import com.bmdstudios.flit.config.AppConfig
import com.bmdstudios.flit.domain.error.AppError
import com.bmdstudios.flit.domain.error.ErrorHandler
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builder for Whisper model configurations.
 * Handles creation and validation of Whisper model configs.
 */
@Singleton
class WhisperModelConfigBuilder @Inject constructor(
    private val fileMatcher: FileMatcher
) {

    /**
     * Creates configuration for a Whisper model.
     * 
     * @param modelDir Directory containing the Whisper model files
     * @param files List of file names in the model directory
     * @param appConfig Application configuration
     * @return Result containing OfflineModelConfig or error
     */
    suspend fun createWhisperModelConfig(
        modelDir: File,
        files: List<String>,
        appConfig: AppConfig
    ): Result<OfflineModelConfig> = withContext(Dispatchers.IO) {
        try {
            val encoderFile = fileMatcher.findModelFile(
                files = files,
                mustContain = ModelConstants.ENCODER_PATTERN,
                prioritizeInt8 = true
            )
            val decoderFile = fileMatcher.findModelFile(
                files = files,
                mustContain = ModelConstants.DECODER_PATTERN,
                prioritizeInt8 = true
            )
            val tokensFile = fileMatcher.findModelFile(
                files = files,
                mustContain = ModelConstants.TOKENS_PATTERN,
                prioritizeInt8 = false,
                extension = ModelConstants.TXT_EXTENSION
            )

            val missingFiles = listOfNotNull(
                if (encoderFile == null) "encoder" else null,
                if (decoderFile == null) "decoder" else null,
                if (tokensFile == null) "tokens" else null
            )

            if (missingFiles.isNotEmpty()) {
                val error = AppError.ModelError.ModelFileMissingError(missingFiles)
                ErrorHandler.logError(error)
                return@withContext Result.failure(error.toException())
            }

            Timber.d("Found Whisper model files - encoder: $encoderFile, decoder: $decoderFile, tokens: $tokensFile")

            val modelConfig = OfflineModelConfig(
                whisper = OfflineWhisperModelConfig(
                    encoder = File(modelDir, encoderFile).absolutePath,
                    decoder = File(modelDir, decoderFile).absolutePath,
                    language = ModelConstants.WHISPER_LANGUAGE,
                    task = ModelConstants.WHISPER_TASK
                ),
                tokens = File(modelDir, tokensFile).absolutePath,
                modelType = ModelConstants.MODEL_TYPE_WHISPER,
                numThreads = appConfig.modelConfig.numThreads,
                debug = appConfig.modelConfig.debug,
                provider = appConfig.modelConfig.provider,
            )

            // Verify all files exist
            val missingPaths = listOf(
                modelConfig.whisper.encoder,
                modelConfig.whisper.decoder,
                modelConfig.tokens
            ).filterNot { File(it).exists() }

            if (missingPaths.isNotEmpty()) {
                val error = AppError.ModelError.ModelFileMissingError(missingPaths)
                ErrorHandler.logError(error)
                return@withContext Result.failure(error.toException())
            }

            Timber.i("Whisper model config created successfully")
            Result.success(modelConfig)
        } catch (e: Exception) {
            val error = ErrorHandler.transform(e, "createWhisperModelConfig")
            ErrorHandler.logError(error)
            Result.failure(error.toException())
        }
    }
}
