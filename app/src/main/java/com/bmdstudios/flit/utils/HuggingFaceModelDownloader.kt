package com.bmdstudios.flit.utils

import com.bmdstudios.flit.config.NetworkConfig
import com.bmdstudios.flit.domain.error.AppError
import com.bmdstudios.flit.domain.error.ErrorHandler
import com.bmdstudios.flit.domain.toAppError
import com.bmdstudios.flit.utils.Constants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Sealed class representing different types of download requests.
 */
private sealed class DownloadRequest {
    abstract val url: String
    abstract val targetFile: File
    abstract val token: String?
    abstract val onProgress: (downloaded: Long, total: Long) -> Unit

    data class HuggingFaceFile(
        val repoId: String,
        val revision: String,
        val filename: String,
        override val targetFile: File,
        override val token: String?,
        override val onProgress: (downloaded: Long, total: Long) -> Unit
    ) : DownloadRequest() {
        override val url: String
            get() = "https://huggingface.co/$repoId/resolve/$revision/$filename"
    }

    data class DirectUrl(
        override val url: String,
        override val targetFile: File,
        override val token: String?,
        override val onProgress: (downloaded: Long, total: Long) -> Unit
    ) : DownloadRequest()
}

/**
 * Downloads models from HuggingFace and other sources.
 * Handles resumable downloads, progress tracking, and error handling.
 */
