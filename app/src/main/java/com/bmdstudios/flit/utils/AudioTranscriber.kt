package com.bmdstudios.flit.utils

import android.content.Context
import com.bmdstudios.flit.config.AppConfig
import com.bmdstudios.flit.data.repository.SettingsRepository
import com.bmdstudios.flit.domain.error.AppError
import com.bmdstudios.flit.domain.error.ErrorHandler
import com.bmdstudios.flit.domain.service.ModelTypeDetector
import com.bmdstudios.flit.domain.toAppError
import com.bmdstudios.flit.utils.audio.AudioConstants
import com.bmdstudios.flit.utils.model.DenoiserModelFactory
import com.bmdstudios.flit.utils.model.ModelConstants
import com.bmdstudios.flit.utils.model.PunctuationModelFactory
import com.bmdstudios.flit.utils.model.SenseVoiceModelConfigBuilder
import com.bmdstudios.flit.utils.model.TransducerModelConfigBuilder
import com.bmdstudios.flit.utils.model.WhisperModelConfigBuilder
import com.bmdstudios.flit.utils.text.TextPostProcessor
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineStream
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiser
import com.k2fsa.sherpa.onnx.OfflinePunctuation
import com.k2fsa.sherpa.onnx.WaveReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Transcribes audio files using offline speech recognition models.
 * Handles model initialization, audio processing, and transcription.
 */
