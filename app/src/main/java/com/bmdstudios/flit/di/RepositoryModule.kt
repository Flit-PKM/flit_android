package com.bmdstudios.flit.di

import android.content.Context
import androidx.room.Room
import com.bmdstudios.flit.config.AppConfig
import com.bmdstudios.flit.data.database.FlitDatabase
import com.bmdstudios.flit.data.database.dao.CategoryDao
import com.bmdstudios.flit.data.database.dao.ChunkDao
import com.bmdstudios.flit.data.database.dao.NoteCategoryDao
import com.bmdstudios.flit.data.database.dao.NoteDao
import com.bmdstudios.flit.data.database.dao.RelationshipDao
import com.bmdstudios.flit.data.database.dao.UserDao
import com.bmdstudios.flit.data.repository.AudioRepositoryImpl
import com.bmdstudios.flit.data.repository.ModelRepositoryImpl
import com.bmdstudios.flit.data.repository.SettingsRepository
import com.bmdstudios.flit.domain.repository.AudioRepository
import com.bmdstudios.flit.domain.repository.ModelRepository
import com.bmdstudios.flit.domain.service.ModelTypeDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository dependencies.
 * Provides repository implementations and database access.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * Provides FlitDatabase instance.
     */
    @Provides
    @Singleton
    fun provideFlitDatabase(@ApplicationContext context: Context): FlitDatabase {
        return Room.databaseBuilder(
            context,
            FlitDatabase::class.java,
            "flit_database"
        )
            .fallbackToDestructiveMigration(dropAllTables = true) // For development - remove in production
            .build()
    }

    /**
     * Provides UserDao.
     */
    @Provides
    @Singleton
    fun provideUserDao(database: FlitDatabase): UserDao {
        return database.userDao()
    }

    /**
     * Provides NoteDao.
     */
    @Provides
    @Singleton
    fun provideNoteDao(database: FlitDatabase): NoteDao {
        return database.noteDao()
    }

    /**
     * Provides ChunkDao.
     */
    @Provides
    @Singleton
    fun provideChunkDao(database: FlitDatabase): ChunkDao {
        return database.chunkDao()
    }

    /**
     * Provides RelationshipDao.
     */
    @Provides
    @Singleton
    fun provideRelationshipDao(database: FlitDatabase): RelationshipDao {
        return database.relationshipDao()
    }

    /**
     * Provides CategoryDao.
     */
    @Provides
    @Singleton
    fun provideCategoryDao(database: FlitDatabase): CategoryDao {
        return database.categoryDao()
    }

    /**
     * Provides NoteCategoryDao.
     */
    @Provides
    @Singleton
    fun provideNoteCategoryDao(database: FlitDatabase): NoteCategoryDao {
        return database.noteCategoryDao()
    }

    /**
     * Provides ModelRepository implementation.
     */
    @Provides
    @Singleton
    fun provideModelRepository(
        appConfig: AppConfig,
        modelTypeDetector: ModelTypeDetector
    ): ModelRepository {
        return ModelRepositoryImpl(appConfig.modelConfig, modelTypeDetector)
    }

    /**
     * Provides AudioRepository implementation.
     */
    @Provides
    @Singleton
    fun provideAudioRepository(@ApplicationContext context: Context): AudioRepository {
        return AudioRepositoryImpl(context)
    }

    /**
     * Provides SettingsRepository instance.
     */
    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepository(context)
    }
}
