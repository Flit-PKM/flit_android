package com.bmdstudios.flit.domain.error

/**
 * Exception wrapper for AppError to enable use with Kotlin's Result type.
 */
class AppErrorException(val appError: AppError) : Exception(appError.userMessage, appError.cause) {
    override val message: String
        get() = appError.userMessage
}

/**
 * Sealed hierarchy representing all application errors.
 * Provides type-safe error handling with user-friendly messages.
 */
sealed class AppError(
    val userMessage: String,
    val technicalMessage: String? = null,
    open val cause: Throwable? = null
) {
    /**
     * Converts this AppError to an exception for use with Result.
     */
    fun toException(): AppErrorException = AppErrorException(this)
    /**
     * Network-related errors.
     */
    sealed class NetworkError(
        userMessage: String,
        technicalMessage: String? = null,
        cause: Throwable? = null
    ) : AppError(userMessage, technicalMessage, cause) {
        data class ConnectionError(
            override val cause: Throwable? = null
        ) : NetworkError(
            userMessage = "Unable to connect to the server. Please check your internet connection.",
            technicalMessage = "Network connection failed",
            cause = cause
        )

        data class TimeoutError(
            override val cause: Throwable? = null
        ) : NetworkError(
            userMessage = "Request timed out. Please try again.",
            technicalMessage = "Network request timeout",
            cause = cause
        )

        data class HttpError(
            val statusCode: Int,
            override val cause: Throwable? = null
        ) : NetworkError(
            userMessage = when {
                statusCode in 400..499 -> "Invalid request. Please try again."
                statusCode in 500..599 -> "Server error. Please try again later."
                else -> "Network error occurred. Please try again."
            },
            technicalMessage = "HTTP error: $statusCode",
            cause = cause
        )

        data class DownloadError(
            val fileName: String? = null,
            override val cause: Throwable? = null
        ) : NetworkError(
            userMessage = "Failed to download ${fileName ?: "file"}. Please check your connection and try again.",
            technicalMessage = "Download failed${fileName?.let { " for file: $it" } ?: ""}",
            cause = cause
        )
    }

    /**
     * File system related errors.
     */
    sealed class FileError(
        userMessage: String,
        technicalMessage: String? = null,
        cause: Throwable? = null
    ) : AppError(userMessage, technicalMessage, cause) {
        data class NotFoundError(
            val fileName: String? = null,
            override val cause: Throwable? = null
        ) : FileError(
            userMessage = "File not found${fileName?.let { ": $it" } ?: ""}.",
            technicalMessage = "File not found${fileName?.let { ": $it" } ?: ""}",
            cause = cause
        )

        data class ReadError(
            val fileName: String? = null,
            override val cause: Throwable? = null
        ) : FileError(
            userMessage = "Unable to read file${fileName?.let { ": $it" } ?: ""}.",
            technicalMessage = "File read error${fileName?.let { " for: $it" } ?: ""}",
            cause = cause
        )

        data class WriteError(
            val fileName: String? = null,
            override val cause: Throwable? = null
        ) : FileError(
            userMessage = "Unable to write file${fileName?.let { ": $it" } ?: ""}.",
            technicalMessage = "File write error${fileName?.let { " for: $it" } ?: ""}",
            cause = cause
        )

        data class InvalidPathError(
            val path: String? = null,
            override val cause: Throwable? = null
        ) : FileError(
            userMessage = "Invalid file path.",
            technicalMessage = "Invalid file path${path?.let { ": $path" } ?: ""}",
            cause = cause
        )

        data class SizeLimitExceededError(
            val maxSize: Long,
            override val cause: Throwable? = null
        ) : FileError(
            userMessage = "File size exceeds the maximum allowed limit.",
            technicalMessage = "File size limit exceeded: max=$maxSize bytes",
            cause = cause
        )
    }

    /**
     * Model-related errors.
     */
    sealed class ModelError(
        userMessage: String,
        technicalMessage: String? = null,
        cause: Throwable? = null
    ) : AppError(userMessage, technicalMessage, cause) {
        data class ModelNotFoundError(
            val modelName: String? = null,
            override val cause: Throwable? = null
        ) : ModelError(
            userMessage = "Model not found${modelName?.let { ": $it" } ?: ""}. Please download the required models.",
            technicalMessage = "Model not found${modelName?.let { ": $it" } ?: ""}",
            cause = cause
        )

        data class ModelInitializationError(
            val modelName: String? = null,
            override val cause: Throwable? = null
        ) : ModelError(
            userMessage = "Failed to initialize model${modelName?.let { ": $it" } ?: ""}.",
            technicalMessage = "Model initialization failed${modelName?.let { " for: $it" } ?: ""}",
            cause = cause
        )

        data class ModelFileMissingError(
            val missingFiles: List<String>,
            override val cause: Throwable? = null
        ) : ModelError(
            userMessage = "Model files are missing. Please download the required models.",
            technicalMessage = "Missing model files: ${missingFiles.joinToString()}",
            cause = cause
        )

        data class InvalidModelConfigError(
            val reason: String? = null,
            override val cause: Throwable? = null
        ) : ModelError(
            userMessage = "Invalid model configuration.",
            technicalMessage = "Invalid model configuration${reason?.let { ": $reason" } ?: ""}",
            cause = cause
        )
    }

    /**
     * Audio recording related errors.
     */
    sealed class AudioError(
        userMessage: String,
        technicalMessage: String? = null,
        cause: Throwable? = null
    ) : AppError(userMessage, technicalMessage, cause) {
        data class RecordingError(
            val reason: String? = null,
            override val cause: Throwable? = null
        ) : AudioError(
            userMessage = "Failed to record audio. Please try again.",
            technicalMessage = "Audio recording failed${reason?.let { ": $reason" } ?: ""}",
            cause = cause
        )

        data class PermissionDeniedError(
            override val cause: Throwable? = null
        ) : AudioError(
            userMessage = "Microphone permission is required to record audio.",
            technicalMessage = "Audio recording permission denied",
            cause = cause
        )

        data class InvalidAudioFormatError(
            override val cause: Throwable? = null
        ) : AudioError(
            userMessage = "Invalid audio format.",
            technicalMessage = "Invalid audio format",
            cause = cause
        )
    }

    /**
     * Transcription related errors.
     */
    sealed class TranscriptionError(
        userMessage: String,
        technicalMessage: String? = null,
        cause: Throwable? = null
    ) : AppError(userMessage, technicalMessage, cause) {
        data class TranscriptionFailedError(
            override val cause: Throwable? = null
        ) : TranscriptionError(
            userMessage = "Transcription failed. Please try again.",
            technicalMessage = "Transcription processing failed",
            cause = cause
        )

        data class ModelNotReadyError(
            override val cause: Throwable? = null
        ) : TranscriptionError(
            userMessage = "Models are not ready yet. Please wait for download to complete.",
            technicalMessage = "Transcription model not ready",
            cause = cause
        )
    }

    /**
     * Generic unexpected errors.
     */
    data class UnexpectedError(
        override val cause: Throwable? = null,
        val context: String? = null
    ) : AppError(
        userMessage = "An unexpected error occurred. Please try again.",
        technicalMessage = "Unexpected error${context?.let { " in: $context" } ?: ""}",
        cause = cause
    )
}
