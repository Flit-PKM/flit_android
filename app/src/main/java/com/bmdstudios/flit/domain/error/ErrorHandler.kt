package com.bmdstudios.flit.domain.error

import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Centralized error handling utilities.
 * Provides consistent error transformation and logging.
 */
object ErrorHandler {

    /**
     * Transforms a Throwable into an AppError with appropriate categorization.
     */
    fun transform(throwable: Throwable, context: String? = null): AppError {
        return when (throwable) {
            is AppError -> throwable
            is SocketTimeoutException -> AppError.NetworkError.TimeoutError(throwable)
            is UnknownHostException -> AppError.NetworkError.ConnectionError(throwable)
            is IOException -> {
                when {
                    throwable.message?.contains("timeout", ignoreCase = true) == true ->
                        AppError.NetworkError.TimeoutError(throwable)
                    throwable.message?.contains("connection", ignoreCase = true) == true ->
                        AppError.NetworkError.ConnectionError(throwable)
                    throwable.message?.contains("not found", ignoreCase = true) == true ->
                        AppError.FileError.NotFoundError(cause = throwable)
                    else -> AppError.FileError.ReadError(cause = throwable)
                }
            }
            is IllegalStateException -> {
                when {
                    throwable.message?.contains("model", ignoreCase = true) == true ->
                        AppError.ModelError.ModelInitializationError(cause = throwable)
                    throwable.message?.contains("recording", ignoreCase = true) == true ->
                        AppError.AudioError.RecordingError(cause = throwable)
                    else -> AppError.UnexpectedError(throwable, context)
                }
            }
            is IllegalArgumentException -> AppError.UnexpectedError(throwable, context)
            else -> AppError.UnexpectedError(throwable, context)
        }
    }

    /**
     * Logs an error with appropriate level based on error type.
     */
    fun logError(error: AppError, tag: String = "ErrorHandler") {
        when (error) {
            is AppError.NetworkError -> {
                Timber.w(error.cause, "[$tag] Network error: ${error.technicalMessage}")
            }
            is AppError.FileError -> {
                Timber.w(error.cause, "[$tag] File error: ${error.technicalMessage}")
            }
            is AppError.ModelError -> {
                Timber.e(error.cause, "[$tag] Model error: ${error.technicalMessage}")
            }
            is AppError.AudioError -> {
                Timber.w(error.cause, "[$tag] Audio error: ${error.technicalMessage}")
            }
            is AppError.TranscriptionError -> {
                Timber.e(error.cause, "[$tag] Transcription error: ${error.technicalMessage}")
            }
            is AppError.UnexpectedError -> {
                Timber.e(error.cause, "[$tag] Unexpected error: ${error.technicalMessage}")
            }
        }
    }

    /**
     * Handles an error by logging it and returning a user-friendly message.
     */
    fun handleError(error: AppError, tag: String = "ErrorHandler"): String {
        logError(error, tag)
        return error.userMessage
    }

    /**
     * Handles a Throwable by transforming it to AppError, logging, and returning user message.
     */
    fun handleThrowable(throwable: Throwable, context: String? = null, tag: String = "ErrorHandler"): String {
        val error = transform(throwable, context)
        return handleError(error, tag)
    }
}
