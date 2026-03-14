package com.bmdstudios.flit.data.database

import com.bmdstudios.flit.data.database.dao.NoteDao
import com.bmdstudios.flit.data.database.dao.NotesearchDao
import com.bmdstudios.flit.data.database.entity.NoteEntity
import com.bmdstudios.flit.data.database.model.NoteStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [NoteWriter]: verifies note writes and notesearch sync.
 */
class NoteWriterTest {

    private val noteDao: NoteDao = mockk(relaxed = true)
    private val notesearchDao: NotesearchDao = mockk(relaxed = true)
    private lateinit var noteWriter: NoteWriter

    @Before
    fun setUp() {
        noteWriter = NoteWriter(noteDao = noteDao, notesearchDao = notesearchDao)
    }

    @Test
    fun insertNote_callsNoteDaoThenUpsertsNotesearch() = runTest {
        val note = NoteEntity(
            title = "Hello",
            text = "World",
            recording = null,
            embedding_vector = null,
            created_at = 1L,
            updated_at = 1L,
            workflow_status = NoteStatus.DRAFT
        )
        coEvery { noteDao.insertNote(note) } returns 42L

        val id = noteWriter.insertNote(note)

        assertEquals(42L, id)
        coVerify { noteDao.insertNote(note) }
        coVerify {
            notesearchDao.upsert(
                match {
                    it.note_id == 42L && it.content == "hello world"
                }
            )
        }
    }

    @Test
    fun updateNote_callsNoteDaoThenUpsertsNotesearch() = runTest {
        val note = NoteEntity(
            id = 10L,
            title = "Title",
            text = "Body",
            recording = null,
            embedding_vector = null,
            created_at = 1L,
            updated_at = 2L,
            workflow_status = NoteStatus.DRAFT
        )

        noteWriter.updateNote(note)

        coVerify { noteDao.updateNote(note) }
        coVerify {
            notesearchDao.upsert(
                match {
                    it.note_id == 10L && it.content == "title body"
                }
            )
        }
    }

    @Test
    fun softDeleteNoteById_callsNoteDaoThenDeletesNotesearch() = runTest {
        noteWriter.softDeleteNoteById(5L, 100L)

        coVerify { noteDao.softDeleteNoteById(5L, 100L) }
        coVerify { notesearchDao.deleteByNoteId(5L) }
    }
}
