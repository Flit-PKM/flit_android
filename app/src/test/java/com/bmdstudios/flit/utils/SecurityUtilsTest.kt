package com.bmdstudios.flit.utils

import com.bmdstudios.flit.domain.error.AppError
import com.bmdstudios.flit.domain.error.AppErrorException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Unit tests for [SecurityUtils] path validation, filename validation, and sanitization.
 */
class SecurityUtilsTest {

    private val baseDir = File(System.getProperty("java.io.tmpdir"), "SecurityUtilsTest").apply {
        mkdirs()
        deleteOnExit()
    }

    @Test
    fun validateAndSanitizePath_blankPath_fails() {
        val result = SecurityUtils.validateAndSanitizePath("   ", baseDir)
        assertTrue(result.isFailure)
        val err = (result.exceptionOrNull() as? AppErrorException)?.appError
        assertTrue(err is AppError.FileError.InvalidPathError)
    }

    @Test
    fun validateAndSanitizePath_validRelativePath_succeeds() {
        val subDir = File(baseDir, "sub").apply { mkdirs() }
        val result = SecurityUtils.validateAndSanitizePath("sub", baseDir)
        assertTrue(result.isSuccess)
        assertEquals(subDir.canonicalPath, result.getOrNull()!!.canonicalPath)
    }

    @Test
    fun validateAndSanitizePath_pathWithDotDot_fails() {
        val result = SecurityUtils.validateAndSanitizePath("..", baseDir)
        assertTrue(result.isFailure)
    }

    @Test
    fun validateAndSanitizePath_pathWithSlash_fails() {
        val result = SecurityUtils.validateAndSanitizePath("foo/bar", baseDir)
        assertTrue(result.isFailure)
    }

    @Test
    fun validateAndSanitizePath_pathTraversalOutsideBase_fails() {
        val safeDir = File(baseDir, "safe").apply { mkdirs() }
        val result = SecurityUtils.validateAndSanitizePath("..", safeDir)
        assertTrue(result.isFailure)
    }

    @Test
    fun validateFileName_blank_fails() {
        val result = SecurityUtils.validateFileName("")
        assertTrue(result.isFailure)
    }

    @Test
    fun validateFileName_validName_succeeds() {
        val result = SecurityUtils.validateFileName("model.onnx")
        assertTrue(result.isSuccess)
    }

    @Test
    fun validateFileName_containsColon_fails() {
        val result = SecurityUtils.validateFileName("file:name.txt")
        assertTrue(result.isFailure)
    }

    @Test
    fun validateFileName_containsAsterisk_fails() {
        val result = SecurityUtils.validateFileName("file*.txt")
        assertTrue(result.isFailure)
    }

    @Test
    fun validateFileName_containsDotDot_fails() {
        val result = SecurityUtils.validateFileName("..")
        assertTrue(result.isFailure)
    }

    @Test
    fun validateFileExtension_blankExtension_fails() {
        val result = SecurityUtils.validateFileExtension("noext")
        assertTrue(result.isFailure)
    }

    @Test
    fun validateFileExtension_allowedExtension_succeeds() {
        assertTrue(SecurityUtils.validateFileExtension("a.wav").isSuccess)
        assertTrue(SecurityUtils.validateFileExtension("b.pcm").isSuccess)
        assertTrue(SecurityUtils.validateFileExtension("c.onnx").isSuccess)
        assertTrue(SecurityUtils.validateFileExtension("d.txt").isSuccess)
    }

    @Test
    fun validateFileExtension_disallowedExtension_fails() {
        val result = SecurityUtils.validateFileExtension("file.exe")
        assertTrue(result.isFailure)
    }

    @Test
    fun validateFileSize_fileNotFound_fails() {
        val missing = File(baseDir, "missing.txt")
        val result = SecurityUtils.validateFileSize(missing, 1024L)
        assertTrue(result.isFailure)
        val err = (result.exceptionOrNull() as? AppErrorException)?.appError
        assertTrue(err is AppError.FileError.NotFoundError)
    }

    @Test
    fun validateFileSize_fileWithinLimit_succeeds() {
        val file = File(baseDir, "small.txt").apply { writeText("hi") }
        val result = SecurityUtils.validateFileSize(file, 1024L)
        assertTrue(result.isSuccess)
        file.delete()
    }

    @Test
    fun validateFileSize_fileExceedsLimit_fails() {
        val file = File(baseDir, "big.txt").apply { writeText("x".repeat(100)) }
        val result = SecurityUtils.validateFileSize(file, 50L)
        assertTrue(result.isFailure)
        val err = (result.exceptionOrNull() as? AppErrorException)?.appError
        assertTrue(err is AppError.FileError.SizeLimitExceededError)
        file.delete()
    }

    @Test
    fun sanitizeErrorMessage_replacesUnixPath() {
        val out = SecurityUtils.sanitizeErrorMessage("Error in /home/user/secret.txt")
        assertFalse(out.contains("/home/user"))
        assertTrue(out.contains("[path]"))
    }

    @Test
    fun sanitizeErrorMessage_replacesBearerToken() {
        val out = SecurityUtils.sanitizeErrorMessage("Bearer abc123xyz")
        assertEquals("Bearer [token]", out)
    }
}
