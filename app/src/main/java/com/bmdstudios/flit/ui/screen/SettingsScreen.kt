package com.bmdstudios.flit.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bmdstudios.flit.ui.component.PrimaryActionButton
import com.bmdstudios.flit.ui.component.PrimaryActionTextButton
import com.bmdstudios.flit.ui.dialog.ConnectDialog
import com.bmdstudios.flit.ui.settings.ModelSize
import com.bmdstudios.flit.ui.theme.ThemeMode
import com.bmdstudios.flit.ui.onboarding.SettingsTourSection
import com.bmdstudios.flit.ui.viewmodel.ConnectionState
import com.bmdstudios.flit.ui.viewmodel.ExportState
import com.bmdstudios.flit.ui.viewmodel.ImportState
import com.bmdstudios.flit.ui.viewmodel.SyncState
import com.bmdstudios.flit.ui.viewmodel.ModelDownloadViewModel
import com.bmdstudios.flit.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

private fun highlightModifier(enabled: Boolean): Modifier {
    if (!enabled) return Modifier
    return Modifier
        .background(
            color = Color(0x66B3E5FF),
            shape = RoundedCornerShape(8.dp)
        )
        .border(
            width = 2.dp,
            color = Color(0xFF7EC3FF),
            shape = RoundedCornerShape(8.dp)
        )
        .padding(horizontal = 8.dp, vertical = 4.dp)
}

