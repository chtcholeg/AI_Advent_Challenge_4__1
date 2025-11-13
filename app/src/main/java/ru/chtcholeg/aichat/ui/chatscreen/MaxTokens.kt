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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaxTokens(
    maxTokens: Int?,
    onValueChange: (Int?) -> Unit,
) {
    val options = listOf(null, 100, 500, 1_000, 2_000, 4_000, 10_000)
    var expanded by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.Companion.Start,
    ) {
        Text(
            text = "Max token count:",
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = maxTokens?.toString() ?: "undefined",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
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
                        text = { Text(option?.toString() ?: "undefined") },
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