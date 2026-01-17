package com.bmdstudios.flit.domain.repository

import java.io.File

/**
 * Repository interface for model management operations.
 * Abstracts model storage and retrieval operations.
 */
interface ModelRepository {
    /**
     * Checks if a model exists and is valid.
     */
    suspend fun modelExists(modelName: String): Result<Boolean>

    /**
     * Gets the directory for a specific model.
     */
    fun getModelDirectory(modelName: String): File

    /**
     * Validates that all required model files are present.
     */
    suspend fun validateModelFiles(modelName: String): Result<Unit>

    /**
     * Cleans up old model directories that are no longer needed.
     */
    suspend fun cleanupOldModels(requiredModelNames: Set<String>): Result<Unit>
}
