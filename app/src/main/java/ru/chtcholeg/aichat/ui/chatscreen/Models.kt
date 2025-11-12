package ru.chtcholeg.aichat.ui.chatscreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.chtcholeg.aichat.core.api.Model

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Models(model: Model, onValueChange: (Model) -> Unit) {
    val options = ChatState.Dialog.Settings.MODELS
    var expanded by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.Companion.Start,
    ) {
        Text("Model:")
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            TextField(
                value = model.id,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                singleLine = true,
                modifier = Modifier.Companion
                    .menuAnchor(MenuAnchorType.Companion.PrimaryNotEditable)
                    .fillMaxWidth(),
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.id) },
                        onClick = {
                            expanded = false
                            onValueChange(option)
                        }
                    )
                }
            }
        }
    }
}