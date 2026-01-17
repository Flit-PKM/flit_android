package com.bmdstudios.flit.utils.model

import com.bmdstudios.flit.config.AppConfig
import com.bmdstudios.flit.domain.error.AppError
import com.bmdstudios.flit.domain.error.ErrorHandler
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builder for transducer model configurations.
 * Handles creation and validation of transducer model configs.
 */
@Singleton
class TransducerModelConfigBuilder @Inject constructor(
    private val fileMatcher: FileMatcher
) {

    /**
     * Creates configuration for a transducer model.
     * 
     * @param modelDir Directory containing the transducer model files
     * @param files List of file names in the model directory
     * @param appConfig Application configuration
     * @return Result containing OfflineModelConfig or error
     */
    suspend fun createTransducerModelConfig(
        modelDir: File,
        files: List<String>,
        appConfig: AppConfig
    ): Result<OfflineModelConfig> = withContext(Dispatchers.IO) {
        try {
            val encoderFile = fileMatcher.findModelFile(
                files = files,
                mustContain = ModelConstants.ENCODER_PATTERN,
                prioritizeInt8 = true,
                extension = ModelConstants.ONNX_EXTENSION
            )
            val decoderFile = fileMatcher.findModelFile(
                files = files,
                mustContain = ModelConstants.DECODER_PATTERN,
                prioritizeInt8 = true,
                extension = ModelConstants.ONNX_EXTENSION
            )
            val joinerFile = fileMatcher.findModelFile(
                files = files,
                mustContain = ModelConstants.JOINER_PATTERN,
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
                if (encoderFile == null) "encoder" else null,
                if (decoderFile == null) "decoder" else null,
                if (joinerFile == null) "joiner" else null,
                if (tokensFile == null) "tokens" else null
            )

            if (missingFiles.isNotEmpty()) {
                val error = AppError.ModelError.ModelFileMissingError(missingFiles)
                ErrorHandler.logError(error)
                return@withContext Result.failure(error.toException())
            }

            Timber.d("Found transducer model files - encoder: $encoderFile, decoder: $decoderFile, joiner: $joinerFile, tokens: $tokensFile")

            val modelConfig = OfflineModelConfig(
                transducer = OfflineTransducerModelConfig(
                    encoder = File(modelDir, encoderFile).absolutePath,
                    decoder = File(modelDir, decoderFile).absolutePath,
                    joiner = File(modelDir, joinerFile).absolutePath,
                ),
                tokens = File(modelDir, tokensFile).absolutePath,
                modelType = ModelConstants.MODEL_TYPE_TRANSDUCER,
                numThreads = appConfig.modelConfig.numThreads,
                debug = appConfig.modelConfig.debug,
                provider = appConfig.modelConfig.provider,
            )

            // Verify files exist
            val missingPaths = listOf(
                modelConfig.transducer.encoder,
                modelConfig.transducer.decoder,
                modelConfig.transducer.joiner,
                modelConfig.tokens
            ).filterNot { File(it).exists() }

            if (missingPaths.isNotEmpty()) {
                val error = AppError.ModelError.ModelFileMissingError(missingPaths)
                ErrorHandler.logError(error)
                return@withContext Result.failure(error.toException())
            }

            Timber.i("Transducer model config created successfully")
            Result.success(modelConfig)
        } catch (e: Exception) {
            val error = ErrorHandler.transform(e, "createTransducerModelConfig")
            ErrorHandler.logError(error)
            Result.failure(error.toException())
        }
    }
}
