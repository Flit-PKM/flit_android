package com.bmdstudios.flit.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bmdstudios.flit.ui.settings.ModelSize
import com.bmdstudios.flit.ui.theme.ThemeMode
import com.bmdstudios.flit.ui.viewmodel.ModelDownloadViewModel
import com.bmdstudios.flit.ui.viewmodel.SettingsViewModel

/**
 * Settings screen composable.
 * Displays application settings including theme mode selection.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    modelDownloadViewModel: ModelDownloadViewModel? = null
) {
    // Use passed viewModel or create new one for backward compatibility
    val downloadViewModel = modelDownloadViewModel ?: hiltViewModel()
    
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val modelSize by viewModel.modelSize.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Track previous model size to detect changes
    var previousModelSize by remember { mutableStateOf<ModelSize?>(null) }
    
    // Trigger model download when model size changes
    LaunchedEffect(modelSize) {
        // Skip initial load - only trigger on actual changes
        if (previousModelSize != null && previousModelSize != modelSize) {
            downloadViewModel.downloadModels(context)
        }
        previousModelSize = modelSize
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
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
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
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

        Text(
            text = "Model",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
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
                                ModelSize.HEAVY -> "Heavy"
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
    }
}
