package com.bmdstudios.flit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmdstudios.flit.data.database.dao.CategoryDao
import com.bmdstudios.flit.data.database.dao.NoteCategoryDao
import com.bmdstudios.flit.data.database.dao.NoteDao
import com.bmdstudios.flit.data.database.dao.RelationshipDao
import com.bmdstudios.flit.data.database.entity.CategoryEntity
import com.bmdstudios.flit.data.database.entity.NoteCategoryCrossRef
import com.bmdstudios.flit.data.database.entity.NoteEntity
import com.bmdstudios.flit.data.database.entity.RelationshipEntity
import com.bmdstudios.flit.data.database.model.NoteStatus
import com.bmdstudios.flit.data.database.model.RelationshipType
import com.bmdstudios.flit.ui.util.NoteTitleExtractor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for managing notes.
 * Observes notes from the database and provides state for the UI.
 */
/**
 * Data class for displaying relationships with related note information.
 */
data class RelationshipDisplay(
    val relationship: RelationshipEntity,
    val relatedNote: NoteEntity
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    val noteDao: NoteDao,
    private val noteCategoryDao: NoteCategoryDao,
    private val categoryDao: CategoryDao,
    private val relationshipDao: RelationshipDao
) : ViewModel() {

    /**
     * State flow of all notes, ordered by last updated (newest first).
     */
    val notes: StateFlow<List<NoteEntity>> = noteDao.getAllNotesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * State flow for tracking which note to append to.
     * null when not in appending mode, otherwise contains the note ID to append to.
     */
    private val _appendingNoteId = MutableStateFlow<Long?>(null)
    val appendingNoteId: StateFlow<Long?> = _appendingNoteId.asStateFlow()

    init {
        Timber.d("NotesViewModel initialized")
    }

    /**
     * Starts appending mode for the specified note.
     * New notes created will have a "Follows On" relationship with this note.
     */
    fun startAppending(noteId: Long) {
        _appendingNoteId.value = noteId
        Timber.d("Started appending to note $noteId")
    }

    /**
     * Stops appending mode.
     */
    fun stopAppending() {
        _appendingNoteId.value = null
        Timber.d("Stopped appending")
    }

    /**
     * Deletes a note from the database and its associated recording file if it exists.
     */
    suspend fun deleteNote(noteId: Long, recordingPath: String?) {
        withContext(Dispatchers.IO) {
            try {
                // Delete the note from database
                noteDao.deleteNoteById(noteId)
                Timber.i("Note deleted successfully: $noteId")

                // Delete the recording file if it exists
                recordingPath?.let { path ->
                    try {
                        val file = File(path)
                        if (file.exists()) {
                            if (file.delete()) {
                                Timber.i("Recording file deleted successfully: $path")
                            } else {
                                Timber.w("Failed to delete recording file: $path")
                            }
                        } else {
                            Timber.d("Recording file does not exist: $path")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error deleting recording file: $path")
                        // Continue even if file deletion fails
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting note: $noteId")
                throw e
            }
        }
    }

    /**
     * Gets a flow of categories for a specific note.
     */
    fun getCategoriesForNoteFlow(noteId: Long): Flow<List<CategoryEntity>> {
        return noteCategoryDao.getCategoriesForNoteFlow(noteId)
    }

    /**
     * Gets a flow of all available categories.
     */
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategoriesFlow()
    }

    /**
     * Gets a flow of a category by its ID.
     */
    fun getCategoryByIdFlow(categoryId: Long): Flow<CategoryEntity?> {
        return categoryDao.getCategoryByIdFlow(categoryId)
    }

    /**
     * Gets a flow of notes for a specific category.
     */
    fun getNotesForCategoryFlow(categoryId: Long): Flow<List<NoteEntity>> {
        return noteCategoryDao.getNotesForCategoryFlow(categoryId)
    }

    /**
     * Gets a flow of notes matching both text search query and optional category filter.
     * If categoryId is null, searches all notes. If provided, filters by category first then by text.
     */
    fun searchNotesWithCategoryFlow(query: String, categoryId: Long?): Flow<List<NoteEntity>> {
        return if (categoryId == null) {
            // No category filter, just text search
            noteDao.searchNotesFlow(query)
        } else {
            // Filter by category first, then filter by text in memory
            getNotesForCategoryFlow(categoryId)
                .flatMapLatest { categoryNotes ->
                    flow {
                        val filtered = if (query.isBlank()) {
                            categoryNotes
                        } else {
                            categoryNotes.filter { note ->
                                note.title.contains(query, ignoreCase = true) ||
                                note.text.contains(query, ignoreCase = true)
                            }
                        }
                        emit(filtered)
                    }
                }
        }
    }

    /**
     * Adds a category to a note.
     */
    suspend fun addCategoryToNote(noteId: Long, categoryId: Long) {
        withContext(Dispatchers.IO) {
            try {
                val crossRef = NoteCategoryCrossRef(
                    note_id = noteId,
                    category_id = categoryId
                )
                noteCategoryDao.insertNoteCategory(crossRef)
                Timber.i("Category $categoryId added to note $noteId")
            } catch (e: Exception) {
                Timber.e(e, "Error adding category $categoryId to note $noteId")
                throw e
            }
        }
    }

    /**
     * Removes a category from a note.
     */
    suspend fun removeCategoryFromNote(noteId: Long, categoryId: Long) {
        withContext(Dispatchers.IO) {
            try {
                noteCategoryDao.deleteNoteCategory(noteId, categoryId)
                Timber.i("Category $categoryId removed from note $noteId")
            } catch (e: Exception) {
                Timber.e(e, "Error removing category $categoryId from note $noteId")
                throw e
            }
        }
    }

    /**
     * Gets a flow of relationships for a specific note.
     */
    fun getRelationshipsForNoteFlow(noteId: Long): Flow<List<RelationshipEntity>> {
        return relationshipDao.getRelationshipsByNoteIdFlow(noteId)
    }

    /**
     * Gets a flow of relationships with their related note information for display.
     */
    fun getRelationshipsWithNotesFlow(noteId: Long): Flow<List<RelationshipDisplay>> {
        return relationshipDao.getRelationshipsByNoteIdFlow(noteId)
            .flatMapLatest { relationships ->
                flow {
                    val displays = withContext(Dispatchers.IO) {
                        relationships.mapNotNull { relationship ->
                            val relatedNoteId = if (relationship.note_a_id == noteId) {
                                relationship.note_b_id
                            } else {
                                relationship.note_a_id
                            }
                            val relatedNote = noteDao.getNoteById(relatedNoteId)
                            relatedNote?.let { RelationshipDisplay(relationship, it) }
                        }
                    }
                    emit(displays)
                }
            }
    }

    /**
     * Adds a relationship between two notes.
     * Always sets note_a_id to the current note and note_b_id to the related note.
     */
    suspend fun addRelationship(noteId: Long, relatedNoteId: Long, type: RelationshipType) {
        withContext(Dispatchers.IO) {
            try {
                // Check if relationship already exists
                val existing = relationshipDao.getRelationshipBetweenNotesWithType(
                    noteId,
                    relatedNoteId,
                    type
                )
                if (existing != null) {
                    Timber.d("Relationship already exists between note $noteId and $relatedNoteId with type $type")
                    return@withContext
                }

                val relationship = RelationshipEntity(
                    note_a_id = noteId,
                    note_b_id = relatedNoteId,
                    type = type,
                    created_at = System.currentTimeMillis()
                )
                relationshipDao.insertRelationship(relationship)
                Timber.i("Relationship added: note $noteId -> note $relatedNoteId (type: $type)")
            } catch (e: Exception) {
                Timber.e(e, "Error adding relationship between note $noteId and $relatedNoteId")
                throw e
            }
        }
    }

    /**
     * Removes a relationship by its ID.
     */
    suspend fun removeRelationship(relationshipId: Long) {
        withContext(Dispatchers.IO) {
            try {
                relationshipDao.deleteRelationshipById(relationshipId)
                Timber.i("Relationship $relationshipId removed")
            } catch (e: Exception) {
                Timber.e(e, "Error removing relationship $relationshipId")
                throw e
            }
        }
    }

    /**
     * Creates a note from text and saves it to the database.
     * Extracts title from the first 5 words of the text.
     * If appending mode is active, creates a "Follows On" relationship with the appending note.
     *
     * @param text The text content for the note
     * @return The ID of the created note, or null if creation failed
     */
    suspend fun createNoteFromText(text: String): Long? {
        if (text.isBlank()) {
            Timber.w("Attempted to create note from blank text")
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                val title = NoteTitleExtractor.extractTitle(text)
                val currentTime = System.currentTimeMillis()

                val note = NoteEntity(
                    title = title,
                    text = text.trim(),
                    recording = null,
                    embedding_vector = null,
                    created_at = currentTime,
                    updated_at = currentTime,
                    status = NoteStatus.DRAFT
                )

                val noteId = noteDao.insertNote(note)
                Timber.i("Note created successfully from text with id: $noteId, title: $title")

                // Check if appending mode is active and create relationship
                val parentNoteId = _appendingNoteId.value
                if (parentNoteId != null) {
                    try {
                        addRelationship(noteId, parentNoteId, RelationshipType.FOLLOWS_ON)
                        Timber.i("Created FOLLOWS_ON relationship: note $noteId -> note $parentNoteId")
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to create relationship for appended note")
                        // Don't fail note creation if relationship creation fails
                    }
                    // Clear appending state after relationship is created
                    _appendingNoteId.value = null
                }

                noteId
            } catch (e: Exception) {
                Timber.e(e, "Failed to create note from text")
                null
            }
        }
    }
}
