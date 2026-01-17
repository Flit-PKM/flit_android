package com.bmdstudios.flit.ui.component

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.core.content.ContextCompat
import com.bmdstudios.flit.R
import com.bmdstudios.flit.ui.settings.ModelSize
import com.bmdstudios.flit.ui.viewmodel.DownloadUiState
import com.bmdstudios.flit.ui.viewmodel.NotesViewModel
import com.bmdstudios.flit.ui.viewmodel.TranscriptionViewModel
import com.bmdstudios.flit.ui.viewmodel.VoiceRecorderViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

private const val TAG = "BottomBar"

/**
 * Bottom bar containing text input and record button.
 */
@Composable
fun BottomBar(
    modelDownloadState: DownloadUiState,
    modelSize: ModelSize,
    voiceRecorderViewModel: VoiceRecorderViewModel,
    transcriptionViewModel: TranscriptionViewModel,
    notesViewModel: NotesViewModel
) {
    val recorderState by voiceRecorderViewModel.uiState.collectAsState()
    val transcriptionState by transcriptionViewModel.uiState.collectAsState()
    val appendingNoteId by notesViewModel.appendingNoteId.collectAsState()
    val isRecording = recorderState is com.bmdstudios.flit.ui.viewmodel.VoiceRecorderUiState.Recording
    
    // Determine status text (priority: Recording > Transcribing > Appending)
    val statusText = when {
        isRecording -> "Recording"
        transcriptionState is com.bmdstudios.flit.ui.viewmodel.TranscriptionUiState.Transcribing -> "Transcribing"
        appendingNoteId != null -> "Appending"
        else -> null
    }

    var textValue by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val backgroundColor = if (isRecording) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // Status indicator
        if (statusText != null) {
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isRecording) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                // Show cancel button only when appending
                if (appendingNoteId != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Card(
                        modifier = Modifier.size(24.dp),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        IconButton(
                            onClick = {
                                notesViewModel.stopAppending()
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Cancel appending",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = if (isFocused) Alignment.Top else Alignment.CenterVertically
        ) {
        TextField(
            value = textValue,
            onValueChange = { textValue = it },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            placeholder = { Text("Type a message...") },
            minLines = 1,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            )
        )
        if (isFocused) {
            SubmitButton(
                text = textValue,
                onNoteCreated = {
                    textValue = ""
                    focusManager.clearFocus()
                },
                notesViewModel = notesViewModel,
                coroutineScope = coroutineScope
            )
        } else {
            RecordButton(
                modelDownloadState = modelDownloadState,
                modelSize = modelSize,
                voiceRecorderViewModel = voiceRecorderViewModel,
                transcriptionViewModel = transcriptionViewModel,
                notesViewModel = notesViewModel
            )
        }
        }
        }
    }
}

/**
 * Submit button component for creating notes from typed text.
 */
