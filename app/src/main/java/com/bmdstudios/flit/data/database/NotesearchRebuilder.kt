package com.bmdstudios.flit.data.database

import android.content.Context
import com.bmdstudios.flit.data.database.dao.NoteDao
import com.bmdstudios.flit.data.database.dao.NotesearchDao
import com.bmdstudios.flit.data.database.entity.NoteSearchEntity
import com.bmdstudios.flit.data.search.SearchNormalizer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREF_NAME = "notesearch_rebuilder"
private const val KEY_REBUILT_V4 = "rebuilt_v4"

/**
 * One-time rebuild of notesearch from notes after migration to DB version 4.
 * Uses a preference flag so rebuild runs only once per install.
 */
@Singleton
class NotesearchRebuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteDao: NoteDao,
    private val notesearchDao: NotesearchDao
) {

    /**
     * If notesearch has not been rebuilt yet, populates it from all non-deleted notes.
     * Call from app startup (e.g. MainActivity onCreate on IO dispatcher).
     */
    suspend fun rebuildIfNeeded() {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_REBUILT_V4, false)) return

        val notes = noteDao.getAllNotes()
        for (note in notes) {
            val content = SearchNormalizer.normalize("${note.title} ${note.text}")
            notesearchDao.upsert(NoteSearchEntity(note_id = note.id, content = content))
        }
        prefs.edit().putBoolean(KEY_REBUILT_V4, true).apply()
    }
}
