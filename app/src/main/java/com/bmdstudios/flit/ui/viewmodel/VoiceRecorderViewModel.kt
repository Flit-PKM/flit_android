package com.bmdstudios.flit.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bmdstudios.flit.config.AudioConfig
import com.bmdstudios.flit.domain.error.AppError
import com.bmdstudios.flit.domain.error.ErrorHandler
import com.bmdstudios.flit.domain.repository.AudioRepository
import com.bmdstudios.flit.utils.VoiceRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
 * UI state for voice recording.
 */
sealed class VoiceRecorderUiState {
    object Idle : VoiceRecorderUiState()
    object Recording : VoiceRecorderUiState()
    data class Recorded(val file: File) : VoiceRecorderUiState()
    data class Error(val message: String) : VoiceRecorderUiState()
}

/**
 * ViewModel for voice recording operations.
 * Manages recording state and coordinates with VoiceRecorder.
 */
@HiltViewModel
class VoiceRecorderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioRepository: AudioRepository,
    private val audioConfig: AudioConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow<VoiceRecorderUiState>(VoiceRecorderUiState.Idle)
    val uiState: StateFlow<VoiceRecorderUiState> = _uiState.asStateFlow()

    private var currentRecorder: VoiceRecorder? = null
    private var currentRecordingFile: File? = null

    /**
     * Starts recording audio.
     */
    fun startRecording() {
        if (_uiState.value is VoiceRecorderUiState.Recording) {
            Timber.w("Attempted to start recording while already recording")
            return
        }

        viewModelScope.launch {
            try {
                val outputFile = audioRepository.createRecordingFile()
                currentRecordingFile = outputFile

                Timber.i("Starting audio recording to file: ${outputFile.absolutePath}")
                val recorder = VoiceRecorder(context, outputFile, audioConfig)
                currentRecorder = recorder

                withContext(Dispatchers.IO) {
                    recorder.startRecording().fold(
                        onSuccess = {
                            Timber.i("Audio recording started successfully")
                            _uiState.value = VoiceRecorderUiState.Recording
                        },
                        onFailure = { error ->
                            Timber.e(error, "Failed to start audio recording")
                            currentRecorder = null
                            currentRecordingFile = null
                            _uiState.value = VoiceRecorderUiState.Error(
                                ErrorHandler.handleError(
                                    ErrorHandler.transform(error, "startRecording")
                                )
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while starting recording")
                _uiState.value = VoiceRecorderUiState.Error(
                    ErrorHandler.handleThrowable(e, "startRecording")
                )
            }
        }
    }

    /**
     * Stops recording audio.
     */
    fun stopRecording() {
        if (_uiState.value !is VoiceRecorderUiState.Recording) {
            Timber.w("Attempted to stop recording while not recording")
            return
        }

        viewModelScope.launch {
            try {
                val recorder = currentRecorder
                val recordingFile = currentRecordingFile

                Timber.i("Stopping audio recording")
                withContext(Dispatchers.IO) {
                    recorder?.stopRecording()?.fold(
                        onSuccess = {
                            Timber.i("Audio recording stopped successfully")
                            currentRecorder = null

                            if (recordingFile != null && recordingFile.exists()) {
                                _uiState.value = VoiceRecorderUiState.Recorded(recordingFile)
                            } else {
                                _uiState.value = VoiceRecorderUiState.Error(
                                    "Recording file was not created"
                                )
                            }
                        },
                        onFailure = { error ->
                            Timber.e(error, "Error while stopping audio recording")
                            currentRecorder = null
                            _uiState.value = VoiceRecorderUiState.Error(
                                ErrorHandler.handleError(
                                    ErrorHandler.transform(error, "stopRecording")
                                )
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while stopping recording")
                currentRecorder = null
                _uiState.value = VoiceRecorderUiState.Error(
                    ErrorHandler.handleThrowable(e, "stopRecording")
                )
            }
        }
    }

    /**
     * Gets the current recording file if available.
     */
    fun getCurrentRecordingFile(): File? = currentRecordingFile

    /**
     * Resets the recorder state to Idle.
     * This should be called after transcription is triggered to prevent duplicate processing.
     */
    fun reset() {
        if (_uiState.value is VoiceRecorderUiState.Recorded) {
            Timber.d("Resetting recorder state to Idle")
            _uiState.value = VoiceRecorderUiState.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("VoiceRecorderViewModel cleared, releasing recorder")
        currentRecorder?.release()
        currentRecorder = null
    }
}
