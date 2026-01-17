package com.bmdstudios.flit.utils.audio

/**
 * Constants related to audio processing and transcription.
 */
object AudioConstants {
    // Audio feature configuration
    const val SAMPLE_RATE = 16000
    const val FEATURE_DIM = 80
    const val DITHER = 0.0f
    
    // WAV file header
    const val WAV_HEADER_SIZE = 44
    const val WAV_RIFF_HEADER = "RIFF"
    const val WAV_WAVE_HEADER = "WAVE"
    const val WAV_FMT_CHUNK = "fmt "
    const val WAV_DATA_CHUNK = "data"
    const val WAV_FMT_SUBCHUNK_SIZE = 16
    const val WAV_AUDIO_FORMAT_PCM = 1
    
    // File size limits
    const val MAX_FILE_SIZE_BYTES = 100L * 1024 * 1024 // 100 MB
}
