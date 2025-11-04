package ru.chtcholeg.aichat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.chtcholeg.aichat.http.Message
import ru.chtcholeg.aichat.ui.theme.AIChatTheme

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ChatScreen(state = state, onAction = viewModel::onAction, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AI Chat") },
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Settings
            Settings(
                onAction = onAction,
                temperature = state.temperature,
            )

            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = true
            ) {
                items(count = state.messages.size) { message ->
                    MessageBubble(message = state.messages[message])
                }
            }

            // Loading indicator
            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Error message
            state.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    onClick = { onAction(ChatAction.Clear) }
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Input area
            InputArea(
                inputText = state.inputText,
                onInputTextChange = { onAction(ChatAction.Input(it)) },
                onSendMessage = { onAction(ChatAction.SendMessage) },
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

        }
    }
}

@Composable
private fun Settings(
    onAction: (ChatAction) -> Unit,
    temperature: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier.padding(8.dp),
    ) {
        Button(
            onClick = { onAction(ChatAction.RefreshToken) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Refresh token")
        }
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            Text(
                text = "Temperature\n(≈0 - deterministic answer)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(2f),
            )
            Spacer(Modifier.width(8.dp))
            FloatTextField(
                value = temperature,
                onValueChange = { onAction(ChatAction.NewTemperature(it)) },
                modifier = Modifier.weight(1.2f),
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    val alignment = if (message.isFromUser) Alignment.Companion.CenterEnd else Alignment.Companion.CenterStart
    val backgroundColor = if (message.isFromUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (message.isFromUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val startPadding = if (message.isFromUser) 24.dp else 0.dp
    val endPadding = if (message.isFromUser) 0.dp else 24.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .padding(start = startPadding, end = endPadding),
        contentAlignment = alignment
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = message.content,
                color = textColor,
                modifier = Modifier.padding(all = 16.dp)
            )
        }
    }
}

@Composable
private fun InputArea(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = inputText,
            onValueChange = onInputTextChange,
            placeholder = { Text("Type a message...") },
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
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

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    AIChatTheme {
        ChatScreen(
            ChatState(),
            {},
        )
    }
}