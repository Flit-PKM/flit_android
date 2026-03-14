package com.bmdstudios.flit.ui.viewmodel

import com.bmdstudios.flit.data.database.dao.CategoryDao
import com.bmdstudios.flit.data.database.dao.ChunkDao
import com.bmdstudios.flit.data.database.dao.NoteCategoryDao
import com.bmdstudios.flit.data.database.NoteWriter
import com.bmdstudios.flit.data.database.dao.NoteDao
import com.bmdstudios.flit.data.database.dao.NotesearchDao
import com.bmdstudios.flit.data.database.dao.RelationshipDao
import com.bmdstudios.flit.data.sync.SyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * Unit tests for [NotesViewModel] appending state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val noteDao: NoteDao = mockk(relaxed = true)
    private val chunkDao: ChunkDao = mockk(relaxed = true)
    private val noteCategoryDao: NoteCategoryDao = mockk(relaxed = true)
    private val categoryDao: CategoryDao = mockk(relaxed = true)
    private val relationshipDao: RelationshipDao = mockk(relaxed = true)
    private val notesearchDao: NotesearchDao = mockk(relaxed = true)
    private val noteWriter: NoteWriter = mockk(relaxed = true)
    private val syncScheduler: SyncScheduler = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { noteDao.getAllNotesFlow() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun startAppending_setsAppendingNoteId() = runTest(testDispatcher) {
        val viewModel = NotesViewModel(
            noteDao = noteDao,
            chunkDao = chunkDao,
            noteCategoryDao = noteCategoryDao,
            categoryDao = categoryDao,
            relationshipDao = relationshipDao,
            notesearchDao = notesearchDao,
            noteWriter = noteWriter,
            syncScheduler = syncScheduler
        )
        advanceUntilIdle()

        viewModel.startAppending(42L)

        assertEquals(42L, viewModel.appendingNoteId.value)
    }

    @Test
    fun stopAppending_clearsAppendingNoteId() = runTest(testDispatcher) {
        val viewModel = NotesViewModel(
            noteDao = noteDao,
            chunkDao = chunkDao,
            noteCategoryDao = noteCategoryDao,
            categoryDao = categoryDao,
            relationshipDao = relationshipDao,
            notesearchDao = notesearchDao,
            noteWriter = noteWriter,
            syncScheduler = syncScheduler
        )
        advanceUntilIdle()
        viewModel.startAppending(1L)

        viewModel.stopAppending()

        assertEquals(null, viewModel.appendingNoteId.value)
    }

    @Test
    fun scheduleSyncAfterMutation_callsSyncScheduler() = runTest(testDispatcher) {
        val viewModel = NotesViewModel(
            noteDao = noteDao,
            chunkDao = chunkDao,
            noteCategoryDao = noteCategoryDao,
            categoryDao = categoryDao,
            relationshipDao = relationshipDao,
            notesearchDao = notesearchDao,
            noteWriter = noteWriter,
            syncScheduler = syncScheduler
        )
        advanceUntilIdle()

        viewModel.scheduleSyncAfterMutation()

        verify(exactly = 1) { syncScheduler.scheduleSyncAfterMutation() }
    }
}
