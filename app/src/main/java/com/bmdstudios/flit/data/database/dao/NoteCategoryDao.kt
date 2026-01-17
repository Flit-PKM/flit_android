package com.bmdstudios.flit.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bmdstudios.flit.data.database.entity.CategoryEntity
import com.bmdstudios.flit.data.database.entity.NoteCategoryCrossRef
import com.bmdstudios.flit.data.database.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for note-category junction table operations.
 */
@Dao
interface NoteCategoryDao {
    @Query("SELECT * FROM category INNER JOIN note_categories ON category.id = note_categories.category_id WHERE note_categories.note_id = :noteId")
    suspend fun getCategoriesForNote(noteId: Long): List<CategoryEntity>

    @Query("SELECT * FROM category INNER JOIN note_categories ON category.id = note_categories.category_id WHERE note_categories.note_id = :noteId")
    fun getCategoriesForNoteFlow(noteId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM notes INNER JOIN note_categories ON notes.id = note_categories.note_id WHERE note_categories.category_id = :categoryId")
    suspend fun getNotesForCategory(categoryId: Long): List<NoteEntity>

    @Query("SELECT * FROM notes INNER JOIN note_categories ON notes.id = note_categories.note_id WHERE note_categories.category_id = :categoryId")
    fun getNotesForCategoryFlow(categoryId: Long): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteCategory(crossRef: NoteCategoryCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteCategories(crossRefs: List<NoteCategoryCrossRef>)

    @Query("DELETE FROM note_categories WHERE note_id = :noteId AND category_id = :categoryId")
    suspend fun deleteNoteCategory(noteId: Long, categoryId: Long)

    @Query("DELETE FROM note_categories WHERE note_id = :noteId")
    suspend fun deleteAllCategoriesForNote(noteId: Long)

    @Query("DELETE FROM note_categories WHERE category_id = :categoryId")
    suspend fun deleteAllNotesForCategory(categoryId: Long)

    @Query("SELECT COUNT(*) FROM note_categories WHERE note_id = :noteId")
    suspend fun getCategoryCountForNote(noteId: Long): Int

    @Query("SELECT COUNT(*) FROM note_categories WHERE category_id = :categoryId")
    suspend fun getNoteCountForCategory(categoryId: Long): Int
}