class AudioTranscriber(
    private val context: Context,
    private val appConfig: AppConfig,
    private val settingsRepository: SettingsRepository,
    private val modelTypeDetector: ModelTypeDetector,
    private val whisperModelConfigBuilder: WhisperModelConfigBuilder,
    private val transducerModelConfigBuilder: TransducerModelConfigBuilder,
    private val senseVoiceModelConfigBuilder: SenseVoiceModelConfigBuilder,
    private val denoiserModelFactory: DenoiserModelFactory,
    private val punctuationModelFactory: PunctuationModelFactory,
    private val textPostProcessor: TextPostProcessor
) {

    /**
     * Creates a speech denoiser if the model is available.
     */
    private suspend fun createDenoiser(modelSize: com.bmdstudios.flit.ui.settings.ModelSize): Result<OfflineSpeechDenoiser?> {
        val denoiserDir = appConfig.modelConfig.getDenoiserModelSourceForSize(modelSize)?.let { source ->
            val modelName = when (source) {
                is com.bmdstudios.flit.config.ModelDownloadSource.HuggingFace -> source.getDirectoryName()
                is com.bmdstudios.flit.config.ModelDownloadSource.DirectUrl -> source.getDirectoryName()
            }
            java.io.File(appConfig.modelConfig.baseDirectory, modelName)
        } ?: appConfig.modelConfig.denoiserModelDirectory
        return denoiserModelFactory.createDenoiser(
            denoiserDir,
            appConfig
        )
    }

    /**
     * Creates a punctuation model if available.
     */
    private suspend fun createPunctuation(modelSize: com.bmdstudios.flit.ui.settings.ModelSize): Result<OfflinePunctuation?> {
        val punctuationDir = appConfig.modelConfig.getPunctuationModelSourceForSize(modelSize)?.let { source ->
            val modelName = when (source) {
                is com.bmdstudios.flit.config.ModelDownloadSource.HuggingFace -> source.getDirectoryName()
                is com.bmdstudios.flit.config.ModelDownloadSource.DirectUrl -> source.getDirectoryName()
            }
            java.io.File(appConfig.modelConfig.baseDirectory, modelName)
        } ?: appConfig.modelConfig.punctuationModelDirectory
        return punctuationModelFactory.createPunctuation(
            punctuationDir,
            appConfig
        )
    }

    /**
     * Creates the model configuration for the ASR model.
     * Supports SenseVoice, Whisper, and Transducer model types with automatic detection.
     */
    private suspend fun createModelConfig(modelSize: com.bmdstudios.flit.ui.settings.ModelSize): Result<OfflineModelConfig> = withContext(Dispatchers.IO) {
        try {
            val modelDir = appConfig.modelConfig.getAsrModelDirectoryForSize(modelSize)
                ?: return@withContext Result.failure(
                    AppError.ModelError.ModelNotFoundError("No model configured for $modelSize").toException()
                )
            Timber.d("Creating model config from directory: ${modelDir.absolutePath}")

            if (!modelDir.exists() || !modelDir.isDirectory) {
                val error = AppError.ModelError.ModelNotFoundError(modelDir.name)
                ErrorHandler.logError(error)
                return@withContext Result.failure(error.toException())
            }

            val files = modelDir.listFiles()?.map { it.name } ?: emptyList()
            Timber.d("Files in model directory: ${files.joinToString()}")

            // Determine model type: use explicit type if set, otherwise auto-detect
            // Priority: explicit type > SenseVoice > Whisper > Transducer
            val explicitModelType = appConfig.modelConfig.asrModelType
            when {
                explicitModelType == com.bmdstudios.flit.config.AsrModelType.SENSE_VOICE -> {
                    Timber.d("Using explicit SENSE_VOICE model type")
                    senseVoiceModelConfigBuilder.createSenseVoiceModelConfig(modelDir, files, appConfig)
                }
                explicitModelType == com.bmdstudios.flit.config.AsrModelType.WHISPER -> {
                    Timber.d("Using explicit WHISPER model type")
                    whisperModelConfigBuilder.createWhisperModelConfig(modelDir, files, appConfig)
                }
                explicitModelType == com.bmdstudios.flit.config.AsrModelType.TRANSDUCER -> {
                    Timber.d("Using explicit TRANSDUCER model type")
                    transducerModelConfigBuilder.createTransducerModelConfig(modelDir, files, appConfig)
                }
                modelTypeDetector.detectSenseVoiceModel(files) -> {
                    Timber.d("Auto-detected SENSE_VOICE model")
                    senseVoiceModelConfigBuilder.createSenseVoiceModelConfig(modelDir, files, appConfig)
                }
                modelTypeDetector.detectWhisperModel(files) -> {
                    Timber.d("Auto-detected WHISPER model")
                    whisperModelConfigBuilder.createWhisperModelConfig(modelDir, files, appConfig)
                }
                else -> {
                    Timber.d("Auto-detected TRANSDUCER model (fallback)")
                    transducerModelConfigBuilder.createTransducerModelConfig(modelDir, files, appConfig)
                }
            }
        } catch (e: Exception) {
            val error = ErrorHandler.transform(e, "createModelConfig")
            ErrorHandler.logError(error)
            Result.failure(error.toException())
        }
    }



    /**
     * Transcribes an audio file.
     */
    suspend fun transcribe(wavFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            Timber.i("Starting transcription for file: ${wavFile.absolutePath}")

            if (!wavFile.exists()) {
                val error = AppError.FileError.NotFoundError(wavFile.name)
                ErrorHandler.logError(error)
                return@withContext Result.failure(error.toException())
            }

            // Get current model size from settings
            val modelSize = settingsRepository.getModelSize()
            
            // If ModelSize.NONE, return error (text-only mode, no transcription)
            if (modelSize == com.bmdstudios.flit.ui.settings.ModelSize.NONE) {
                val error = AppError.ModelError.ModelNotFoundError("No ASR model configured (text-only mode)")
                ErrorHandler.logError(error)
                return@withContext Result.failure(error.toException())
            }

            val modelConfigResult = createModelConfig(modelSize)
            val modelConfig = modelConfigResult.getOrElse { throwable ->
                val error = throwable.toAppError("createModelConfig")
                return@withContext Result.failure(error.toException())
            }

            val featureConfig = FeatureConfig(
                sampleRate = AudioConstants.SAMPLE_RATE,
                featureDim = AudioConstants.FEATURE_DIM,
                dither = AudioConstants.DITHER
            )

            val recognizerConfig = OfflineRecognizerConfig(
                featConfig = featureConfig,
                modelConfig = modelConfig,
                decodingMethod = ModelConstants.DECODING_METHOD_GREEDY_SEARCH
            )

            Timber.d("Creating OfflineRecognizer")
            val recognizer = OfflineRecognizer(config = recognizerConfig)

            val denoiserResult = createDenoiser(modelSize)
            val denoiser = denoiserResult.getOrElse {
                Timber.w("Denoiser initialization failed, continuing without denoising")
                null
            } ?: denoiserResult.getOrNull()

            val punctuationResult = createPunctuation(modelSize)
            val punctuation = punctuationResult.getOrElse {
                Timber.w("Punctuation initialization failed, continuing without punctuation")
                null
            } ?: punctuationResult.getOrNull()

            try {
                Timber.d("Reading WAV file: ${wavFile.absolutePath}")
                val waveData = WaveReader.readWave(wavFile.absolutePath)
                Timber.d("Read WAV file: ${waveData.samples.size} samples at ${waveData.sampleRate} Hz")

                val (audioSamples, audioSampleRate) = applyDenoising(waveData, denoiser)

                val stream = recognizer.createStream()
                try {
                    Timber.d("Accepting waveform")
                    stream.acceptWaveform(audioSamples, audioSampleRate)

                    Timber.d("Decoding")
                    recognizer.decode(stream)

                    Timber.d("Getting result")
                    val result = recognizer.getResult(stream)
                    val rawText = result.text

                    // Process text with punctuation model
                    val finalText = textPostProcessor.processWithPunctuationModel(rawText) { text ->
                        if (punctuation != null && text.isNotBlank()) {
                            try {
                                Timber.d("Applying punctuation to text: $text")
                                val punctuatedText = punctuation.addPunctuation(text)
                                Timber.d("Punctuated text: $punctuatedText")
                                punctuatedText
                            } catch (e: Exception) {
                                Timber.w(e, "Punctuation failed, using original text")
                                null
                            }
                        } else {
                            null
                        }
                    }

                    Timber.i("Transcription completed: $finalText")
                    Result.success(finalText)
                } finally {
                    stream.release()
                }
            } finally {
                recognizer.release()
                denoiser?.release()
                punctuation?.release()
            }
        } catch (e: Exception) {
            val error = ErrorHandler.transform(e, "transcribe")
            ErrorHandler.logError(error)
            Result.failure(error.toException())
        }
    }

    /**
     * Applies denoising to audio if denoiser is available.
     */
    private suspend fun applyDenoising(
        waveData: com.k2fsa.sherpa.onnx.WaveData,
        denoiser: OfflineSpeechDenoiser?
    ): Pair<FloatArray, Int> = withContext(Dispatchers.IO) {
        if (denoiser != null) {
            try {
                Timber.d("Applying speech denoiser")
                val denoisedAudio = denoiser.run(waveData.samples, waveData.sampleRate)
                Timber.d("Denoised audio: ${denoisedAudio.samples.size} samples at ${denoisedAudio.sampleRate} Hz")
                Pair(denoisedAudio.samples, denoisedAudio.sampleRate)
            } catch (e: Exception) {
                Timber.w(e, "Denoising failed, using original audio")
                Pair(waveData.samples, waveData.sampleRate)
            }
        } else {
            Pair(waveData.samples, waveData.sampleRate)
        }
    }

}
