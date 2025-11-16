package ru.chtcholeg.aichat.ui.chatscreen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RowScope.ActionButton(
    text: String,
    onAction: (ChatAction) -> Unit,
) {
    Button(
        onClick = { onAction(ChatAction.ResetChat) },
        contentPadding = PaddingValues(
            start = 8.dp,
            top = 8.dp,
            end = 8.dp,
            bottom = 8.dp,
        ),
        modifier = Modifier.Companion.weight(1f),
    ) {
        Text(
            text = text,
            maxLines = 1,
        )
    }
}