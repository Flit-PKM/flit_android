package com.bmdstudios.flit.config

/**
 * Configuration for audio recording operations.
 * Centralizes all audio-related constants and settings.
 */
data class AudioConfig(
    val sampleRate: Int = DEFAULT_SAMPLE_RATE,
    val channelConfig: Int = DEFAULT_CHANNEL_CONFIG,
    val audioFormat: Int = DEFAULT_AUDIO_FORMAT,
    val bitsPerSample: Int = DEFAULT_BITS_PER_SAMPLE,
    val bufferMultiplier: Int = DEFAULT_BUFFER_MULTIPLIER,
    val threadJoinTimeoutMs: Long = DEFAULT_THREAD_JOIN_TIMEOUT_MS
) {
    init {
        require(sampleRate > 0) { "Sample rate must be positive" }
        require(bitsPerSample > 0) { "Bits per sample must be positive" }
        require(bufferMultiplier > 0) { "Buffer multiplier must be positive" }
        require(threadJoinTimeoutMs > 0) { "Thread join timeout must be positive" }
    }

    val bytesPerSample: Int
        get() = bitsPerSample / 8

    companion object {
        private const val DEFAULT_SAMPLE_RATE = 16000
        private const val DEFAULT_CHANNEL_CONFIG = android.media.AudioFormat.CHANNEL_IN_MONO
        private const val DEFAULT_AUDIO_FORMAT = android.media.AudioFormat.ENCODING_PCM_16BIT
        private const val DEFAULT_BITS_PER_SAMPLE = 16
        private const val DEFAULT_BUFFER_MULTIPLIER = 2
        private const val DEFAULT_THREAD_JOIN_TIMEOUT_MS = 1000L

        /**
         * Default audio configuration for standard speech recognition.
         */
        @JvmStatic
        fun default(): AudioConfig = AudioConfig()
    }
}