class HuggingFaceModelDownloader(
    private val client: OkHttpClient,
    private val networkConfig: NetworkConfig
) {

    /**
     * Downloads all files from a HuggingFace repository.
     */
    suspend fun download(
        repoId: String,
        revision: String = "main",
        localDir: File,
        token: String? = null,
        onProgress: (filename: String, downloaded: Long, total: Long) -> Unit = { _, _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Download initialization: repoId=$repoId, revision=$revision, localDir=${localDir.absolutePath}")
            
            if (!localDir.mkdirs() && !localDir.isDirectory) {
                val error = AppError.FileError.WriteError(localDir.absolutePath)
                ErrorHandler.logError(error)
                return@withContext Result.failure(error.toException())
            }
            Timber.d("Created/verified local directory: ${localDir.absolutePath}")

            val filenames = fetchRepositoryFileList(repoId, token).getOrElse { throwable ->
                val error = ErrorHandler.transform(throwable, "fetchRepositoryFileList")
                ErrorHandler.logError(error)
                return@withContext Result.failure(error.toException())
            }

            Timber.i("Found ${filenames.size} files in repository: $repoId")

            // Filter to prefer int8 versions over regular versions
            val filteredFilenames = filterFilesToPreferInt8(filenames)
            Timber.i("Filtered to ${filteredFilenames.size} files (preferring int8 versions)")

            for (filename in filteredFilenames) {
                val targetFile = File(localDir, filename)
                targetFile.parentFile?.mkdirs()

                if (targetFile.exists() && targetFile.length() > 0) {
                    val fileSize = targetFile.length()
                    Timber.d("File already exists, skipping: $filename (${fileSize} bytes)")
                    onProgress(filename, fileSize, fileSize)
                    continue
                }

                Timber.i("Starting download for file: $filename to ${targetFile.absolutePath}")
                val request = DownloadRequest.HuggingFaceFile(
                    repoId = repoId,
                    revision = revision,
                    filename = filename,
                    targetFile = targetFile,
                    token = token,
                    onProgress = { downloaded, total -> onProgress(filename, downloaded, total) }
                )

                downloadFile(request).fold(
                    onSuccess = {
                        Timber.d("Completed download for file: $filename")
                    },
                    onFailure = { throwable ->
                        val error = throwable.toAppError("downloadFile")
                        Timber.e(throwable, "Failed to download file: $filename")
                        return@withContext Result.failure(error.toException())
                    }
                )
            }

            Timber.i("All files downloaded successfully from repository: $repoId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            // Rethrow cancellation - don't treat as error
            throw e
        } catch (e: Exception) {
            val error = ErrorHandler.transform(e, "download")
            ErrorHandler.logError(error)
            Result.failure(error.toException())
        }
    }

    /**
     * Downloads a file from a direct URL.
     */
    suspend fun downloadFileFromUrl(
        url: String,
        targetFile: File,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Downloading file from URL: $url to ${targetFile.absolutePath}")
            targetFile.parentFile?.mkdirs()

            if (targetFile.exists() && targetFile.length() > 0) {
                val fileSize = targetFile.length()
                Timber.d("File already exists, skipping: ${targetFile.name} (${fileSize} bytes)")
                onProgress(fileSize, fileSize)
                return@withContext Result.success(Unit)
            }

            val request = DownloadRequest.DirectUrl(
                url = url,
                targetFile = targetFile,
                token = null,
                onProgress = onProgress
            )

            downloadFile(request).fold(
                onSuccess = {
                    Timber.i("File downloaded successfully: ${targetFile.name}")
                    Result.success(Unit)
                },
                onFailure = { throwable ->
                    val error = throwable.toAppError("downloadFileFromUrl")
                    Timber.e(throwable, "Failed to download file from URL: $url")
                    Result.failure(error.toException())
                }
            )
        } catch (e: Exception) {
            val error = ErrorHandler.transform(e, "downloadFileFromUrl")
            ErrorHandler.logError(error)
            Result.failure(error.toException())
        }
    }

    /**
     * Fetches the list of files from a HuggingFace repository.
     */
    private suspend fun fetchRepositoryFileList(
        repoId: String,
        token: String?
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://huggingface.co/api/models/$repoId"
            Timber.d("Fetching repository file list from: $apiUrl")

            val listRequest = Request.Builder()
                .url(apiUrl)
                .apply {
                    token?.let {
                        header("Authorization", "Bearer $it")
                        Timber.d("Using authentication token for API request")
                    }
                }
                .build()

            client.newCall(listRequest).execute().use { response ->
                Timber.d("API response status: ${response.code}")

                if (!response.isSuccessful) {
                    val error = AppError.NetworkError.HttpError(response.code)
                    ErrorHandler.logError(error)
                    return@withContext Result.failure(error.toException())
                }

                val bodyString = response.body?.string() ?: run {
                    val error = AppError.NetworkError.DownloadError()
                    ErrorHandler.logError(error)
                    return@withContext Result.failure(error.toException())
                }

                val json = JSONObject(bodyString)
                val siblings = json.optJSONArray("siblings") ?: run {
                    val error = AppError.NetworkError.DownloadError("No files found in repository")
                    ErrorHandler.logError(error)
                    return@withContext Result.failure(error.toException())
                }

                val filenames = mutableListOf<String>()
                for (i in 0 until siblings.length()) {
                    filenames.add(siblings.getJSONObject(i).getString("rfilename"))
                }

                Result.success(filenames)
            }
        } catch (e: Exception) {
            val error = ErrorHandler.transform(e, "fetchRepositoryFileList")
            ErrorHandler.logError(error)
            Result.failure(error.toException())
        }
    }

    /**
     * Filters file list to prefer int8 versions over regular versions.
     * If both versions exist for a file (e.g., "encoder.onnx" and "encoder.int8.onnx"),
     * only the int8 version will be kept.
     * 
     * @param filenames List of all filenames from the repository
     * @return Filtered list with int8 versions preferred
     */
    private fun filterFilesToPreferInt8(filenames: List<String>): List<String> {
        val filesByBaseName = mutableMapOf<String, MutableList<String>>()
        
        // Group files by their base name (before any int8 qualifier)
        filenames.forEach { filename ->
            val baseName = extractBaseName(filename)
            filesByBaseName.getOrPut(baseName) { mutableListOf() }.add(filename)
        }
        
        val filteredFiles = mutableListOf<String>()
        
        // For each base name, prefer int8 version if it exists
        filesByBaseName.forEach { (baseName, variants) ->
            val int8Version = variants.firstOrNull { isInt8Version(it) }
            val regularVersion = variants.firstOrNull { !isInt8Version(it) }
            
            when {
                int8Version != null -> {
                    // Prefer int8 version, skip regular
                    filteredFiles.add(int8Version)
                    if (regularVersion != null) {
                        Timber.d("Preferring int8 version: $int8Version over $regularVersion")
                    }
                }
                regularVersion != null -> {
                    // Only regular version exists, use it
                    filteredFiles.add(regularVersion)
                }
                // If neither matches (shouldn't happen), skip or handle edge case
            }
        }
        
        return filteredFiles
    }

    /**
     * Extracts the base name of a file by removing int8 qualifier.
     * Examples:
     * - "encoder.onnx" -> "encoder.onnx"
     * - "encoder.int8.onnx" -> "encoder.onnx"
     * - "base-encoder.int8.onnx" -> "base-encoder.onnx"
     */
    private fun extractBaseName(filename: String): String {
        val components = filename.split('.')
        val baseComponents = components.filterNot { it.equals("int8", ignoreCase = true) }
        return baseComponents.joinToString(".")
    }

    /**
     * Checks if a filename is an int8 version.
     * An int8 version has "int8" as a component in the filename.
     */
    private fun isInt8Version(filename: String): Boolean {
        val components = filename.split('.')
        return components.any { it.equals("int8", ignoreCase = true) }
    }

    /**
     * Unified download method that handles both HuggingFace and direct URL downloads.
     * Eliminates code duplication between different download types.
     */
    private suspend fun downloadFile(request: DownloadRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val offset = if (request.targetFile.exists()) request.targetFile.length() else 0L

            if (offset > 0) {
                Timber.d("Resuming download for ${request.targetFile.name} from offset: $offset bytes")
            } else {
                Timber.d("Starting new download for ${request.targetFile.name}")
            }
            Timber.d("Download URL: ${request.url}")

            val httpRequest = buildHttpRequest(request, offset)
            val lastProgressUpdate = System.currentTimeMillis()

            client.newCall(httpRequest).execute().use { response ->
                handleDownloadResponse(response, request, offset, lastProgressUpdate)
            }
        } catch (e: Exception) {
            val error = ErrorHandler.transform(e, "downloadFile")
            ErrorHandler.logError(error)
            throw error.toException()
        }
    }

    /**
     * Builds the HTTP request for downloading.
     */
    private fun buildHttpRequest(request: DownloadRequest, offset: Long): Request {
        return Request.Builder()
            .url(request.url)
            .apply {
                request.token?.let {
                    header("Authorization", "Bearer $it")
                    Timber.v("Added authentication header")
                }
                if (offset > 0) {
                    header("Range", "bytes=$offset-")
                    Timber.d("Added Range header for resume: bytes=$offset-")
                }
            }
            .build()
    }

    /**
     * Handles the HTTP response and performs the actual file download.
     */
    private suspend fun handleDownloadResponse(
        response: Response,
        request: DownloadRequest,
        offset: Long,
        lastProgressUpdate: Long
    ): Result<Unit> {
        Timber.d("File download response status: ${response.code} for ${request.targetFile.name}")

            if (response.code !in listOf(200, 206)) {
                val error = AppError.NetworkError.HttpError(response.code)
                ErrorHandler.logError(error)
                throw error.toException()
            }

            val body = response.body ?: run {
                val error = AppError.NetworkError.DownloadError(request.targetFile.name)
                ErrorHandler.logError(error)
                throw error.toException()
            }

        val isPartial = response.code == 206
        val contentLength = body.contentLength()
        Timber.d("Response details for ${request.targetFile.name}: isPartial=$isPartial, contentLength=$contentLength, offset=$offset")

        val totalSize = calculateTotalSize(contentLength, offset, isPartial, response)
        writeFileData(body, request, offset, isPartial, totalSize, lastProgressUpdate)

        Timber.i("File download completed: ${request.targetFile.name}")
        return Result.success(Unit)
    }

    /**
     * Calculates the total file size from the response.
     */
    private fun calculateTotalSize(
        contentLength: Long,
        offset: Long,
        isPartial: Boolean,
        response: Response
    ): Long {
        return if (contentLength >= 0) {
            val rangeHeader = response.header("Content-Range")
            if (isPartial && rangeHeader != null) {
                val calculatedTotal = rangeHeader.substringAfterLast("/").toLongOrNull()
                    ?: (offset + contentLength)
                Timber.d("Partial download detected. Range header: $rangeHeader, Total file size: $calculatedTotal")
                calculatedTotal
            } else {
                offset + contentLength
            }
        } else {
            Timber.w("Unknown content length")
            -1L
        }
    }

    /**
     * Writes the downloaded data to the file with progress tracking.
     */
    private suspend fun writeFileData(
        body: okhttp3.ResponseBody,
        request: DownloadRequest,
        offset: Long,
        isPartial: Boolean,
        totalSize: Long,
        lastProgressUpdate: Long
    ) {
        FileOutputStream(request.targetFile, offset > 0 && isPartial).use { out ->
            body.byteStream().use { input ->
                val buffer = ByteArray(Constants.DOWNLOAD_BUFFER_SIZE)
                var bytesRead: Int
                var downloaded = offset
                var lastLoggedBytes = offset
                var lastUpdateTime = lastProgressUpdate

                Timber.d("Starting to write file: ${request.targetFile.absolutePath}")

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    out.write(buffer, 0, bytesRead)
                    downloaded += bytesRead

                    // Throttle progress updates
                    val currentTime = System.currentTimeMillis()
                    val shouldUpdate = (currentTime - lastUpdateTime) >= networkConfig.progressUpdateThrottleMs

                    // Log progress milestones (every MB or when complete)
                    if (downloaded - lastLoggedBytes >= Constants.PROGRESS_LOG_INTERVAL_BYTES || bytesRead == -1) {
                        Timber.v("Download progress for ${request.targetFile.name}: $downloaded/$totalSize bytes")
                        lastLoggedBytes = downloaded
                    }

                    // Throttled progress callback
                    if (shouldUpdate || bytesRead == -1) {
                        request.onProgress(downloaded, totalSize)
                        lastUpdateTime = currentTime
                    }
                }

                out.flush()
                Timber.i("File download completed: ${request.targetFile.name}, total bytes: $downloaded")
            }
        }
    }
}
