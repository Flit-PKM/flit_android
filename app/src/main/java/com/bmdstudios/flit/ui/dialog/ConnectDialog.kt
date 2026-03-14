package com.bmdstudios.flit.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Dialog for connecting to Flit Core by entering a connection code.
 */
@Composable
fun ConnectDialog(
    onDismiss: () -> Unit,
    onConnect: (connectionCode: String) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var connectionCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = if (isLoading) { {} } else { onDismiss },
        title = {
            Text("Connect to Flit - Core")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = connectionCode,
                    onValueChange = { connectionCode = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Connection Code") },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (connectionCode.isNotBlank() && !isLoading) {
                                onConnect(connectionCode.trim())
                            }
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (connectionCode.isNotBlank() && !isLoading) {
                        onConnect(connectionCode.trim())
                    }
                },
                enabled = connectionCode.isNotBlank() && !isLoading
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}
