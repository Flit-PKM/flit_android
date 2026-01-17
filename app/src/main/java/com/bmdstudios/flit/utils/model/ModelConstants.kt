package com.bmdstudios.flit.utils.model

/**
 * Constants related to model configuration and file patterns.
 */
object ModelConstants {
    // Model file patterns
    const val ENCODER_PATTERN = "encoder"
    const val DECODER_PATTERN = "decoder"
    const val JOINER_PATTERN = "joiner"
    const val TOKENS_PATTERN = "tokens"
    const val WHISPER_PATTERN = "whisper"
    const val SENSE_VOICE_PATTERN = "sense"
    const val GTCRN_PATTERN = "gtcrn"
    const val CT_TRANSFORMER_PATTERN = "ct"
    const val TRANSFORMER_PATTERN = "transformer"
    
    // Model file extensions
    const val ONNX_EXTENSION = "onnx"
    const val TXT_EXTENSION = "txt"
    
    // Whisper model file names
    const val TINY_EN_ENCODER_INT8 = "tiny.en-encoder.int8.onnx"
    const val TINY_EN_ENCODER = "tiny.en-encoder.onnx"
    const val BASE_EN_ENCODER_INT8 = "base.en-encoder.int8.onnx"
    const val BASE_EN_ENCODER = "base.en-encoder.onnx"
    const val TINY_EN_DECODER_INT8 = "tiny.en-decoder.int8.onnx"
    const val TINY_EN_DECODER = "tiny.en-decoder.onnx"
    const val BASE_EN_DECODER_INT8 = "base.en-decoder.int8.onnx"
    const val BASE_EN_DECODER = "base.en-decoder.onnx"
    const val TINY_EN_TOKENS = "tiny.en-tokens.txt"
    const val BASE_EN_TOKENS = "base.en-tokens.txt"
    const val TOKENS_FILE = "tokens.txt"
    
    // Transducer model file names
    const val ENCODER_EPOCH_99 = "encoder-epoch-99-avg-1.onnx"
    const val DECODER_EPOCH_99 = "decoder-epoch-99-avg-1.onnx"
    const val JOINER_EPOCH_99 = "joiner-epoch-99-avg-1.onnx"
    
    // Punctuation model file names
    const val MODEL_ONNX = "model.onnx"
    const val MODEL_INT8_ONNX = "model.int8.onnx"
    
    // Model types
    const val MODEL_TYPE_WHISPER = "whisper"
    const val MODEL_TYPE_TRANSDUCER = "transducer"
    const val MODEL_TYPE_SENSE_VOICE = "sense_voice"
    
    // Decoding method
    const val DECODING_METHOD_GREEDY_SEARCH = "greedy_search"
    
    // Whisper model configuration
    const val WHISPER_LANGUAGE = "en"
    const val WHISPER_TASK = "transcribe"
}
