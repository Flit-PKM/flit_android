package com.bmdstudios.flit.utils.model

import com.bmdstudios.flit.config.AppConfig
import com.bmdstudios.flit.domain.error.ErrorHandler
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiser
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiserConfig
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiserModelConfig
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiserGtcrnModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating speech denoiser models.
 * Handles denoiser initialization with graceful degradation if model is unavailable.
 */
@Singleton
class DenoiserModelFactory @Inject constructor(
    private val modelFileFinder: ModelFileFinder
) {

    /**
     * Creates a speech denoiser if the model is available.
     * Returns null if model is not found or initialization fails (graceful degradation).
     * 
     * @param denoiserModelDir Directory containing the denoiser model
     * @param appConfig Application configuration
     * @return Result containing OfflineSpeechDenoiser or null if unavailable
     */
    suspend fun createDenoiser(
        denoiserModelDir: File,
        appConfig: AppConfig
    ): Result<OfflineSpeechDenoiser?> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Checking for denoiser model in: ${denoiserModelDir.absolutePath}")

            if (!denoiserModelDir.exists() || !denoiserModelDir.isDirectory) {
                Timber.d("Denoiser model directory does not exist: ${denoiserModelDir.absolutePath}, skipping denoising")
                return@withContext Result.success(null)
            }

            val allFiles = denoiserModelDir.listFiles()?.toList() ?: emptyList()
            val fileNames = allFiles.map { it.name }
            Timber.d("Files found in denoiser directory (${fileNames.size} total): ${fileNames.joinToString()}")

            // Look for model file with flexible matching
            // Priority: 1) files containing "gtcrn", 2) any .onnx file, 3) check subdirectories
            val modelFile = allFiles.find { file ->
                file.isFile && 
                file.name.contains(ModelConstants.GTCRN_PATTERN, ignoreCase = true) && 
                file.extension.equals(ModelConstants.ONNX_EXTENSION, ignoreCase = true) &&
                file.length() > 0
            } ?: allFiles.find { file ->
                file.isFile && 
                file.extension.equals(ModelConstants.ONNX_EXTENSION, ignoreCase = true) &&
                file.length() > 0
            } ?: modelFileFinder.findModelFileInDirectory(
                denoiserModelDir,
                listOf(ModelConstants.ONNX_EXTENSION),
                mustContain = null
            )

            if (modelFile == null) {
                Timber.d("No denoiser model file found in ${denoiserModelDir.absolutePath}. Available files: ${fileNames.joinToString()}")
                return@withContext Result.success(null)
            }

            val modelPath = if (modelFile.isFile) {
                modelFile.absolutePath
            } else {
                File(denoiserModelDir, modelFile.name).absolutePath
            }

            Timber.i("Found denoiser model file: ${modelFile.name} (${modelFile.length()} bytes)")
            Timber.i("Initializing denoiser with model: $modelPath")

            val config = OfflineSpeechDenoiserConfig(
                model = OfflineSpeechDenoiserModelConfig(
                    gtcrn = OfflineSpeechDenoiserGtcrnModelConfig(
                        model = modelPath
                    ),
                    numThreads = appConfig.modelConfig.numThreads,
                    debug = appConfig.modelConfig.debug,
                    provider = appConfig.modelConfig.provider
                )
            )

            Result.success(OfflineSpeechDenoiser(config = config))
        } catch (e: Exception) {
            Timber.w(e, "Failed to initialize denoiser, continuing without denoising")
            Result.success(null) // Graceful degradation
        }
    }
}
