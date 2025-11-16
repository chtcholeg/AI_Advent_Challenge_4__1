package ru.chtcholeg.aichat.ui.chatscreen

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.chtcholeg.aichat.ui.views.BottomSheet

@Composable
fun HistoryDialog(
    onAction: (ChatAction) -> Unit,
    settings: ChatState.Dialog.History,
    modifier: Modifier = Modifier.Companion,
) {
    BottomSheet(
        onDismissRequest = { onAction(ChatAction.HideDialog) },
        modifier = modifier,
    ) {
        Text(
            text = "History",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp),
        )

    }
}