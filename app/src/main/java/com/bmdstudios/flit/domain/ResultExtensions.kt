package com.bmdstudios.flit.domain

import com.bmdstudios.flit.domain.error.AppErrorException

/**
 * Extension functions for Kotlin's Result type to provide fold-like functionality.
 */

/**
 * Folds the result into a single value.
 * Parameter order is (onFailure, onSuccess), i.e. error-first—opposite to Kotlin stdlib
 * [Result.fold], which uses (onSuccess, onFailure). Use named parameters at call sites to avoid confusion.
 */
inline fun <T, R> Result<T>.fold(
    onFailure: (Throwable) -> R,
    onSuccess: (T) -> R
): R = when {
    isSuccess -> onSuccess(getOrNull()!!)
    else -> onFailure(exceptionOrNull()!!)
}

/**
 * Gets the AppError from an AppErrorException if present.
 */
fun Result<*>.getAppErrorOrNull(): com.bmdstudios.flit.domain.error.AppError? {
    return exceptionOrNull()?.let { exception ->
        if (exception is AppErrorException) {
            exception.appError
        } else {
            null
        }
    }
}

/**
 * Converts a Throwable to AppError.
 * If the throwable is an AppErrorException, extracts the AppError.
 * Otherwise, transforms it using ErrorHandler.
 */
fun Throwable.toAppError(context: String = ""): com.bmdstudios.flit.domain.error.AppError {
    return if (this is AppErrorException) {
        this.appError
    } else {
        com.bmdstudios.flit.domain.error.ErrorHandler.transform(this, context)
    }
}
