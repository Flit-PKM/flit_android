package com.bmdstudios.flit.utils

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.bmdstudios.flit.config.AudioConfig
import com.bmdstudios.flit.domain.error.AppError
import com.bmdstudios.flit.domain.error.ErrorHandler
import com.bmdstudios.flit.domain.toAppError
import com.bmdstudios.flit.utils.Constants
import com.bmdstudios.flit.utils.audio.AudioConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.InputStream

/**
 * Records audio from the device microphone and saves it as a WAV file.
 * Handles PCM recording, conversion to WAV format, and resource management.
 */
class VoiceRecorder(
    private val context: Context,
    private val outputFile: File,
    private val audioConfig: AudioConfig = AudioConfig.default()
) {
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false
    private var pcmFile: File? = null

    init {
        Timber.d("VoiceRecorder initialized with output file: ${outputFile.absolutePath}")
    }

    /**
     * Starts recording audio.
     */
    suspend fun startRecording(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isRecording) {
            Timber.w("Attempted to start recording while already recording")
            return@withContext Result.failure(
                AppError.AudioError.RecordingError("Already recording").toException()
            )
        }

        try {
            Timber.d("Starting audio recording to: ${outputFile.absolutePath}")
            outputFile.parentFile?.mkdirs()
            Timber.d("Created/verified parent directory: ${outputFile.parentFile?.absolutePath}")

            val bufferSize = calculateBufferSize()
            if (bufferSize <= 0) {
                val error = AppError.AudioError.RecordingError("Invalid buffer size: $bufferSize")
                ErrorHandler.logError(error)
                return@withContext Result.failure(error.toException())
            }

            Timber.d("AudioRecord buffer size: $bufferSize bytes")

            pcmFile = File(outputFile.parent, "${outputFile.nameWithoutExtension}.${Constants.PCM_FILE_EXTENSION}")
            Timber.d("PCM temporary file: ${pcmFile?.absolutePath}")

            audioRecord = createAudioRecord(bufferSize).fold(
                onSuccess = { it },
                onFailure = { throwable ->
                    val error = throwable.toAppError("createAudioRecord")
                    cleanup()
                    return@withContext Result.failure(error.toException())
                }
            )

            Timber.d("AudioRecord initialized successfully")

            audioRecord?.startRecording()
            isRecording = true

            recordingThread = Thread(
                { writeAudioDataToFile(bufferSize) },
                "AudioRecorderThread"
            ).apply {
                start()
            }

            Timber.i("Audio recording started successfully: ${outputFile.absolutePath}")
            Result.success(Unit)
        } catch (e: IOException) {
            val error = ErrorHandler.transform(e, "startRecording")
            ErrorHandler.logError(error)
            cleanup()
            Result.failure(error.toException())
        } catch (e: IllegalStateException) {
            val error = ErrorHandler.transform(e, "startRecording")
            ErrorHandler.logError(error)
            cleanup()
            Result.failure(error.toException())
        }
    }

    /**
     * Calculates the required buffer size for AudioRecord.
     */
    private fun calculateBufferSize(): Int {
        return AudioRecord.getMinBufferSize(
            audioConfig.sampleRate,
            audioConfig.channelConfig,
            audioConfig.audioFormat
        )
    }

    /**
     * Creates and initializes an AudioRecord instance.
     */
    private fun createAudioRecord(bufferSize: Int): Result<AudioRecord> {
        return try {
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                audioConfig.sampleRate,
                audioConfig.channelConfig,
                audioConfig.audioFormat,
                bufferSize * audioConfig.bufferMultiplier
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord.release()
                val error = AppError.AudioError.RecordingError("AudioRecord initialization failed")
                ErrorHandler.logError(error)
                Result.failure(error.toException())
            } else {
                Result.success(audioRecord)
            }
        } catch (e: Exception) {
            val error = ErrorHandler.transform(e, "createAudioRecord")
            ErrorHandler.logError(error)
            Result.failure(error.toException())
        }
    }

    /**
     * Writes audio data from AudioRecord to a PCM file.
     */
    private fun writeAudioDataToFile(bufferSize: Int) {
        val buffer = ByteArray(bufferSize)
        var outputStream: FileOutputStream? = null

        try {
            outputStream = FileOutputStream(pcmFile)
            Timber.d("Started writing PCM data to file")

            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: break

                when (read) {
                    AudioRecord.ERROR_INVALID_OPERATION -> {
                        Timber.e("AudioRecord read error: ERROR_INVALID_OPERATION")
                        break
                    }
                    AudioRecord.ERROR_BAD_VALUE -> {
                        Timber.e("AudioRecord read error: ERROR_BAD_VALUE")
                        break
                    }
                    else -> {
                        outputStream.write(buffer, 0, read)
                    }
                }
            }

            outputStream.flush()
            Timber.d("Finished writing PCM data to file")
        } catch (e: IOException) {
            Timber.e(e, "Error writing PCM data to file")
        } finally {
            outputStream?.close()
        }
    }

    /**
     * Stops recording and converts PCM to WAV.
     */
    suspend fun stopRecording(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isRecording) {
            Timber.w("Attempted to stop recording while not recording")
            return@withContext Result.failure(
                AppError.AudioError.RecordingError("Not recording").toException()
            )
        }

        try {
            Timber.d("Stopping audio recording: ${outputFile.absolutePath}")

            isRecording = false

            audioRecord?.apply {
                stop()
                release()
            }
            audioRecord = null

            recordingThread?.join(audioConfig.threadJoinTimeoutMs)
            recordingThread = null

            pcmFile?.let { pcm ->
                if (pcm.exists()) {
                    convertPcmToWav(pcm, outputFile).fold(
                        onSuccess = {
                            pcm.delete()
                            Timber.d("Cleaned up temporary PCM file")
                        },
                        onFailure = { throwable ->
                            val error = if (throwable is com.bmdstudios.flit.domain.error.AppErrorException) {
                                throwable.appError
                            } else {
                                com.bmdstudios.flit.domain.error.ErrorHandler.transform(throwable, "convertPcmToWav")
                            }
                            Timber.e("Failed to convert PCM to WAV: ${error.userMessage}")
                            return@withContext Result.failure(error.toException())
                        }
                    )
                }
            }
            pcmFile = null

            val fileSize = if (outputFile.exists()) outputFile.length() else 0L
            Timber.i("Audio recording stopped successfully: ${outputFile.absolutePath}, file size: $fileSize bytes")
            Result.success(Unit)
        } catch (e: Exception) {
            val error = ErrorHandler.transform(e, "stopRecording")
            ErrorHandler.logError(error)
            cleanup()
            Result.failure(error.toException())
        }
    }

    /**
     * Converts PCM data to WAV format using streaming to avoid loading entire file into memory.
     */
    private fun convertPcmToWav(pcmFile: File, wavFile: File): Result<Unit> {
        return try {
            Timber.d("Converting PCM to WAV: ${pcmFile.absolutePath} -> ${wavFile.absolutePath}")

            val pcmDataSize = pcmFile.length()
            if (pcmDataSize == 0L) {
                val error = AppError.FileError.ReadError(pcmFile.name)
                ErrorHandler.logError(error)
                return Result.failure(error.toException())
            }

            val headerSize = AudioConstants.WAV_HEADER_SIZE
            val fileSize = pcmDataSize + headerSize - 8

            val wavHeader = createWavHeader(pcmDataSize, fileSize)

            // Use streaming approach for large files
            FileOutputStream(wavFile).use { out ->
                // Write header first
                out.write(wavHeader.array())
                
                // Stream PCM data in chunks
                pcmFile.inputStream().use { input ->
                    val buffer = ByteArray(Constants.DOWNLOAD_BUFFER_SIZE)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        out.write(buffer, 0, bytesRead)
                    }
                }
                out.flush()
            }

            Timber.d("Successfully converted PCM to WAV: ${wavFile.absolutePath}")
            Result.success(Unit)
        } catch (e: Exception) {
            val error = ErrorHandler.transform(e, "convertPcmToWav")
            ErrorHandler.logError(error)
            Result.failure(error.toException())
        }
    }

    /**
     * Creates the WAV file header.
     */
    private fun createWavHeader(pcmDataSize: Long, fileSize: Long): ByteBuffer {
        return ByteBuffer.allocate(AudioConstants.WAV_HEADER_SIZE).apply {
            order(ByteOrder.LITTLE_ENDIAN)

            // RIFF header (12 bytes)
            put(AudioConstants.WAV_RIFF_HEADER.toByteArray())
            putInt(fileSize.toInt())
            put(AudioConstants.WAV_WAVE_HEADER.toByteArray())

            // Format chunk (24 bytes)
            put(AudioConstants.WAV_FMT_CHUNK.toByteArray())
            putInt(AudioConstants.WAV_FMT_SUBCHUNK_SIZE) // Subchunk1Size (16 for PCM)
            putShort(AudioConstants.WAV_AUDIO_FORMAT_PCM.toShort()) // AudioFormat (1 = PCM)
            putShort(1.toShort()) // NumChannels (1 = mono)
            putInt(audioConfig.sampleRate) // SampleRate
            putInt(audioConfig.sampleRate * audioConfig.bytesPerSample) // ByteRate
            putShort(audioConfig.bytesPerSample.toShort()) // BlockAlign
            putShort(audioConfig.bitsPerSample.toShort()) // BitsPerSample

            // Data chunk header (8 bytes)
            put(AudioConstants.WAV_DATA_CHUNK.toByteArray())
            putInt(pcmDataSize.toInt()) // Subchunk2Size
        }
    }

    /**
     * Cleans up resources.
     */
    private fun cleanup() {
        isRecording = false
        audioRecord?.release()
        audioRecord = null
        recordingThread?.interrupt()
        recordingThread = null
        pcmFile?.delete()
        pcmFile = null
    }

    /**
     * Checks if currently recording.
     */
    fun isCurrentlyRecording(): Boolean = isRecording

    /**
     * Releases all resources.
     * Note: This is a non-suspend function. If recording is in progress,
     * it will attempt to stop it synchronously which may block.
     */
    fun release() {
        Timber.d("Releasing VoiceRecorder: ${outputFile.absolutePath}")
        if (isRecording) {
            Timber.d("Recording in progress, stopping before release")
            // Stop recording synchronously for cleanup
            isRecording = false
            audioRecord?.apply {
                stop()
                release()
            }
            audioRecord = null
            recordingThread?.interrupt()
            recordingThread = null
            pcmFile?.delete()
            pcmFile = null
        } else {
            cleanup()
        }
    }
}
