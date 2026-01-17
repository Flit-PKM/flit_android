package com.bmdstudios.flit.di

import android.content.Context
import com.bmdstudios.flit.config.AppConfig
import com.bmdstudios.flit.config.AudioConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for configuration-related dependencies.
 * Provides AppConfig, ModelConfig, and AudioConfig instances.
 */
@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    /**
     * Provides AppConfig instance.
     */
    @Provides
    @Singleton
    fun provideAppConfig(@ApplicationContext context: Context): AppConfig {
        return AppConfig.createDefault(context)
    }

    /**
     * Provides AudioConfig instance from AppConfig.
     */
    @Provides
    @Singleton
    fun provideAudioConfig(appConfig: AppConfig): AudioConfig {
        return appConfig.audioConfig
    }
}
