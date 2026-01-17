package com.bmdstudios.flit.domain.repository

import java.io.File

/**
 * Repository interface for audio operations.
 * Abstracts audio recording and file management.
 */
interface AudioRepository {
    /**
     * Creates a temporary file for audio recording.
     */
    fun createRecordingFile(): File

    /**
     * Validates that a file is a valid audio file.
     */
    suspend fun validateAudioFile(file: File): Result<Unit>

    /**
     * Gets the size of an audio file.
     */
    suspend fun getAudioFileSize(file: File): Result<Long>

    /**
     * Cleans up temporary audio files.
     */
    suspend fun cleanupTemporaryFiles(): Result<Unit>
}
