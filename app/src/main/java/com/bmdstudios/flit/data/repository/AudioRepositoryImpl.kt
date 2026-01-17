package com.bmdstudios.flit.data.repository

import android.content.Context
import com.bmdstudios.flit.domain.error.AppError
import com.bmdstudios.flit.domain.repository.AudioRepository
import com.bmdstudios.flit.utils.Constants
import com.bmdstudios.flit.utils.audio.AudioConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Implementation of AudioRepository.
 * Handles audio file operations.
 */
class AudioRepositoryImpl(
    private val context: Context,
    private val maxFileSizeBytes: Long = DEFAULT_MAX_FILE_SIZE_BYTES
) : AudioRepository {

    override fun createRecordingFile(): File {
        val cacheDir = context.cacheDir
        val fileName = "${Constants.RECORDING_FILE_PREFIX}${System.currentTimeMillis()}.${Constants.WAV_FILE_EXTENSION}"
        return File(cacheDir, fileName)
    }

    override suspend fun validateAudioFile(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        kotlin.runCatching {
            if (!file.exists()) {
                throw AppError.FileError.NotFoundError(file.name).toException()
            }

            if (!file.isFile) {
                throw AppError.FileError.InvalidPathError(file.absolutePath).toException()
            }

            if (file.length() == 0L) {
                throw AppError.FileError.ReadError(file.name).toException()
            }

            if (file.length() > maxFileSizeBytes) {
                throw AppError.FileError.SizeLimitExceededError(maxFileSizeBytes).toException()
            }

            // Basic WAV file validation (check for RIFF header)
            if (file.extension.equals(Constants.WAV_FILE_EXTENSION, ignoreCase = true)) {
                val header = ByteArray(4)
                file.inputStream().use { it.read(header) }
                val headerString = String(header)
                if (headerString != AudioConstants.WAV_RIFF_HEADER) {
                    throw AppError.FileError.InvalidPathError("Invalid WAV file format").toException()
                }
            }
        }.onFailure { e ->
            Timber.e(e, "Error validating audio file: ${file.name}")
            if (e !is com.bmdstudios.flit.domain.error.AppErrorException) {
                val error = com.bmdstudios.flit.domain.error.ErrorHandler.transform(e, "validateAudioFile")
                com.bmdstudios.flit.domain.error.ErrorHandler.logError(error)
                throw error.toException()
            }
        }
    }

    override suspend fun getAudioFileSize(file: File): Result<Long> = withContext(Dispatchers.IO) {
        kotlin.runCatching {
            if (!file.exists()) {
                throw AppError.FileError.NotFoundError(file.name).toException()
            }
            file.length()
        }.onFailure { e ->
            Timber.e(e, "Error getting audio file size: ${file.name}")
            if (e !is com.bmdstudios.flit.domain.error.AppErrorException) {
                val error = com.bmdstudios.flit.domain.error.ErrorHandler.transform(e, "getAudioFileSize")
                com.bmdstudios.flit.domain.error.ErrorHandler.logError(error)
                throw error.toException()
            }
        }
    }

    override suspend fun cleanupTemporaryFiles(): Result<Unit> = withContext(Dispatchers.IO) {
        kotlin.runCatching {
            val cacheDir = context.cacheDir
            val tempFiles = cacheDir.listFiles()?.filter {
                it.name.startsWith(Constants.RECORDING_FILE_PREFIX) && 
                (it.extension == Constants.WAV_FILE_EXTENSION || it.extension == Constants.PCM_FILE_EXTENSION)
            } ?: emptyList()

            var cleanedCount = 0
            for (file in tempFiles) {
                try {
                    if (file.delete()) {
                        cleanedCount++
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to delete temporary file: ${file.name}")
                }
            }

            if (cleanedCount > 0) {
                Timber.d("Cleaned up $cleanedCount temporary audio file(s)")
            }
        }.onFailure { e ->
            Timber.w(e, "Error cleaning up temporary files")
            if (e !is com.bmdstudios.flit.domain.error.AppErrorException) {
                val error = com.bmdstudios.flit.domain.error.ErrorHandler.transform(e, "cleanupTemporaryFiles")
                com.bmdstudios.flit.domain.error.ErrorHandler.logError(error)
                throw error.toException()
            }
        }
    }

    companion object {
        private const val DEFAULT_MAX_FILE_SIZE_BYTES = AudioConstants.MAX_FILE_SIZE_BYTES
    }
}
