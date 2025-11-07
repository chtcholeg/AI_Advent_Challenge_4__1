package ru.chtcholeg.aichat.ui.chatscreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
                navigationIcon = { NavigationIcon(onAction) },
                actions = { Actions(onAction) },
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = true
            ) {
                items(count = state.messages.size) { message ->
                    val chatMessage = state.messages[message]
                    val context = LocalContext.current
                    MessageBubble(
                        chatMessage = chatMessage,
                        onCopyClicked = { onAction(ChatAction.Copy(context, chatMessage)) },
                    )
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
                    onClick = { onAction(ChatAction.ResetChat) }
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
                onFocusRequested = { onAction(ChatAction.ResetNeedForInputFocus) },
                onSendMessage = { onAction(ChatAction.SendMessage) },
                isLoading = state.isLoading,
                shouldFocus = state.shouldSetFocusOnInput,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    state.dialog?.let { dialog ->
        when (dialog) {
            is ChatState.Dialog.Settings -> SettingsDialog(
                onAction,
                dialog,
            )
        }
    }
}

@Composable
private fun Actions(onAction: (ChatAction) -> Unit) {
    IconButton(
        onClick = { onAction(ChatAction.ShowSettings) }
    ) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = "Settings"
        )
    }
}

@Composable
private fun NavigationIcon(onAction: (ChatAction) -> Unit) {
    val context = LocalContext.current
    IconButton(
        onClick = { onAction(ChatAction.CopyAll(context)) }
    ) {
        Icon(
            imageVector = Icons.Filled.CopyAll,
            contentDescription = "Copy all conversation"
        )
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