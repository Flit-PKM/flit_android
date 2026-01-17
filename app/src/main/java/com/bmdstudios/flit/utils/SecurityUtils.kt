package com.bmdstudios.flit.utils

import com.bmdstudios.flit.domain.error.AppError
import timber.log.Timber
import java.io.File

/**
 * Security utilities for input validation and path sanitization.
 */
object SecurityUtils {
    private const val MAX_FILE_NAME_LENGTH = 255
    private const val MAX_PATH_LENGTH = 4096
    private val ALLOWED_FILE_EXTENSIONS = setOf("wav", "pcm", "onnx", "txt")
    private val DANGEROUS_PATH_PATTERNS = listOf("..", "~", "/", "\\")

    /**
     * Validates and sanitizes a file path to prevent directory traversal attacks.
     */
    fun validateAndSanitizePath(path: String, baseDirectory: File): kotlin.Result<File> {
        return kotlin.runCatching {
            if (path.isBlank()) {
                throw AppError.FileError.InvalidPathError("Path cannot be blank").toException()
            }

            if (path.length > MAX_PATH_LENGTH) {
                throw AppError.FileError.InvalidPathError("Path exceeds maximum length").toException()
            }

            // Check for dangerous path patterns
            for (pattern in DANGEROUS_PATH_PATTERNS) {
                if (path.contains(pattern)) {
                    Timber.w("Dangerous path pattern detected: $pattern in path: $path")
                    throw AppError.FileError.InvalidPathError("Invalid path pattern detected").toException()
                }
            }

            val file = File(baseDirectory, path)
            val canonicalBase = baseDirectory.canonicalPath
            val canonicalFile = file.canonicalPath

            // Ensure the file is within the base directory
            if (!canonicalFile.startsWith(canonicalBase)) {
                Timber.w("Path traversal attempt detected: $path")
                throw AppError.FileError.InvalidPathError("Path traversal attempt detected").toException()
            }

            file
        }
    }

    /**
     * Validates a filename to ensure it's safe.
     */
    fun validateFileName(fileName: String): kotlin.Result<Unit> {
        return kotlin.runCatching {
            if (fileName.isBlank()) {
                throw AppError.FileError.InvalidPathError("Filename cannot be blank").toException()
            }

            if (fileName.length > MAX_FILE_NAME_LENGTH) {
                throw AppError.FileError.InvalidPathError("Filename exceeds maximum length").toException()
            }

            // Check for dangerous characters
            val dangerousChars = listOf("<", ">", ":", "\"", "|", "?", "*", "\u0000")
            for (char in dangerousChars) {
                if (fileName.contains(char)) {
                    throw AppError.FileError.InvalidPathError("Filename contains invalid characters").toException()
                }
            }

            // Check for dangerous path patterns
            for (pattern in DANGEROUS_PATH_PATTERNS) {
                if (fileName.contains(pattern)) {
                    throw AppError.FileError.InvalidPathError("Filename contains invalid patterns").toException()
                }
            }
        }
    }

    /**
     * Validates file extension.
     */
    fun validateFileExtension(fileName: String): kotlin.Result<Unit> {
        return kotlin.runCatching {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            if (extension.isBlank()) {
                throw AppError.FileError.InvalidPathError("File must have an extension").toException()
            }

            if (extension !in ALLOWED_FILE_EXTENSIONS) {
                throw AppError.FileError.InvalidPathError("File extension not allowed: $extension").toException()
            }
        }
    }

    /**
     * Validates file size against a maximum.
     */
    fun validateFileSize(file: File, maxSizeBytes: Long): kotlin.Result<Unit> {
        return kotlin.runCatching {
            if (!file.exists()) {
                throw AppError.FileError.NotFoundError(file.name).toException()
            }

            val fileSize = file.length()
            if (fileSize > maxSizeBytes) {
                throw AppError.FileError.SizeLimitExceededError(maxSizeBytes).toException()
            }
        }
    }

    /**
     * Sanitizes a user-facing error message to remove sensitive information.
     */
    fun sanitizeErrorMessage(message: String): String {
        // Remove file paths
        var sanitized = message.replace(Regex("(/[^\\s]+)"), "[path]")
        
        // Remove absolute paths
        sanitized = sanitized.replace(Regex("([A-Z]:\\\\[^\\s]+)"), "[path]")
        
        // Remove potential sensitive data patterns
        sanitized = sanitized.replace(Regex("Bearer [A-Za-z0-9]+"), "Bearer [token]")
        
        return sanitized
    }

    /**
     * Securely deletes a file.
     */
    fun secureDelete(file: File): kotlin.Result<Unit> {
        return kotlin.runCatching {
            if (file.exists()) {
                // Overwrite with zeros before deletion (basic security)
                if (file.isFile && file.length() > 0 && file.length() < 10 * 1024 * 1024) {
                    // Only for small files to avoid performance issues
                    file.writeBytes(ByteArray(file.length().toInt()))
                }
                
                if (!file.delete()) {
                    throw AppError.FileError.WriteError(file.name).toException()
                }
                Timber.d("Securely deleted file: ${file.name}")
            }
        }
    }
}
