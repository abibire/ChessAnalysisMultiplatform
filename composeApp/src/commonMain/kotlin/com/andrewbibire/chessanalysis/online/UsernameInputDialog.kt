package com.andrewbibire.chessanalysis.online

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.andrewbibire.chessanalysis.BoardDark
import kotlinx.coroutines.launch

@Composable
fun UsernameInputDialog(
    platform: Platform,
    onDismiss: () -> Unit,
    onConfirm: suspend (String) -> Unit,
    isLoading: Boolean = false
) {
    // Load last username
    val lastUsername = remember { UserPreferences.getLastUsername() }

    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = lastUsername ?: "",
                selection = TextRange((lastUsername?.length ?: 0))
            )
        )
    }
    var isError by remember { mutableStateOf(false) }
    var isPrefilledText by remember { mutableStateOf(lastUsername != null) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
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
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        // Check if backspace was pressed on prefilled text
                        if (isPrefilledText && newValue.text.length < textFieldValue.text.length) {
                            // Clear entire field on first backspace
                            textFieldValue = TextFieldValue(text = "", selection = TextRange(0))
                            isPrefilledText = false
                        } else {
                            textFieldValue = newValue
                            isPrefilledText = false
                        }
                        isError = false
                    },
                    label = { Text("Username") },
                    placeholder = { Text("Enter your username") },
                    isError = isError,
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent { keyEvent ->
                            // Handle backspace key for clearing prefilled text
                            if (isPrefilledText &&
                                keyEvent.type == KeyEventType.KeyDown &&
                                keyEvent.key == Key.Backspace) {
                                textFieldValue = TextFieldValue(text = "", selection = TextRange(0))
                                isPrefilledText = false
                                true
                            } else if (keyEvent.type == KeyEventType.KeyDown) {
                                // Any other key disables the prefilled state
                                isPrefilledText = false
                                false
                            } else {
                                false
                            }
                        },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = if (isPrefilledText) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = if (isPrefilledText) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (textFieldValue.text.isNotBlank() && !isLoading) {
                                if (DummyData.validateUsername(textFieldValue.text, platform)) {
                                    coroutineScope.launch {
                                        UserPreferences.saveLastUsername(textFieldValue.text)
                                        onConfirm(textFieldValue.text)
                                    }
                                } else {
                                    isError = true
                                }
                            }
                        }
                    )
                )
                if (isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = BoardDark
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Fetching profile...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (DummyData.validateUsername(textFieldValue.text, platform)) {
                        coroutineScope.launch {
                            UserPreferences.saveLastUsername(textFieldValue.text)
                            onConfirm(textFieldValue.text)
                        }
                    } else {
                        isError = true
                    }
                },
                enabled = textFieldValue.text.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
