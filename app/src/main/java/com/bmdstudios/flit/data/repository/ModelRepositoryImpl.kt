package com.bmdstudios.flit.data.repository

import com.bmdstudios.flit.config.ModelConfig
import com.bmdstudios.flit.domain.error.AppError
import com.bmdstudios.flit.domain.repository.ModelRepository
import com.bmdstudios.flit.domain.service.ModelTypeDetector
import com.bmdstudios.flit.utils.model.ModelConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import java.io.File

/**
 * Implementation of ModelRepository.
 * Handles model file system operations.
 */
class ModelRepositoryImpl @Inject constructor(
    private val modelConfig: ModelConfig,
    private val modelTypeDetector: ModelTypeDetector
) : ModelRepository {

    override suspend fun modelExists(modelName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        kotlin.runCatching {
            val modelDir = File(modelConfig.baseDirectory, modelName)
            if (!modelDir.exists() || !modelDir.isDirectory) {
                return@runCatching false
            }

            val files = modelDir.listFiles() ?: return@runCatching false
            if (files.isEmpty()) {
                return@runCatching false
            }

            // Check for .onnx files (model files)
            files.any { it.isFile && it.extension == ModelConstants.ONNX_EXTENSION && it.length() > 0 }
        }.onFailure { e ->
            Timber.e(e, "Error checking model existence: $modelName")
            val error = com.bmdstudios.flit.domain.error.ErrorHandler.transform(e, "modelExists")
            com.bmdstudios.flit.domain.error.ErrorHandler.logError(error)
            throw error.toException()
        }
    }

    override fun getModelDirectory(modelName: String): File {
        return File(modelConfig.baseDirectory, modelName)
    }

    override suspend fun validateModelFiles(modelName: String): Result<Unit> = withContext(Dispatchers.IO) {
        kotlin.runCatching {
            val modelDir = File(modelConfig.baseDirectory, modelName)
            Timber.d("Validating model files for: $modelName at ${modelDir.absolutePath}")
            
            if (!modelDir.exists() || !modelDir.isDirectory) {
                Timber.d("Model directory does not exist: ${modelDir.absolutePath}")
                throw AppError.ModelError.ModelNotFoundError(modelName).toException()
            }

            val allItems = modelDir.listFiles()?.toList() ?: emptyList()
            val files = allItems.filter { it.isFile }
            val subDirs = allItems.filter { it.isDirectory }
            val fileNames = files.map { it.name }
            
            Timber.d("Found ${files.size} files and ${subDirs.size} subdirectories in $modelName")
            if (files.isEmpty() && subDirs.isEmpty()) {
                Timber.d("Model directory is empty: ${modelDir.absolutePath}")
                throw AppError.ModelError.ModelNotFoundError(modelName).toException()
            }

            // Check for required model files based on model type
            val isOptionalModel = modelName != modelConfig.asrModelName
            val requiredFiles = when {
                modelName == modelConfig.asrModelName -> {
                    // Detect if it's a Whisper model or transducer model
                    val isWhisperModel = modelConfig.asrModelType?.let { it == com.bmdstudios.flit.config.AsrModelType.WHISPER }
                        ?: modelTypeDetector.detectWhisperModel(fileNames)
                    if (isWhisperModel) {
                        listOf(ModelConstants.ENCODER_PATTERN, ModelConstants.DECODER_PATTERN, ModelConstants.TOKENS_PATTERN) // Whisper models don't have joiner
                    } else {
                        listOf(ModelConstants.ENCODER_PATTERN, ModelConstants.DECODER_PATTERN, ModelConstants.JOINER_PATTERN, ModelConstants.TOKENS_PATTERN) // Transducer models
                    }
                }
                else -> listOf(ModelConstants.ONNX_EXTENSION) // For optional models, just check for .onnx files
            }

            val missingFiles = requiredFiles.filter { required ->
                isFileMissing(required, files, subDirs, isOptionalModel)
            }

            if (missingFiles.isNotEmpty()) {
                Timber.w("Missing required files for model $modelName: ${missingFiles.joinToString()}")
                if (isOptionalModel) {
                    Timber.d("This is an optional model, validation failure is non-critical")
                }
                throw AppError.ModelError.ModelFileMissingError(missingFiles).toException()
            }
            
            Timber.d("Model validation successful for: $modelName")
        }.onFailure { e ->
            Timber.e(e, "Error validating model files: $modelName")
            if (e !is com.bmdstudios.flit.domain.error.AppErrorException) {
                val error = com.bmdstudios.flit.domain.error.ErrorHandler.transform(e, "validateModelFiles")
                com.bmdstudios.flit.domain.error.ErrorHandler.logError(error)
                throw error.toException()
            }
        }
    }

    /**
     * Checks if a required file is missing from the model directory.
     */
    private fun isFileMissing(
        required: String,
        files: List<File>,
        subDirs: List<File>,
        isOptionalModel: Boolean
    ): Boolean {
        return when (required) {
            ModelConstants.TOKENS_PATTERN -> isTokensFileMissing(files, subDirs, isOptionalModel)
            ModelConstants.ONNX_EXTENSION -> isOnnxFileMissing(files, subDirs, isOptionalModel)
            else -> isModelFileMissing(required, files, subDirs, isOptionalModel)
        }
    }

    /**
     * Checks if tokens file is missing.
     */
    private fun isTokensFileMissing(
        files: List<File>,
        subDirs: List<File>,
        isOptionalModel: Boolean
    ): Boolean {
        val hasTokens = files.any { 
            it.name.contains(ModelConstants.TOKENS_PATTERN, ignoreCase = true) && 
            it.extension.equals(ModelConstants.TXT_EXTENSION, ignoreCase = true) 
        }
        if (!hasTokens && isOptionalModel) {
            return !subDirs.any { subDir ->
                subDir.listFiles()?.any { f ->
                    f.isFile && 
                    f.name.contains(ModelConstants.TOKENS_PATTERN, ignoreCase = true) && 
                    f.extension.equals(ModelConstants.TXT_EXTENSION, ignoreCase = true)
                } == true
            }
        }
        return !hasTokens
    }

    /**
     * Checks if ONNX file is missing.
     */
    private fun isOnnxFileMissing(
        files: List<File>,
        subDirs: List<File>,
        isOptionalModel: Boolean
    ): Boolean {
        val hasOnnxInRoot = files.any { 
            it.extension.equals(ModelConstants.ONNX_EXTENSION, ignoreCase = true) && 
            it.length() > 0 
        }
        if (hasOnnxInRoot) {
            return false
        }
        if (isOptionalModel) {
            val hasOnnxInSubdir = subDirs.any { subDir ->
                subDir.listFiles()?.any { f ->
                    f.isFile && 
                    f.extension.equals(ModelConstants.ONNX_EXTENSION, ignoreCase = true) && 
                    f.length() > 0
                } == true
            }
            return !hasOnnxInSubdir
        }
        return true
    }

    /**
     * Checks if a specific model file (encoder, decoder, joiner) is missing.
     */
    private fun isModelFileMissing(
        required: String,
        files: List<File>,
        subDirs: List<File>,
        isOptionalModel: Boolean
    ): Boolean {
        val hasFile = files.any { 
            it.name.contains(required, ignoreCase = true) && 
            it.extension.equals(ModelConstants.ONNX_EXTENSION, ignoreCase = true) && 
            it.length() > 0 
        }
        if (!hasFile && isOptionalModel) {
            return !subDirs.any { subDir ->
                subDir.listFiles()?.any { f ->
                    f.isFile && 
                    f.name.contains(required, ignoreCase = true) && 
                    f.extension.equals(ModelConstants.ONNX_EXTENSION, ignoreCase = true) && 
                    f.length() > 0
                } == true
            }
        }
        return !hasFile
    }


    override suspend fun cleanupOldModels(requiredModelNames: Set<String>): Result<Unit> = withContext(Dispatchers.IO) {
        kotlin.runCatching {
            val baseDir = modelConfig.baseDirectory
            if (!baseDir.exists() || !baseDir.isDirectory) {
                return@runCatching Unit
            }

            val existingDirs = baseDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
            var cleanedCount = 0

            for (existingDir in existingDirs) {
                if (existingDir.name !in requiredModelNames) {
                    try {
                        Timber.i("Removing old model directory: ${existingDir.name}")
                        existingDir.deleteRecursively()
                        cleanedCount++
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to remove old model directory: ${existingDir.name}")
                    }
                }
            }

            if (cleanedCount > 0) {
                Timber.i("Cleaned up $cleanedCount old model directory(ies)")
            }
        }.onFailure { e ->
            Timber.w(e, "Exception during cleanup of old models")
            val error = com.bmdstudios.flit.domain.error.ErrorHandler.transform(e, "cleanupOldModels")
            com.bmdstudios.flit.domain.error.ErrorHandler.logError(error)
            throw error.toException()
        }
    }
}
