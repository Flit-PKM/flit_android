package com.bmdstudios.flit.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmdstudios.flit.config.AppConfig
import com.bmdstudios.flit.config.ModelDownloadSource
import com.bmdstudios.flit.data.repository.SettingsRepository
import com.bmdstudios.flit.domain.error.AppError
import com.bmdstudios.flit.ui.settings.ModelSize
import com.bmdstudios.flit.domain.error.ErrorHandler
import com.bmdstudios.flit.domain.repository.ModelRepository
import com.bmdstudios.flit.domain.toAppError
import com.bmdstudios.flit.utils.Constants
import com.bmdstudios.flit.utils.HuggingFaceModelDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import java.io.File

/**
 * Represents a required model that should be present in the models directory.
 */
private data class RequiredModel(
    val directoryName: String,
    val isRequired: Boolean,
    val downloadType: ModelDownloadType
)

/**
 * Type of download for a model.
 */
private sealed class ModelDownloadType {
    data class HuggingFace(val repoId: String, val revision: String = "main") : ModelDownloadType()
    data class DirectUrl(val url: String, val filename: String?) : ModelDownloadType()
}

/**
 * ViewModel for managing model downloads.
 * Coordinates between the downloader and repository to manage model lifecycle.
 */
@HiltViewModel
class ModelDownloadViewModel @Inject constructor(
    private val downloader: HuggingFaceModelDownloader,
    private val modelRepository: ModelRepository,
    private val appConfig: AppConfig,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DownloadUiState>(DownloadUiState.Idle)
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    /**
     * Builds the list of required models from the ModelConfig.
     * Dynamically constructs the model list based on configured download sources and current ModelSize.
     */
    private suspend fun buildRequiredModelsFromConfig(modelSize: ModelSize): List<RequiredModel> {
        val modelConfig = appConfig.modelConfig
        val models = mutableListOf<RequiredModel>()

        // If ModelSize.NONE, skip ASR model (text-only mode)
        if (modelSize != ModelSize.NONE) {
            // Add ASR model (required)
            val asrSource = modelConfig.getAsrModelSourceForSize(modelSize)
            asrSource?.let { source ->
                val modelName = modelConfig.getAsrModelNameForSize(modelSize)
                if (modelName.isNotBlank()) {
                    models.add(
                        RequiredModel(
                            directoryName = modelName,
                            isRequired = true,
                            downloadType = convertDownloadSource(source)
                        )
                    )
                }
            }
        }

        // Add denoiser model (optional, shared across sizes)
        val denoiserSource = modelConfig.getDenoiserModelSourceForSize(modelSize)
            ?: modelConfig.denoiserModelSource // Fallback to legacy config
        denoiserSource?.let { source ->
            val modelName = when (source) {
                is ModelDownloadSource.HuggingFace -> source.getDirectoryName()
                is ModelDownloadSource.DirectUrl -> source.getDirectoryName()
            }
            models.add(
                RequiredModel(
                    directoryName = modelName,
                    isRequired = false,
                    downloadType = convertDownloadSource(source)
                )
            )
        }

        // Add punctuation model (optional, shared across sizes)
        val punctuationSource = modelConfig.getPunctuationModelSourceForSize(modelSize)
            ?: modelConfig.punctuationModelSource // Fallback to legacy config
        punctuationSource?.let { source ->
            val modelName = when (source) {
                is ModelDownloadSource.HuggingFace -> source.getDirectoryName()
                is ModelDownloadSource.DirectUrl -> source.getDirectoryName()
            }
            models.add(
                RequiredModel(
                    directoryName = modelName,
                    isRequired = false,
                    downloadType = convertDownloadSource(source)
                )
            )
        }

        return models
    }

    /**
     * Converts a ModelDownloadSource to the internal ModelDownloadType.
     */
    private fun convertDownloadSource(source: ModelDownloadSource): ModelDownloadType {
        return when (source) {
            is ModelDownloadSource.HuggingFace -> {
                ModelDownloadType.HuggingFace(source.repoId, source.revision)
            }
            is ModelDownloadSource.DirectUrl -> {
                ModelDownloadType.DirectUrl(source.url, source.filename)
            }
        }
    }

    /**
     * Downloads all required models.
     */
    fun downloadModels(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Timber.i("Starting model download process")

                // Get current model size from settings
                val modelSize = settingsRepository.getModelSize()
                val requiredModels = buildRequiredModelsFromConfig(modelSize)
                Timber.i("Required models checklist: ${requiredModels.map { it.directoryName }.joinToString()}")

                // Removed automatic cleanup to preserve all downloaded models
                // Users can switch between model sizes without re-downloading

                val requiredModelList = requiredModels.filter { it.isRequired }
                val optionalModelList = requiredModels.filter { !it.isRequired }

                // Download required models first
                downloadRequiredModels(requiredModelList)

                // Download optional models
                downloadOptionalModels(optionalModelList)

                Timber.i("State transition: -> Success")
                Timber.i("Model download process completed")
                _uiState.value = DownloadUiState.Success
            } catch (e: CancellationException) {
                // Rethrow cancellation exceptions - they're part of normal coroutine lifecycle
                Timber.d("Download job was cancelled")
                throw e
            } catch (e: Exception) {
                val error = ErrorHandler.transform(e, "downloadModels")
                ErrorHandler.logError(error)
                _uiState.value = DownloadUiState.Error(ErrorHandler.handleError(error))
            }
        }
    }

    /**
     * Downloads required models. Fails fast if any required model fails to download.
     */
    private suspend fun downloadRequiredModels(models: List<RequiredModel>) {
        for (model in models) {
            val modelExists = modelRepository.modelExists(model.directoryName).getOrElse { throwable ->
                val error = throwable.toAppError("modelExists")
                Timber.e(throwable, "Error checking model existence: ${model.directoryName}")
                _uiState.value = DownloadUiState.Error(ErrorHandler.handleError(error))
                return
            }

            if (!modelExists) {
                Timber.i("State transition: Idle -> Downloading (initializing)")
                _uiState.value = DownloadUiState.Downloading(0f, "Downloading ${model.directoryName}...")

                val modelDir = modelRepository.getModelDirectory(model.directoryName)
                val downloadResult = downloadModel(model, modelDir)

                downloadResult.fold(
                    onSuccess = {
                        Timber.i("Required model ${model.directoryName} downloaded successfully")
                    },
                    onFailure = { throwable ->
                        val error = throwable.toAppError("downloadModel")
                        Timber.e(throwable, "${model.directoryName} download failed")
                        _uiState.value = DownloadUiState.Error(ErrorHandler.handleError(error))
                        return
                    }
                )
            } else {
                Timber.i("Required model ${model.directoryName} already exists, skipping download")
            }
        }
    }

    /**
     * Downloads optional models. Continues even if some models fail to download.
     */
    private suspend fun downloadOptionalModels(models: List<RequiredModel>) {
        for (model in models) {
            val modelExists = modelRepository.modelExists(model.directoryName).getOrElse { error ->
                Timber.w(error, "Error checking optional model existence: ${model.directoryName}, skipping")
                continue
            }

            if (!modelExists) {
                Timber.i("Downloading optional model: ${model.directoryName}")
                val modelDir = modelRepository.getModelDirectory(model.directoryName)
                Timber.d("Optional model ${model.directoryName} will be saved to: ${modelDir.absolutePath}")
                downloadModel(model, modelDir).fold(
                    onSuccess = {
                        Timber.i("Optional model ${model.directoryName} downloaded successfully to ${modelDir.absolutePath}")
                        verifyModelDownload(model.directoryName, modelDir)
                    },
                    onFailure = { error ->
                        Timber.w(error, "Failed to download optional model ${model.directoryName} to ${modelDir.absolutePath}, continuing without it. Error: ${error.message}")
                    }
                )
            } else {
                Timber.i("Optional model ${model.directoryName} already exists, skipping download")
                val modelDir = modelRepository.getModelDirectory(model.directoryName)
                Timber.d("Optional model ${model.directoryName} found at: ${modelDir.absolutePath}")
            }
        }
    }

    /**
     * Verifies that a model was successfully downloaded.
     */
    private suspend fun verifyModelDownload(modelName: String, modelDir: File) {
        val verifyExists = modelRepository.modelExists(modelName).getOrElse { false }
        if (!verifyExists) {
            Timber.w("Optional model $modelName download reported success but model not found at ${modelDir.absolutePath}")
        } else {
            Timber.d("Optional model $modelName verified to exist after download")
        }
    }

    /**
     * Downloads a model based on its download type.
     */
    private suspend fun downloadModel(model: RequiredModel, modelDir: File): Result<Unit> {
        return when (val downloadType = model.downloadType) {
            is ModelDownloadType.HuggingFace -> {
                downloadHuggingFaceModel(downloadType.repoId, downloadType.revision, modelDir, model.directoryName)
            }
            is ModelDownloadType.DirectUrl -> {
                downloadDirectUrlModel(downloadType.url, downloadType.filename, modelDir, model.directoryName)
            }
        }
    }

    /**
     * Downloads a model from HuggingFace.
     */
    private suspend fun downloadHuggingFaceModel(
        repoId: String,
        revision: String,
        modelDir: File,
        modelName: String
    ): Result<Unit> {
        return try {
            modelDir.mkdirs()

            val fileProgress = mutableMapOf<String, Pair<Long, Long>>()
            val lastLoggedProgress = mutableListOf(-1f)

            Timber.i("Downloading $modelName from HuggingFace: $repoId (revision: $revision)")
            val result = downloader.download(
                repoId = repoId,
                revision = revision,
                localDir = modelDir,
                onProgress = { filename, downloaded, total ->
                    fileProgress[filename] = Pair(downloaded, total)
                    val progress = calculateProgress(fileProgress)
                    handleProgressUpdate(modelName, filename, downloaded, total, progress, lastLoggedProgress) { newProgress ->
                        lastLoggedProgress[0] = newProgress
                        _uiState.value = DownloadUiState.Downloading(
                            progress = progress,
                            currentFile = "$modelName: $filename"
                        )
                    }
                }
            )

            result.fold(
                onSuccess = {
                    Timber.i("$modelName downloaded successfully from HuggingFace")
                },
                onFailure = { error ->
                    Timber.e("Failed to download $modelName from HuggingFace")
                }
            )

            result
        } catch (e: CancellationException) {
            // Rethrow cancellation - don't treat as error
            throw e
        } catch (e: Exception) {
            val error = ErrorHandler.transform(e, "downloadHuggingFaceModel")
            ErrorHandler.logError(error)
            Result.failure(error.toException())
        }
    }

    /**
     * Calculates overall download progress from file progress map.
     */
    private fun calculateProgress(fileProgress: Map<String, Pair<Long, Long>>): Float {
        val totalDownloaded = fileProgress.values.sumOf { it.first }
        val totalSize = fileProgress.values.sumOf { it.second }
        val completedFiles = fileProgress.values.count { it.first == it.second && it.second > 0 }

        return when {
            totalSize > 0 -> (totalDownloaded.toFloat() / totalSize.toFloat()).coerceIn(0f, 1f)
            fileProgress.isNotEmpty() -> (completedFiles.toFloat() / fileProgress.size.toFloat()).coerceIn(0f, 1f)
            else -> 0f
        }
    }

    /**
     * Handles progress update logging and state updates.
     */
    private fun handleProgressUpdate(
        modelName: String,
        filename: String,
        downloaded: Long,
        total: Long,
        progress: Float,
        lastLoggedProgress: MutableList<Float>,
        updateState: (Float) -> Unit
    ) {
        val progressPercent = (progress * 100).toInt()
        val progressPercentFloat = progressPercent.toFloat()
        
        if ((progressPercentFloat != lastLoggedProgress[0] && progressPercent % Constants.PROGRESS_UPDATE_PERCENT_INTERVAL == 0) ||
            (downloaded == total && total > 0)
        ) {
            Timber.d("$modelName download progress: ${progressPercent}% | File: $filename | Downloaded: $downloaded/$total bytes")
            updateState(progressPercentFloat)

            if (downloaded == total && total > 0) {
                Timber.i("Completed downloading file: $filename ($total bytes)")
            }
        }
    }

    /**
     * Downloads a model from a direct URL.
     */
    private suspend fun downloadDirectUrlModel(
        url: String,
        filename: String?,
        modelDir: File,
        modelName: String
    ): Result<Unit> {
        return try {
            modelDir.mkdirs()
            // Derive filename from URL if not provided
            val actualFilename = filename ?: url.substringBefore("?").substringBefore("#").substringAfterLast("/")
                .takeIf { it.isNotBlank() && it != url } ?: com.bmdstudios.flit.utils.Constants.DEFAULT_MODEL_FILENAME
            val modelFile = File(modelDir, actualFilename)

            Timber.i("Downloading $modelName from direct URL: $url")
            val result = downloader.downloadFileFromUrl(
                url = url,
                targetFile = modelFile,
                onProgress = { downloaded, total ->
                    if (total > 0) {
                        val progress = (downloaded.toFloat() / total.toFloat() * 100).toInt()
                        if (progress % Constants.PROGRESS_UPDATE_PERCENT_INTERVAL_DIRECT == 0 || downloaded == total) {
                            Timber.d("$modelName download progress: $progress% ($downloaded/$total bytes)")
                        }
                    }
                }
            )

            result.fold(
                onSuccess = {
                    Timber.i("$modelName downloaded successfully from direct URL")
                },
                onFailure = { error ->
                    Timber.e("Failed to download $modelName from direct URL")
                }
            )

            result
        } catch (e: CancellationException) {
            // Rethrow cancellation - don't treat as error
            throw e
        } catch (e: Exception) {
            val error = ErrorHandler.transform(e, "downloadDirectUrlModel")
            ErrorHandler.logError(error)
            Result.failure(error.toException())
        }
    }
}
