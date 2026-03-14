package com.bmdstudios.flit.data.repository

import com.bmdstudios.flit.data.api.FlitApiService
import com.bmdstudios.flit.data.database.dao.CategoryDao
import com.bmdstudios.flit.data.database.dao.ChunkDao
import com.bmdstudios.flit.data.database.dao.NoteCategoryDao
import com.bmdstudios.flit.data.database.NoteWriter
import com.bmdstudios.flit.data.database.dao.NoteDao
import com.bmdstudios.flit.data.database.dao.RelationshipDao
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [SyncRepository.runSync] (auth and result behaviour).
 */
class SyncRepositoryTest {

    private val api: FlitApiService = mockk(relaxed = true)
    private val settings: SettingsRepository = mockk(relaxed = true)
    private val noteDao: NoteDao = mockk(relaxed = true)
    private val chunkDao: ChunkDao = mockk(relaxed = true)
    private val categoryDao: CategoryDao = mockk(relaxed = true)
    private val relationshipDao: RelationshipDao = mockk(relaxed = true)
    private val noteCategoryDao: NoteCategoryDao = mockk(relaxed = true)
    private val noteWriter: NoteWriter = mockk(relaxed = true)

    private val repository = SyncRepository(
        api = api,
        settings = settings,
        noteDao = noteDao,
        chunkDao = chunkDao,
        categoryDao = categoryDao,
        relationshipDao = relationshipDao,
        noteCategoryDao = noteCategoryDao,
        noteWriter = noteWriter
    )

    @Test
    fun runSync_whenNoAccessToken_returnsNotAuthenticated() = runTest {
        coEvery { settings.getAccessToken() } returns null

        val result = repository.runSync()

        assertEquals(SyncRepository.SyncResult.NotAuthenticated, result)
    }
}
