package com.bmdstudios.flit.utils.model

import com.bmdstudios.flit.domain.error.AppError
import com.bmdstudios.flit.domain.error.ErrorHandler
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility for finding model files in directories.
 * Consolidates file finding logic to eliminate duplication.
 */
@Singleton
class ModelFileFinder @Inject constructor() {

    /**
     * Finds a model file matching the given patterns.
     * 
     * @param files List of file names to search
     * @param patterns List of patterns to match (in priority order)
     * @param extension File extension to match (default: ".onnx")
     * @param mustContain Optional string that must be contained in the filename
     * @return Matching file name, or null if not found
     */
    fun findModelFile(
        files: List<String>,
        patterns: List<String>,
        extension: String = ModelConstants.ONNX_EXTENSION,
        mustContain: String? = null
    ): String? {
        for (pattern in patterns) {
            val found = files.find { fileName ->
                val matchesPattern = fileName == pattern ||
                    (fileName.contains(pattern, ignoreCase = true) && fileName.endsWith(extension))
                val matchesMustContain = mustContain == null || fileName.contains(mustContain, ignoreCase = true)
                matchesPattern && matchesMustContain
            }
            if (found != null) return found
        }
        return null
    }

    /**
     * Finds a model file in a directory, checking both root and subdirectories.
     * 
     * @param directory Directory to search
     * @param patterns List of patterns to match (in priority order)
     * @param extension File extension to match (default: ".onnx")
     * @param mustContain Optional string that must be contained in the filename
     * @return Matching File, or null if not found
     */
    fun findModelFileInDirectory(
        directory: File,
        patterns: List<String>,
        extension: String = ModelConstants.ONNX_EXTENSION,
        mustContain: String? = null
    ): File? {
        if (!directory.exists() || !directory.isDirectory) {
            return null
        }

        val allFiles = directory.listFiles()?.toList() ?: emptyList()
        val fileNames = allFiles.map { it.name }

        // First try to find in root directory
        val foundFileName = findModelFile(fileNames, patterns, extension, mustContain)
        if (foundFileName != null) {
            val file = File(directory, foundFileName)
            if (file.exists() && file.isFile && file.length() > 0) {
                return file
            }
        }

        // If not found, check subdirectories
        val subDirs = allFiles.filter { it.isDirectory }
        for (subDir in subDirs) {
            val subFiles = subDir.listFiles()?.toList() ?: emptyList()
            val subFileNames = subFiles.map { it.name }
            val foundSubFileName = findModelFile(subFileNames, patterns, extension, mustContain)
            if (foundSubFileName != null) {
                val file = File(subDir, foundSubFileName)
                if (file.exists() && file.isFile && file.length() > 0) {
                    return file
                }
            }
        }

        return null
    }
}
