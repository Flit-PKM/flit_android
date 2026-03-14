package com.bmdstudios.flit.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bmdstudios.flit.data.database.entity.CategoryEntity
import com.bmdstudios.flit.data.database.entity.NoteCategoryCrossRef
import com.bmdstudios.flit.data.database.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for note-category junction table operations.
 * Visible queries exclude rows with is_deleted = true on both note_categories and related entities.
 */
@Dao
interface NoteCategoryDao {
    @Query("SELECT * FROM category INNER JOIN note_categories ON category.id = note_categories.category_id WHERE note_categories.note_id = :noteId AND note_categories.is_deleted = 0 AND category.is_deleted = 0")
    suspend fun getCategoriesForNote(noteId: Long): List<CategoryEntity>

    @Query("SELECT * FROM category INNER JOIN note_categories ON category.id = note_categories.category_id WHERE note_categories.note_id = :noteId AND note_categories.is_deleted = 0 AND category.is_deleted = 0")
    fun getCategoriesForNoteFlow(noteId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM notes INNER JOIN note_categories ON notes.id = note_categories.note_id WHERE note_categories.category_id = :categoryId AND note_categories.is_deleted = 0 AND notes.is_deleted = 0")
    suspend fun getNotesForCategory(categoryId: Long): List<NoteEntity>

    @Query("SELECT * FROM notes INNER JOIN note_categories ON notes.id = note_categories.note_id WHERE note_categories.category_id = :categoryId AND note_categories.is_deleted = 0 AND notes.is_deleted = 0")
    fun getNotesForCategoryFlow(categoryId: Long): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteCategory(crossRef: NoteCategoryCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteCategories(crossRefs: List<NoteCategoryCrossRef>)

    @Update
    suspend fun updateNoteCategory(crossRef: NoteCategoryCrossRef)

    @Query("UPDATE note_categories SET is_deleted = 1, updated_at = :updatedAt, ver = ver + 1 WHERE note_id = :noteId AND category_id = :categoryId")
    suspend fun softDeleteNoteCategory(noteId: Long, categoryId: Long, updatedAt: Long)

    @Query("UPDATE note_categories SET is_deleted = 1, updated_at = :updatedAt, ver = ver + 1 WHERE note_id = :noteId")
    suspend fun softDeleteAllCategoriesForNote(noteId: Long, updatedAt: Long)

    @Query("DELETE FROM note_categories WHERE note_id = :noteId AND category_id = :categoryId")
    suspend fun deleteNoteCategory(noteId: Long, categoryId: Long)

    @Query("DELETE FROM note_categories WHERE note_id = :noteId")
    suspend fun deleteAllCategoriesForNote(noteId: Long)

    @Query("DELETE FROM note_categories WHERE category_id = :categoryId")
    suspend fun deleteAllNotesForCategory(categoryId: Long)

    @Query("SELECT COUNT(*) FROM note_categories WHERE note_id = :noteId AND is_deleted = 0")
    suspend fun getCategoryCountForNote(noteId: Long): Int

    @Query("SELECT COUNT(*) FROM note_categories WHERE category_id = :categoryId AND is_deleted = 0")
    suspend fun getNoteCountForCategory(categoryId: Long): Int

    @Query("SELECT * FROM note_categories WHERE is_deleted = 0")
    suspend fun getAllNoteCategories(): List<NoteCategoryCrossRef>

    /** All note_category links including soft-deleted, for building sync compare payload. */
    @Query("SELECT * FROM note_categories ORDER BY note_id, category_id")
    suspend fun getAllNoteCategoriesForSync(): List<NoteCategoryCrossRef>

    @Query("DELETE FROM note_categories WHERE is_deleted = 1 AND updated_at < :cutoffMs")
    suspend fun purgeDeletedOlderThan(cutoffMs: Long)
}