@Composable
private fun SubmitButton(
    text: String,
    onNoteCreated: () -> Unit,
    notesViewModel: NotesViewModel,
    coroutineScope: CoroutineScope
) {
    val isEnabled = text.isNotBlank()

    Card(
        modifier = Modifier
            .size(48.dp)
            .clickable(enabled = isEnabled) {
                if (isEnabled) {
                    Timber.tag(TAG).d("Submitting note from text")
                    coroutineScope.launch {
                        val noteId = notesViewModel.createNoteFromText(text)
                        if (noteId != null) {
                            onNoteCreated()
                        } else {
                            Timber.tag(TAG).e("Failed to create note from text")
                        }
                    }
                }
            },
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Submit note",
                tint = if (isEnabled) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Record button component (WhatsApp-style circular button).
 */
@Composable
private fun RecordButton(
    modelDownloadState: DownloadUiState,
    modelSize: ModelSize,
    voiceRecorderViewModel: VoiceRecorderViewModel,
    transcriptionViewModel: TranscriptionViewModel,
    notesViewModel: NotesViewModel
) {
    val context = LocalContext.current
    val recorderState by voiceRecorderViewModel.uiState.collectAsState()
    val transcriptionState by transcriptionViewModel.uiState.collectAsState()
    val appendingNoteId by notesViewModel.appendingNoteId.collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    
    // Button is enabled only if model is not NONE and permission is granted
    val isEnabled = modelSize != ModelSize.NONE && hasPermission

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            Timber.tag(TAG).i("Audio recording permission granted")
        } else {
            Timber.tag(TAG).w("Audio recording permission denied by user")
        }
    }

    // Request permission if not granted
    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            Timber.tag(TAG).d("Requesting audio recording permission")
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Track processed files to prevent duplicate transcriptions
    val processedFiles = remember { mutableSetOf<String>() }

    // Handle recording completion and trigger transcription
    LaunchedEffect(recorderState, modelDownloadState) {
        if (recorderState is com.bmdstudios.flit.ui.viewmodel.VoiceRecorderUiState.Recorded) {
            val file = (recorderState as com.bmdstudios.flit.ui.viewmodel.VoiceRecorderUiState.Recorded).file
            val filePath = file.absolutePath
            
            // Check if this file has already been processed
            if (filePath !in processedFiles && modelDownloadState is DownloadUiState.Success) {
                Timber.tag(TAG).d("Recording completed, triggering transcription for: $filePath")
                processedFiles.add(filePath)
                transcriptionViewModel.transcribe(file, appendingNoteId)
                // Reset recorder state to prevent re-triggering
                voiceRecorderViewModel.reset()
            } else if (filePath in processedFiles) {
                Timber.tag(TAG).d("File already processed, skipping transcription: $filePath")
            }
        }
    }

    // Clear appending state when transcription succeeds and note is created
    // Also reset transcription state after completion for clean state
    LaunchedEffect(transcriptionState) {
        when (transcriptionState) {
            is com.bmdstudios.flit.ui.viewmodel.TranscriptionUiState.Success -> {
                if (appendingNoteId != null) {
                    Timber.tag(TAG).d("Transcription succeeded, clearing appending state")
                    notesViewModel.stopAppending()
                }
                // Reset transcription state after success to prepare for next transcription
                kotlinx.coroutines.delay(500)
                transcriptionViewModel.reset()
            }
            is com.bmdstudios.flit.ui.viewmodel.TranscriptionUiState.Error -> {
                // Reset transcription state after error to allow retry
                // Longer delay to give user time to notice error state if needed
                kotlinx.coroutines.delay(3000)
                transcriptionViewModel.reset()
            }
            else -> {
                // Idle or Transcribing - no action needed
            }
        }
    }

    Card(
        modifier = Modifier
            .size(48.dp)
            .then(
                if (isEnabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                Timber.tag(TAG).d("Voice recorder button pressed")
                                when (recorderState) {
                                    is com.bmdstudios.flit.ui.viewmodel.VoiceRecorderUiState.Idle,
                                    is com.bmdstudios.flit.ui.viewmodel.VoiceRecorderUiState.Recorded,
                                    is com.bmdstudios.flit.ui.viewmodel.VoiceRecorderUiState.Error -> {
                                        voiceRecorderViewModel.startRecording()
                                    }
                                    is com.bmdstudios.flit.ui.viewmodel.VoiceRecorderUiState.Recording -> {
                                        voiceRecorderViewModel.stopRecording()
                                    }
                                }
                                tryAwaitRelease()
                                if (recorderState is com.bmdstudios.flit.ui.viewmodel.VoiceRecorderUiState.Recording) {
                                    voiceRecorderViewModel.stopRecording()
                                }
                            }
                        )
                    }
                } else {
                    Modifier
                }
            ),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = when {
                !isEnabled -> MaterialTheme.colorScheme.surfaceVariant
                recorderState is com.bmdstudios.flit.ui.viewmodel.VoiceRecorderUiState.Recording -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (recorderState is com.bmdstudios.flit.ui.viewmodel.VoiceRecorderUiState.Recording) {
                Text(
                    text = "●",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onError
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mic),
                    contentDescription = "Record voice",
                    tint = if (isEnabled) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
