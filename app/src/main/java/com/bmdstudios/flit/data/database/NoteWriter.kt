package com.bmdstudios.flit.data.database

import com.bmdstudios.flit.data.database.dao.NoteDao
import com.bmdstudios.flit.data.database.dao.NotesearchDao
import com.bmdstudios.flit.data.database.entity.NoteEntity
import com.bmdstudios.flit.data.database.entity.NoteSearchEntity
import com.bmdstudios.flit.data.search.SearchNormalizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for note writes. Every insert, update, or soft-delete of a note
 * goes through here so that the notesearch table is kept in sync. Callers should use
 * NoteWriter for writes and NoteDao for reads.
 */
@Singleton
class NoteWriter @Inject constructor(
    private val noteDao: NoteDao,
    private val notesearchDao: NotesearchDao
) {

    /**
     * Inserts a note and upserts its notesearch row. Returns the new note id.
     */
    suspend fun insertNote(note: NoteEntity): Long {
        val noteId = noteDao.insertNote(note)
        notesearchDao.upsert(
            NoteSearchEntity(
                note_id = noteId,
                content = SearchNormalizer.normalize("${note.title} ${note.text}")
            )
        )
        return noteId
    }

    /**
     * Updates a note and its notesearch row.
     */
    suspend fun updateNote(note: NoteEntity) {
        noteDao.updateNote(note)
        notesearchDao.upsert(
            NoteSearchEntity(
                note_id = note.id,
                content = SearchNormalizer.normalize("${note.title} ${note.text}")
            )
        )
    }

    /**
     * Soft-deletes a note and hard-deletes its notesearch row.
     */
    suspend fun softDeleteNoteById(id: Long, updatedAt: Long) {
        noteDao.softDeleteNoteById(id, updatedAt)
        notesearchDao.deleteByNoteId(id)
    }
}