/**
 * Settings screen composable.
 * Displays application settings including theme mode selection.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    modelDownloadViewModel: ModelDownloadViewModel? = null,
    highlightConnectionSection: Boolean = false,
    highlightedTourSection: SettingsTourSection? = null
) {
    // Use passed viewModel or create new one for backward compatibility
    val downloadViewModel = modelDownloadViewModel ?: hiltViewModel()

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val modelSize by viewModel.modelSize.collectAsStateWithLifecycle()
    val noteDetails by viewModel.noteDetails.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    // Snackbar for export status messages
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var themeSectionY by remember { mutableStateOf<Float?>(null) }
    var noteDetailsSectionY by remember { mutableStateOf<Float?>(null) }
    var modelSectionY by remember { mutableStateOf<Float?>(null) }
    var dataSectionY by remember { mutableStateOf<Float?>(null) }
    var connectionSectionY by remember { mutableStateOf<Float?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { viewModel.importData(it) } }
    )

    // Track previous model size to detect changes
    var previousModelSize by remember { mutableStateOf<ModelSize?>(null) }

    // Connection dialog state
    var showConnectDialog by remember { mutableStateOf(false) }

    // Error dialog state
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Sync error dialog state
    var showSyncErrorDialog by remember { mutableStateOf(false) }
    var syncErrorMessage by remember { mutableStateOf<String?>(null) }

    // Trigger model download when model size changes
    LaunchedEffect(modelSize) {
        // Skip initial load - only trigger on actual changes
        if (previousModelSize != null && previousModelSize != modelSize) {
            downloadViewModel.downloadModels(context)
        }
        previousModelSize = modelSize
    }

    // Handle export state changes
    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is ExportState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Export successful!\nFile saved to: ${state.filePath}",
                    duration = androidx.compose.material3.SnackbarDuration.Long
                )
                viewModel.resetExportState()
            }
            is ExportState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Export failed: ${state.message}",
                    duration = androidx.compose.material3.SnackbarDuration.Long
                )
                viewModel.resetExportState()
            }
            else -> { /* Idle or Exporting - no action needed */ }
        }
    }

    LaunchedEffect(importState) {
        when (val state = importState) {
            is ImportState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Import complete: ${state.notesImported} notes, " +
                        "${state.relationshipsImported} relationships linked " +
                        "(${state.relationshipsSkipped} skipped).",
                    duration = androidx.compose.material3.SnackbarDuration.Long
                )
                viewModel.resetImportState()
            }
            is ImportState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Import failed: ${state.message}",
                    duration = androidx.compose.material3.SnackbarDuration.Long
                )
                viewModel.resetImportState()
            }
            else -> { }
        }
    }

    // Handle connection state changes
    LaunchedEffect(connectionState) {
        when (val state = connectionState) {
            is ConnectionState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Successfully connected to Flit - Core!",
                    duration = androidx.compose.material3.SnackbarDuration.Short
                )
                showConnectDialog = false
                showErrorDialog = false
                viewModel.resetConnectionState()
            }
            is ConnectionState.Error -> {
                // Show error dialog instead of resetting immediately
                errorMessage = state.message
                showErrorDialog = true
            }
            else -> { /* Idle or Connecting - no action needed */ }
        }
    }

    // Handle sync state changes
    LaunchedEffect(syncState) {
        when (val state = syncState) {
            is SyncState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Sync completed successfully!",
                    duration = androidx.compose.material3.SnackbarDuration.Short
                )
                viewModel.resetSyncState()
            }
            is SyncState.Error -> {
                syncErrorMessage = state.message
                showSyncErrorDialog = true
            }
            else -> { /* Idle or Syncing - no action needed */ }
        }
    }

    LaunchedEffect(highlightConnectionSection, connectionSectionY) {
        if (!highlightConnectionSection) return@LaunchedEffect
        val target = connectionSectionY ?: return@LaunchedEffect
        scrollState.animateScrollTo(target.toInt().coerceAtLeast(0))
    }

    LaunchedEffect(highlightedTourSection, themeSectionY, noteDetailsSectionY, modelSectionY, dataSectionY, connectionSectionY) {
        val targetY = when (highlightedTourSection) {
            SettingsTourSection.Theme -> themeSectionY
            SettingsTourSection.NoteDetails -> noteDetailsSectionY
            SettingsTourSection.Model -> modelSectionY
            SettingsTourSection.DataManagement -> dataSectionY
            SettingsTourSection.Connection -> connectionSectionY
            null -> null
        } ?: return@LaunchedEffect
        scrollState.animateScrollTo(targetY.toInt().coerceAtLeast(0))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )

        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(top = 8.dp, bottom = 4.dp)
                .onGloballyPositioned { layoutCoordinates ->
                    themeSectionY = layoutCoordinates.positionInParent().y
                }
                .then(highlightModifier(highlightedTourSection == SettingsTourSection.Theme)),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ThemeMode.values().forEach { mode ->
            ListItem(
                headlineContent = {
                    Text(
                        text = when (mode) {
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                            ThemeMode.SYSTEM -> "System default"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                leadingContent = {
                    RadioButton(
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) }
                    )
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        ListItem(
            headlineContent = {
                Text(
                    text = "Note Details",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            supportingContent = {
                Text(
                    text = "Show preview text in note cards",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                Switch(
                    checked = noteDetails,
                    onCheckedChange = { viewModel.setNoteDetails(it) }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { layoutCoordinates ->
                    noteDetailsSectionY = layoutCoordinates.positionInParent().y
                }
                .then(highlightModifier(highlightedTourSection == SettingsTourSection.NoteDetails))
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Model",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(top = 8.dp, bottom = 4.dp)
                .onGloballyPositioned { layoutCoordinates ->
                    modelSectionY = layoutCoordinates.positionInParent().y
                }
                .then(highlightModifier(highlightedTourSection == SettingsTourSection.Model))
        )

        Text(
            text = "Transcription Model Size",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Download size and Performance requirements",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ModelSize.values().forEach { size ->
            ListItem(
                headlineContent = {
                    Column {
                        Text(
                            text = when (size) {
                                ModelSize.NONE -> "None"
                                ModelSize.LIGHT -> "Light"
                                ModelSize.MEDIUM -> "Medium"
                                ModelSize.HEAVY -> "Performance"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = when (size) {
                                ModelSize.NONE -> "Text only - no transcription model"
                                ModelSize.LIGHT -> "Fast and less accurate, better on older phones • 162 MB"
                                ModelSize.MEDIUM -> "Balanced speed and accuracy • 375 MB"
                                ModelSize.HEAVY -> "Slower and more accurate, better on newer phones • 946 MB"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                },
                leadingContent = {
                    RadioButton(
                        selected = modelSize == size,
                        onClick = { viewModel.setModelSize(size) }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = modelSize == size,
                        onClick = { viewModel.setModelSize(size) }
                    )
            )
        }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Data Management",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 4.dp)
                    .onGloballyPositioned { layoutCoordinates ->
                        dataSectionY = layoutCoordinates.positionInParent().y
                    }
                    .then(highlightModifier(highlightedTourSection == SettingsTourSection.DataManagement))
            )

            Text(
                text = "Export Data",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Export all notes as a zip of markdown files (categories, relationships, and timestamps in frontmatter).",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PrimaryActionButton(
                onClick = { viewModel.exportData() },
                enabled = exportState !is ExportState.Exporting && importState !is ImportState.Importing
            ) {
                if (exportState is ExportState.Exporting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exporting...")
                    }
                } else {
                    Text("Export Data")
                }
            }

            Text(
                text = "Import notes",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Choose a Flit export zip (root-level .md files only) or a single markdown file. " +
                    "Notes are merged into your library; relationships only link notes from the same import.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PrimaryActionButton(
                onClick = {
                    importLauncher.launch(
                        arrayOf(
                            "application/zip",
                            "text/markdown",
                            "text/x-markdown",
                            "text/plain",
                            "*/*"
                        )
                    )
                },
                enabled = importState !is ImportState.Importing && exportState !is ExportState.Exporting
            ) {
                if (importState is ImportState.Importing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importing...")
                    }
                } else {
                    Text("Import…")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Connection",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 4.dp)
                    .onGloballyPositioned { layoutCoordinates ->
                        connectionSectionY = layoutCoordinates.positionInParent().y
                    }
                    .then(highlightModifier(highlightedTourSection == SettingsTourSection.Connection))
                    .background(
                        color = if (highlightConnectionSection) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .then(
                        if (highlightConnectionSection) {
                            Modifier.border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            if (isConnected) {
                Text(
                    text = "Sync with Flit - Core",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Sync your notes, categories, and relationships with Flit - Core",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PrimaryActionButton(
                    onClick = { viewModel.sync() },
                    enabled = syncState !is SyncState.Syncing
                ) {
                    when (val state = syncState) {
                        is SyncState.Syncing -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(state.currentStep)
                            }
                        }
                        else -> {
                            Text("Sync with Flit - Core")
                        }
                    }
                }

                Spacer(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Reconnect to Flit - Core",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Reconnect this app to Flit - Core backend",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PrimaryActionButton(
                    onClick = {
                        viewModel.resetConnectionState()
                        showConnectDialog = true
                    },
                    enabled = connectionState !is ConnectionState.Connecting
                ) {
                    if (connectionState is ConnectionState.Connecting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connecting...")
                        }
                    } else {
                        Text("Reconnect to Flit - Core")
                    }
                }
            } else {
                Text(
                    text = "Connect to Flit - Core",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Connect this app to Flit - Core backend for sync and cloud features",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Request a connection code from your Flit - Core account.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PrimaryActionTextButton(
                    onClick = {
                        try {
                            uriHandler.openUri(viewModel.flitCoreWebLoginUrl)
                        } catch (_: Exception) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Could not open browser")
                            }
                        }
                    }
                ) {
                    Text("Log in on Flit - Core")
                }

                PrimaryActionButton(
                    onClick = {
                        viewModel.resetConnectionState()
                        showConnectDialog = true
                    },
                    enabled = connectionState !is ConnectionState.Connecting
                ) {
                    if (connectionState is ConnectionState.Connecting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connecting...")
                        }
                    } else {
                        Text("Connect to Flit - Core")
                    }
                }
            }
        }

        // Connection dialog
        if (showConnectDialog) {
            ConnectDialog(
                onDismiss = {
                    showConnectDialog = false
                    // Reset error state if there's an error when dismissing
                    if (connectionState is ConnectionState.Error) {
                        viewModel.resetConnectionState()
                        showErrorDialog = false
                    }
                },
                onConnect = { connectionCode ->
                    viewModel.connectToFlitCore(connectionCode)
                },
                isLoading = connectionState is ConnectionState.Connecting,
                errorMessage = null // Errors are now shown in separate dialog
            )
        }

        // Close dialog when connection is successful
        LaunchedEffect(connectionState) {
            if (connectionState is ConnectionState.Success && showConnectDialog) {
                showConnectDialog = false
            }
        }

        // Connection error dialog
        if (showErrorDialog && errorMessage != null) {
            AlertDialog(
                onDismissRequest = {
                    showErrorDialog = false
                    viewModel.resetConnectionState()
                    errorMessage = null
                },
                title = {
                    Text("Connection Error")
                },
                text = {
                    Text(errorMessage ?: "An unknown error occurred")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showErrorDialog = false
                            viewModel.resetConnectionState()
                            errorMessage = null
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }

        // Sync error dialog
        if (showSyncErrorDialog && syncErrorMessage != null) {
            AlertDialog(
                onDismissRequest = {
                    showSyncErrorDialog = false
                    viewModel.resetSyncState()
                    syncErrorMessage = null
                },
                title = {
                    Text("Sync Error")
                },
                text = {
                    Text(syncErrorMessage ?: "An unknown error occurred")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSyncErrorDialog = false
                            viewModel.resetSyncState()
                            syncErrorMessage = null
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }

        // Snackbar for export status messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(16.dp)
        )
    }
}
