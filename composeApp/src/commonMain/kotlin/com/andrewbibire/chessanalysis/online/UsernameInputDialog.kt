package com.andrewbibire.chessanalysis.online

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun UsernameInputDialog(
    platform: Platform,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (platform) {
                    Platform.CHESS_COM -> "Enter Chess.com Username"
                    Platform.LICHESS -> "Enter Lichess Username"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        isError = false
                    },
                    label = { Text("Username") },
                    placeholder = { Text("Enter your username") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                if (isError) {
                    Text(
                        text = "Invalid username. Use only letters, numbers, underscore, or hyphen (min 3 characters)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (DummyData.validateUsername(username, platform)) {
                        onConfirm(username)
                    } else {
                        isError = true
                    }
                },
                enabled = username.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
