package ru.chtcholeg.aichat.ui.views

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

@Composable
fun ClickableCheckbox(
    isChecked: Boolean,
    text: String,
    onCheckChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .toggleable(
                value = isChecked,
                onValueChange = onCheckChange,
                role = Role.Checkbox
            ),
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckChange,
        )
        Text(text = text)
    }
}