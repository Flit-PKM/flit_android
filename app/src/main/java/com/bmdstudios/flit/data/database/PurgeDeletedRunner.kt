package com.bmdstudios.flit.data.database

import com.bmdstudios.flit.data.database.dao.CategoryDao
import com.bmdstudios.flit.data.database.dao.NoteCategoryDao
import com.bmdstudios.flit.data.database.dao.NoteDao
import com.bmdstudios.flit.data.database.dao.NotesearchDao
import com.bmdstudios.flit.data.database.dao.RelationshipDao
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** 6 weeks in milliseconds. */
private const val SIX_WEEKS_MS = 6L * 7 * 24 * 60 * 60 * 1000

/**
 * Runs purge of deleted rows across all tables.
 * Rows with is_deleted = true and updated_at older than 6 weeks are permanently removed.
 * Also removes orphan notesearch rows (note_id no longer in notes).
 */
@Singleton
class PurgeDeletedRunner @Inject constructor(
    private val noteDao: NoteDao,
    private val categoryDao: CategoryDao,
    private val relationshipDao: RelationshipDao,
    private val noteCategoryDao: NoteCategoryDao,
    private val notesearchDao: NotesearchDao
) {
    /**
     * Purges all rows where is_deleted = true and updated_at < (now - 6 weeks).
     * Removes notesearch rows whose note has been purged. Call from app startup or periodically.
     */
    suspend fun purge() {
        val cutoffMs = System.currentTimeMillis() - SIX_WEEKS_MS
        try {
            noteDao.purgeDeletedOlderThan(cutoffMs)
            categoryDao.purgeDeletedOlderThan(cutoffMs)
            relationshipDao.purgeDeletedOlderThan(cutoffMs)
            noteCategoryDao.purgeDeletedOlderThan(cutoffMs)
            notesearchDao.deleteOrphans()
            Timber.d("Purge of deleted rows older than 6 weeks completed")
        } catch (e: Exception) {
            Timber.e(e, "Purge of deleted rows failed")
        }
    }
}
