package com.bmdstudios.flit.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bmdstudios.flit.ui.settings.ModelSize

/**
 * Dialog for selecting transcription model size on first startup.
 * This dialog appears when no model size preference has been set.
 */
@Composable
fun ModelSelectionDialog(
    onConfirm: (ModelSize) -> Unit
) {
    var selectedSize by remember { mutableStateOf<ModelSize>(ModelSize.NONE) }

    AlertDialog(
        onDismissRequest = { /* Dialog is not dismissible */ },
        title = {
            Text("Select Transcription Model")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Choose a transcription model size based on your device and needs:",
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
                                selected = selectedSize == size,
                                onClick = { selectedSize = size }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedSize == size,
                                onClick = { selectedSize = size }
                            )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedSize) }
            ) {
                Text("Continue")
            }
        }
    )
}
