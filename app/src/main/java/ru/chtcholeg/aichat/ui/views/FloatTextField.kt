package ru.chtcholeg.aichat.ui.views

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun FloatTextField(
    value: Float?,
    onValueChange: (Float?) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    var textValue by remember(value) {
        mutableStateOf(value?.toString() ?: "")
    }
    var error by remember { mutableStateOf<String?>(null) }

    OutlinedTextField(
        value = textValue,
        onValueChange = { newText ->
            textValue = newText
            error = null

            if (newText.isEmpty()) {
                onValueChange(null)
                return@OutlinedTextField
            }

            try {
                val floatValue = newText.toFloat()
                onValueChange(floatValue)
            } catch (e: NumberFormatException) {
                error = "Invalid float format"
            }
        },
        label = label?.let { text -> { Text(text) } },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Companion.Decimal
        ),
        isError = error != null,
        supportingText = {
            if (error != null) {
                Text(error!!)
            }
        },
        singleLine = true,
    )
}