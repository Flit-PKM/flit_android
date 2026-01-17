package com.bmdstudios.flit.di

import com.bmdstudios.flit.config.AppConfig
import com.bmdstudios.flit.utils.HuggingFaceModelDownloader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module for network-related dependencies.
 * Provides OkHttpClient and downloader instances.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Provides OkHttpClient with configured timeouts and retry logic.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(appConfig: AppConfig): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(appConfig.networkConfig.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(appConfig.networkConfig.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(appConfig.networkConfig.writeTimeoutSeconds, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Provides HuggingFaceModelDownloader instance.
     */
    @Provides
    @Singleton
    fun provideHuggingFaceModelDownloader(
        okHttpClient: OkHttpClient,
        appConfig: AppConfig
    ): HuggingFaceModelDownloader {
        return HuggingFaceModelDownloader(okHttpClient, appConfig.networkConfig)
    }
}
