package com.bmdstudios.flit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmdstudios.flit.data.database.dao.NoteDao
import com.bmdstudios.flit.data.database.dao.RelationshipDao
import com.bmdstudios.flit.data.database.entity.NoteEntity
import com.bmdstudios.flit.data.database.entity.RelationshipEntity
import com.bmdstudios.flit.data.database.model.NoteStatus
import com.bmdstudios.flit.data.database.model.RelationshipType
import com.bmdstudios.flit.domain.error.AppError
import com.bmdstudios.flit.domain.error.ErrorHandler
import com.bmdstudios.flit.domain.repository.AudioRepository
import com.bmdstudios.flit.domain.toAppError
import com.bmdstudios.flit.ui.util.NoteTitleExtractor
import com.bmdstudios.flit.utils.AudioTranscriber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import java.io.File

/**
 * UI state for transcription.
 */
sealed class TranscriptionUiState {
    object Idle : TranscriptionUiState()
    object Transcribing : TranscriptionUiState()
    data class Success(val text: String) : TranscriptionUiState()
    data class Error(val message: String) : TranscriptionUiState()
}

/**
 * ViewModel for transcription operations.
 * Manages transcription state and coordinates with AudioTranscriber.
 */
@HiltViewModel
class TranscriptionViewModel @Inject constructor(
    private val audioTranscriber: AudioTranscriber,
    private val audioRepository: AudioRepository,
    private val noteDao: NoteDao,
    private val relationshipDao: RelationshipDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<TranscriptionUiState>(TranscriptionUiState.Idle)
    val uiState: StateFlow<TranscriptionUiState> = _uiState.asStateFlow()

    /**
     * Transcribes an audio file.
     * 
     * @param audioFile The audio file to transcribe
     * @param parentNoteId Optional parent note ID. If provided, creates a "Follows On" relationship after note creation.
     */
    fun transcribe(audioFile: File, parentNoteId: Long? = null) {
        if (_uiState.value is TranscriptionUiState.Transcribing) {
            Timber.w("Transcription already in progress")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = TranscriptionUiState.Transcribing

                // Validate file first
                audioRepository.validateAudioFile(audioFile).fold(
                    onSuccess = {
                        Timber.i("Starting transcription for file: ${audioFile.absolutePath}")
                    },
                    onFailure = { throwable ->
                        val error = throwable.toAppError("validateAudioFile")
                        Timber.e(throwable, "Audio file validation failed")
                        _uiState.value = TranscriptionUiState.Error(
                            ErrorHandler.handleError(error)
                        )
                        return@launch
                    }
                )

                withContext(Dispatchers.IO) {
                    audioTranscriber.transcribe(audioFile).fold(
                        onSuccess = { text ->
                            Timber.i("Transcription successful: $text")
                            _uiState.value = TranscriptionUiState.Success(text)
                            
                            // Create note from transcription
                            createNoteFromTranscription(text, audioFile, parentNoteId)
                        },
                        onFailure = { throwable ->
                            val error = throwable.toAppError("transcribe")
                            Timber.e(throwable, "Transcription failed")
                            _uiState.value = TranscriptionUiState.Error(
                                ErrorHandler.handleError(error)
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception during transcription")
                _uiState.value = TranscriptionUiState.Error(
                    ErrorHandler.handleThrowable(e, "transcribe")
                )
            }
        }
    }

    /**
     * Resets the transcription state.
     */
    fun reset() {
        _uiState.value = TranscriptionUiState.Idle
    }

    /**
     * Creates a note from transcription text and saves it to the database.
     * If parentNoteId is provided, creates a "Follows On" relationship.
     * 
     * @param text The transcribed text
     * @param audioFile The audio file that was transcribed
     * @param parentNoteId Optional parent note ID to create a "Follows On" relationship with
     */
    private suspend fun createNoteFromTranscription(text: String, audioFile: File, parentNoteId: Long?) {
        try {
            val title = NoteTitleExtractor.extractTitle(text)
            val currentTime = System.currentTimeMillis()
            
            val note = NoteEntity(
                title = title,
                text = text,
                recording = audioFile.absolutePath,
                embedding_vector = null,
                created_at = currentTime,
                updated_at = currentTime,
                status = NoteStatus.DRAFT
            )
            
            val noteId = noteDao.insertNote(note)
            Timber.i("Note created successfully with id: $noteId, title: $title")

            // Create relationship if parent note ID is provided
            if (parentNoteId != null) {
                try {
                    // Check if relationship already exists
                    val existing = relationshipDao.getRelationshipBetweenNotesWithType(
                        noteId,
                        parentNoteId,
                        RelationshipType.FOLLOWS_ON
                    )
                    if (existing == null) {
                        val relationship = RelationshipEntity(
                            note_a_id = noteId,
                            note_b_id = parentNoteId,
                            type = RelationshipType.FOLLOWS_ON,
                            created_at = System.currentTimeMillis()
                        )
                        relationshipDao.insertRelationship(relationship)
                        Timber.i("Created FOLLOWS_ON relationship: note $noteId -> note $parentNoteId")
                    } else {
                        Timber.d("Relationship already exists between note $noteId and $parentNoteId")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to create relationship for transcribed note")
                    // Don't fail note creation if relationship creation fails
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create note from transcription")
            // Don't update UI state - transcription was successful, note creation failure is logged
        }
    }
}
