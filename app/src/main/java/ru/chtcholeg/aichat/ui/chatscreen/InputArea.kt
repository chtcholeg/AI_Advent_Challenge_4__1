package ru.chtcholeg.aichat.ui.chatscreen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun InputArea(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onFocusRequested: () -> Unit,
    onSendMessage: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    shouldFocus: Boolean = false,
) {
    // Text updating
    var isFocused by remember { mutableStateOf(false) }
    var currentText by remember { mutableStateOf("") }
    LaunchedEffect(inputText) {
        if (!isFocused) {
            currentText = inputText
        }
    }

    // Focus
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(shouldFocus, isLoading) {
        if (shouldFocus && !isLoading) {
            focusRequester.requestFocus()
            onFocusRequested()
        }
    }

    Row(
        modifier = modifier.padding(16.dp),
        verticalAlignment = Alignment.Companion.CenterVertically
    ) {
        TextField(
            value = currentText,
            onValueChange = { newText ->
                currentText = newText
                onInputTextChange(newText)
            },
            placeholder = { Text("Type a message...") },
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Companion.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (!isLoading && inputText.isNotBlank()) {
                        onSendMessage()
                    }
                }
            ),
            enabled = !isLoading,
            singleLine = false,
            maxLines = 3
        )

        IconButton(
            onClick = onSendMessage,
            enabled = inputText.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message"
                )
            }
        }
    }
}
