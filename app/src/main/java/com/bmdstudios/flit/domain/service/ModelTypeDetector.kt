package com.bmdstudios.flit.domain.service

import com.bmdstudios.flit.utils.model.FileMatcher
import com.bmdstudios.flit.utils.model.ModelConstants
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for detecting model types based on file names.
 * Centralizes model type detection logic to eliminate duplication.
 */
@Singleton
class ModelTypeDetector @Inject constructor(
    private val fileMatcher: FileMatcher
) {

    /**
     * Detects if the model is a Whisper model based on file structure.
     * Whisper models have encoder + decoder + tokens, but NO joiner.
     * This structural detection works with any naming convention.
     * 
     * @param files List of file names in the model directory
     * @return true if the model appears to be a Whisper model, false otherwise
     */
    fun detectWhisperModel(files: List<String>): Boolean {
        // Whisper models have encoder + decoder + tokens, but NO joiner
        val hasEncoder = fileMatcher.hasModelFile(files, ModelConstants.ENCODER_PATTERN)
        val hasDecoder = fileMatcher.hasModelFile(files, ModelConstants.DECODER_PATTERN)
        val hasTokens = fileMatcher.hasModelFile(files, ModelConstants.TOKENS_PATTERN, ModelConstants.TXT_EXTENSION)
        val hasJoiner = fileMatcher.hasModelFile(files, ModelConstants.JOINER_PATTERN)
        
        // Whisper: encoder + decoder + tokens, but no joiner
        return hasEncoder && hasDecoder && hasTokens && !hasJoiner
    }

    /**
     * Detects if the model is a SenseVoice model based on file names.
     * 
     * @param files List of file names in the model directory
     * @return true if the model appears to be a SenseVoice model, false otherwise
     */
    fun detectSenseVoiceModel(files: List<String>): Boolean {
        val hasModelOnnx = fileMatcher.hasModelFile(files, "model")
        val hasTokens = fileMatcher.hasModelFile(files, ModelConstants.TOKENS_PATTERN, ModelConstants.TXT_EXTENSION)
        // SenseVoice models are uniquely identified by having model.onnx/model.int8.onnx
        // and tokens.txt but NOT having encoder/decoder/joiner files
        val hasEncoder = fileMatcher.hasModelFile(files, ModelConstants.ENCODER_PATTERN)
        val hasDecoder = fileMatcher.hasModelFile(files, ModelConstants.DECODER_PATTERN)
        val hasJoiner = fileMatcher.hasModelFile(files, ModelConstants.JOINER_PATTERN)
        
        // SenseVoice: has model file + tokens, but NOT multi-file structure
        return hasModelOnnx && hasTokens && !hasEncoder && !hasDecoder && !hasJoiner
    }

    /**
     * Detects if the model is a transducer model based on file names.
     * This checks for encoder, decoder, and joiner files typical of transducer models.
     * 
     * @param files List of file names in the model directory
     * @return true if the model appears to be a transducer model, false otherwise
     */
    fun detectTransducerModel(files: List<String>): Boolean {
        // Don't treat as transducer if it's SenseVoice or Whisper
        if (detectSenseVoiceModel(files) || detectWhisperModel(files)) {
            return false
        }
        // Transducer models have encoder, decoder, and joiner files
        val hasEncoder = fileMatcher.hasModelFile(files, ModelConstants.ENCODER_PATTERN)
        val hasDecoder = fileMatcher.hasModelFile(files, ModelConstants.DECODER_PATTERN)
        val hasJoiner = fileMatcher.hasModelFile(files, ModelConstants.JOINER_PATTERN)
        return hasEncoder && hasDecoder && hasJoiner
    }
}
