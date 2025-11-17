package ru.chtcholeg.aichat.ui.chatscreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.chtcholeg.aichat.database.Chat
import ru.chtcholeg.aichat.ui.views.BottomSheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryDialog(
    onAction: (ChatAction) -> Unit,
    history: ChatState.Dialog.History,
    modifier: Modifier = Modifier,
) {
    BottomSheet(
        onDismissRequest = { onAction(ChatAction.HideDialog) },
        modifier = modifier,
    ) {
        history.items.forEachIndexed { index, item ->
            when (item) {
                ChatState.Dialog.History.Item.Title -> {
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 8.dp),
                    )
                }

                is ChatState.Dialog.History.Item.Header -> {
                    if (index > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Start)
                            .padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is ChatState.Dialog.History.Item.ChatInfo -> {
                    ChatItem(
                        item.chat,
                        onLoad = { onAction(ChatAction.LoadChat(item.chat.id)) },
                        onDelete = { onAction(ChatAction.DeleteChat(item.chat.id)) }
                    )
                }
            }

        }
    }
}

@Composable
private fun ChatItem(
    chat: Chat,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Updated at ${formatTime(chat.updated_at)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row {
            IconButton(onClick = onLoad) {
                Icon(
                    imageVector = Icons.Filled.Upload,
                    contentDescription = "Open"
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete"
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}