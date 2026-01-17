package com.bmdstudios.flit.utils

/**
 * General application constants.
 */
object Constants {
    // Download buffer size
    const val DOWNLOAD_BUFFER_SIZE = 8192
    
    // Progress update thresholds
    const val PROGRESS_LOG_INTERVAL_BYTES = 1_000_000 // 1 MB
    const val PROGRESS_UPDATE_PERCENT_INTERVAL = 10
    const val PROGRESS_UPDATE_PERCENT_INTERVAL_DIRECT = 25
    
    // File naming
    const val RECORDING_FILE_PREFIX = "recording_"
    const val PCM_FILE_EXTENSION = "pcm"
    const val WAV_FILE_EXTENSION = "wav"
    
    // Default model filename
    const val DEFAULT_MODEL_FILENAME = "model.onnx"
}
