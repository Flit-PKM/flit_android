package com.bmdstudios.flit.di

import android.content.Context
import androidx.room.Room
import com.bmdstudios.flit.config.AppConfig
import com.bmdstudios.flit.data.database.FlitDatabase
import com.bmdstudios.flit.data.database.MIGRATION_3_4
import com.bmdstudios.flit.data.database.MIGRATION_4_5
import com.bmdstudios.flit.data.database.NoteWriter
import com.bmdstudios.flit.data.database.PurgeDeletedRunner
import com.bmdstudios.flit.data.database.dao.CategoryDao
import com.bmdstudios.flit.data.database.dao.NoteCategoryDao
import com.bmdstudios.flit.data.database.dao.NoteDao
import com.bmdstudios.flit.data.database.dao.NotesearchDao
import com.bmdstudios.flit.data.database.dao.RelationshipDao
import com.bmdstudios.flit.data.repository.AudioRepositoryImpl
import com.bmdstudios.flit.data.repository.ModelRepositoryImpl
import com.bmdstudios.flit.data.repository.SettingsRepository
import com.bmdstudios.flit.data.repository.SyncRepository
import com.bmdstudios.flit.data.sync.SyncScheduler
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
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
            .build()
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
     * Provides NotesearchDao for ranked note search.
     */
    @Provides
    @Singleton
    fun provideNotesearchDao(database: FlitDatabase): NotesearchDao {
        return database.notesearchDao()
    }

    /**
     * Provides NoteWriter: single entry point for note insert/update/soft-delete; keeps notesearch in sync.
     */
    @Provides
    @Singleton
    fun provideNoteWriter(noteDao: NoteDao, notesearchDao: NotesearchDao): NoteWriter {
        return NoteWriter(noteDao = noteDao, notesearchDao = notesearchDao)
    }

    /**
     * Provides PurgeDeletedRunner for purging soft-deleted rows older than 6 weeks.
     */
    @Suppress("LongParameterList")
    @Provides
    @Singleton
    fun providePurgeDeletedRunner(
        noteDao: NoteDao,
        categoryDao: CategoryDao,
        relationshipDao: RelationshipDao,
        noteCategoryDao: NoteCategoryDao,
        notesearchDao: NotesearchDao
    ): PurgeDeletedRunner {
        return PurgeDeletedRunner(
            noteDao = noteDao,
            categoryDao = categoryDao,
            relationshipDao = relationshipDao,
            noteCategoryDao = noteCategoryDao,
            notesearchDao = notesearchDao
        )
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

    /**
     * Provides SyncScheduler for background sync (startup, after mutations, every 5 min when foreground).
     */
    @Provides
    @Singleton
    fun provideSyncScheduler(
        syncRepository: SyncRepository,
        settingsRepository: SettingsRepository,
        @ApplicationContext context: Context
    ): SyncScheduler {
        return SyncScheduler(
            syncRepository = syncRepository,
            settings = settingsRepository,
            context = context
        )
    }

}
