package com.bmdstudios.flit.domain.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Unit tests for [ErrorHandler.transform].
 */
class ErrorHandlerTest {

    @Test
    fun transform_passesThroughAppErrorException() {
        val appError = AppError.FileError.NotFoundError(fileName = "test.txt")
        val result = ErrorHandler.transform(appError.toException())
        assertEquals(appError, result)
    }

    @Test
    fun transform_ConnectException_returnsConnectionError() {
        val cause = ConnectException("Connection refused")
        val result = ErrorHandler.transform(cause)
        assertTrue(result is AppError.NetworkError.ConnectionError)
        assertEquals(cause, (result as AppError.NetworkError.ConnectionError).cause)
    }

    @Test
    fun transform_SocketTimeoutException_returnsTimeoutError() {
        val cause = SocketTimeoutException("Read timed out")
        val result = ErrorHandler.transform(cause)
        assertTrue(result is AppError.NetworkError.TimeoutError)
        assertEquals(cause, (result as AppError.NetworkError.TimeoutError).cause)
    }

    @Test
    fun transform_UnknownHostException_returnsConnectionError() {
        val cause = UnknownHostException("Unable to resolve host")
        val result = ErrorHandler.transform(cause)
        assertTrue(result is AppError.NetworkError.ConnectionError)
        assertEquals(cause, (result as AppError.NetworkError.ConnectionError).cause)
    }

    @Test
    fun transform_IOException_withTimeoutMessage_returnsTimeoutError() {
        val cause = IOException("Connection timeout")
        val result = ErrorHandler.transform(cause)
        assertTrue(result is AppError.NetworkError.TimeoutError)
    }

    @Test
    fun transform_IOException_withConnectionMessage_returnsConnectionError() {
        val cause = IOException("Connection reset")
        val result = ErrorHandler.transform(cause)
        assertTrue(result is AppError.NetworkError.ConnectionError)
    }

    @Test
    fun transform_IOException_withNotFoundMessage_returnsNotFoundError() {
        val cause = IOException("File not found")
        val result = ErrorHandler.transform(cause)
        assertTrue(result is AppError.FileError.NotFoundError)
    }

    @Test
    fun transform_IOException_generic_returnsReadError() {
        val cause = IOException("Permission denied")
        val result = ErrorHandler.transform(cause)
        assertTrue(result is AppError.FileError.ReadError)
    }

    @Test
    fun transform_IllegalStateException_withModelMessage_returnsModelInitializationError() {
        val cause = IllegalStateException("Model not loaded")
        val result = ErrorHandler.transform(cause)
        assertTrue(result is AppError.ModelError.ModelInitializationError)
    }

    @Test
    fun transform_IllegalStateException_withRecordingMessage_returnsRecordingError() {
        val cause = IllegalStateException("Recording already started")
        val result = ErrorHandler.transform(cause)
        assertTrue(result is AppError.AudioError.RecordingError)
    }

    @Test
    fun transform_IllegalStateException_generic_returnsUnexpectedError() {
        val cause = IllegalStateException("Something else")
        val result = ErrorHandler.transform(cause, "TestContext")
        assertTrue(result is AppError.UnexpectedError)
        assertEquals("TestContext", (result as AppError.UnexpectedError).context)
    }

    @Test
    fun transform_IllegalArgumentException_returnsUnexpectedError() {
        val cause = IllegalArgumentException("Invalid argument")
        val result = ErrorHandler.transform(cause)
        assertTrue(result is AppError.UnexpectedError)
    }

    @Test
    fun transform_otherThrowable_returnsUnexpectedError() {
        val cause = RuntimeException("Unknown")
        val result = ErrorHandler.transform(cause)
        assertTrue(result is AppError.UnexpectedError)
        assertEquals(cause, (result as AppError.UnexpectedError).cause)
    }

    @Test
    fun handleError_returnsUserMessage() {
        val error = AppError.NetworkError.ConnectionError()
        val message = ErrorHandler.handleError(error)
        assertEquals(error.userMessage, message)
    }

    @Test
    fun handleThrowable_transformsThenReturnsUserMessage() {
        val cause = ConnectException("Failed")
        val message = ErrorHandler.handleThrowable(cause)
        assertEquals(
            (ErrorHandler.transform(cause) as AppError.NetworkError.ConnectionError).userMessage,
            message
        )
    }
}
