package com.bmdstudios.flit.di

import android.content.Context
import com.bmdstudios.flit.config.AppConfig
import com.bmdstudios.flit.data.repository.SettingsRepository
import com.bmdstudios.flit.domain.service.ModelTypeDetector
import com.bmdstudios.flit.utils.AudioTranscriber
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for utility dependencies.
 * Provides AudioTranscriber and other utility instances.
 */
@Module
@InstallIn(SingletonComponent::class)
object UtilityModule {

    /**
     * Provides AudioTranscriber instance.
     */
    @Provides
    @Singleton
    fun provideAudioTranscriber(
        @ApplicationContext context: Context,
        appConfig: AppConfig,
        settingsRepository: SettingsRepository,
        modelTypeDetector: ModelTypeDetector,
        whisperModelConfigBuilder: com.bmdstudios.flit.utils.model.WhisperModelConfigBuilder,
        transducerModelConfigBuilder: com.bmdstudios.flit.utils.model.TransducerModelConfigBuilder,
        senseVoiceModelConfigBuilder: com.bmdstudios.flit.utils.model.SenseVoiceModelConfigBuilder,
        denoiserModelFactory: com.bmdstudios.flit.utils.model.DenoiserModelFactory,
        punctuationModelFactory: com.bmdstudios.flit.utils.model.PunctuationModelFactory,
        textPostProcessor: com.bmdstudios.flit.utils.text.TextPostProcessor
    ): AudioTranscriber {
        return AudioTranscriber(
            context,
            appConfig,
            settingsRepository,
            modelTypeDetector,
            whisperModelConfigBuilder,
            transducerModelConfigBuilder,
            senseVoiceModelConfigBuilder,
            denoiserModelFactory,
            punctuationModelFactory,
            textPostProcessor
        )
    }
}
