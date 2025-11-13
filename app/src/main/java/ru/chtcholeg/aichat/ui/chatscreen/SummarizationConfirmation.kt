package ru.chtcholeg.aichat.ui.chatscreen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.chtcholeg.aichat.ui.views.BottomSheet
import ru.chtcholeg.aichat.ui.views.ClickableCheckbox

@Composable
fun SummarizationConfirmation(
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shouldSaveSystemPrompt: Boolean by rememberSaveable { mutableStateOf(true) }

    BottomSheet(
        onDismissRequest = { onAction(ChatAction.HideDialog) },
        modifier = modifier,
    ) {
        Text(
            text = "Summarization confirmation",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp),
        )

        Text("The current chat will be reset and a new chat will be started!")
        Spacer(modifier = Modifier.height(8.dp))

        ClickableCheckbox(
            isChecked = shouldSaveSystemPrompt,
            text = "Save the system prompt",
            onCheckChange = { shouldSaveSystemPrompt = it },
            modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        Row {
            ActionButton("Ok") { onAction(ChatAction.Summarize(shouldSaveSystemPrompt)) }
            Spacer(Modifier.width(16.dp))
            ActionButton("Cancel") { onAction(ChatAction.HideDialog) }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}