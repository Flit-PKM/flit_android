package com.bmdstudios.flit.config

import android.content.Context
import com.bmdstudios.flit.BuildConfig
import java.io.File

/**
 * Application-wide configuration.
 * Aggregates all configuration components and provides a single entry point.
 */
data class AppConfig(
    val audioConfig: AudioConfig = AudioConfig.default(),
    val modelConfig: ModelConfig,
    val networkConfig: NetworkConfig = NetworkConfig.default(),
    val backendBaseUrl: String = "http://localhost:8000",
    /** HTTPS URL opened in the browser for Flit Core account / connection code (not the API base). */
    val flitCoreWebLoginUrl: String = "https://core.flit-pkm.com/?redirect=login"
) {
    companion object {
        const val ONBOARDING_REVISION: Int = 1

        /**
         * Creates the default application configuration.
         */
        @JvmStatic
        fun createDefault(context: Context): AppConfig {
            val modelConfig = ModelConfig.createDefault(context)
            return AppConfig(
                modelConfig = modelConfig,
                backendBaseUrl = BuildConfig.BACKEND_BASE_URL,
                flitCoreWebLoginUrl = BuildConfig.FLIT_CORE_WEB_LOGIN_URL
            )
        }
    }
}

/**
 * Configuration for network operations.
 */
data class NetworkConfig(
    val connectTimeoutSeconds: Long = DEFAULT_CONNECT_TIMEOUT_SECONDS,
    val readTimeoutSeconds: Long = DEFAULT_READ_TIMEOUT_SECONDS,
    val writeTimeoutSeconds: Long = DEFAULT_WRITE_TIMEOUT_SECONDS,
    val maxRetries: Int = DEFAULT_MAX_RETRIES,
    val retryBackoffMultiplier: Double = DEFAULT_RETRY_BACKOFF_MULTIPLIER,
    val progressUpdateThrottleMs: Long = DEFAULT_PROGRESS_UPDATE_THROTTLE_MS
) {
    init {
        require(connectTimeoutSeconds > 0) { "Connect timeout must be positive" }
        require(readTimeoutSeconds > 0) { "Read timeout must be positive" }
        require(writeTimeoutSeconds > 0) { "Write timeout must be positive" }
        require(maxRetries >= 0) { "Max retries must be non-negative" }
        require(retryBackoffMultiplier > 1.0) { "Retry backoff multiplier must be greater than 1.0" }
        require(progressUpdateThrottleMs >= 0) { "Progress update throttle must be non-negative" }
    }

    companion object {
        private const val DEFAULT_CONNECT_TIMEOUT_SECONDS = 30L
        private const val DEFAULT_READ_TIMEOUT_SECONDS = 60L
        private const val DEFAULT_WRITE_TIMEOUT_SECONDS = 60L
        private const val DEFAULT_MAX_RETRIES = 3
        private const val DEFAULT_RETRY_BACKOFF_MULTIPLIER = 2.0
        private const val DEFAULT_PROGRESS_UPDATE_THROTTLE_MS = 100L

        @JvmStatic
        fun default(): NetworkConfig = NetworkConfig()
    }
}
