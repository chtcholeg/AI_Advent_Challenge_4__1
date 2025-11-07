package ru.chtcholeg.aichat.ui.chatscreen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.chtcholeg.aichat.ui.theme.AIChatTheme
import ru.chtcholeg.aichat.ui.views.BottomSheet
import ru.chtcholeg.aichat.ui.views.FloatTextField

@Composable
fun SettingsDialog(
    onAction: (ChatAction) -> Unit,
    settings: ChatState.Dialog.Settings,
    modifier: Modifier = Modifier,
) {
    BottomSheet(
        onDismissRequest = { onAction(ChatAction.HideDialog) },
        modifier = modifier
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp),
        )
        Temperature(settings.temperature) { onAction(ChatAction.SetTemperature(it)) }

        Text(
            text = "Output content",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .align(Alignment.Start)
        )
        OutputContent.entries.forEach { outputContent ->
            OutputContentItem(
                outputContent = outputContent,
                selectedOutputContent = settings.outputContent,
                onSelected = { onAction(ChatAction.SetOutputContent(outputContent)) },
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Button(
                onClick = { onAction(ChatAction.ResetChat) },
                modifier = Modifier.weight(1f),
            ) {
                Text("Reset chat")
            }
            Spacer(Modifier.width(16.dp))
            Button(
                onClick = { onAction(ChatAction.RefreshToken) },
                modifier = Modifier.weight(1f),
            ) {
                Text("Refresh token")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { onAction(ChatAction.HideDialog) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("OK")
        }
    }
}

@Composable
private fun Temperature(
    temperature: Float,
    onValueChange: (Float) -> Unit,
) {
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
            onValueChange = { onValueChange(it ?: 1f) },
            modifier = Modifier.weight(1.2f),
        )
    }
}

@Composable
private fun OutputContentItem(
    outputContent: OutputContent,
    selectedOutputContent: OutputContent,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelected = selectedOutputContent == outputContent
    Row(
        modifier = modifier
            .selectable(
                selected = isSelected,
                onClick = onSelected,
                role = Role.RadioButton
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = outputContent.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private val OutputContent.text: String
    get() = when (this) {
        OutputContent.PLAIN_TEXT -> "Plain text"
        OutputContent.JSON -> "Json"
        OutputContent.XML -> "Xml"
        OutputContent.FULL_FLEDGED_ASSISTANT -> "Full-fledged assistant"
        OutputContent.SEQUENTIAL_ASSISTANT -> "Sequential assistant"
    }

@Preview(showBackground = true)
@Composable
fun SettingsDialogPreview() {
    AIChatTheme {
        SettingsDialog(
            onAction = {},
            settings = ChatState.Dialog.Settings(),
        )
    }
}