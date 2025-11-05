package ru.chtcholeg.aichat.ui.chatscreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MessageBubble(
    chatMessage: ChatMessage,
    modifier: Modifier = Modifier
) {
    val alignment = if (chatMessage.isFromUser) Alignment.Companion.CenterEnd else Alignment.Companion.CenterStart
    val startPadding = if (chatMessage.isFromUser) 32.dp else 0.dp
    val endPadding = if (chatMessage.isFromUser) 0.dp else 32.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .padding(start = startPadding, end = endPadding),
        contentAlignment = alignment
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = chatMessage.backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            when (chatMessage) {
                is ChatMessage.RegularMessage -> SimpleText(chatMessage)
                is ChatMessage.ErrorOnParsing -> ErrorText(chatMessage)
                is ChatMessage.Parsed -> ParsedText(chatMessage)
            }
        }
    }
}

@Composable
private fun SimpleText(chatMessage: ChatMessage.RegularMessage) {
    Text(
        text = chatMessage.originalMessage.content,
        color = chatMessage.textColor,
        modifier = Modifier.padding(all = 16.dp)
    )
}

@Composable
private fun ErrorText(chatMessage: ChatMessage.ErrorOnParsing) {
    Column(
        modifier = Modifier.padding(all = 16.dp)
    ) {
        Text(
            text = chatMessage.outputContent.text,
            color = chatMessage.textColor,
            fontWeight = FontWeight.W200,
        )
        Text(
            text = chatMessage.message,
            color = chatMessage.textColor,
        )
    }
}

@Composable
private fun ParsedText(chatMessage: ChatMessage.Parsed) {
    Column(
        modifier = Modifier.padding(all = 16.dp)
    ) {
        Text(
            text = chatMessage.outputContent.text,
            color = chatMessage.textColor,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.W200,

        )
        Text(
            text = chatMessage.title,
            color = chatMessage.textColor,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = chatMessage.beautifulFraming.insertSpacesBetweenUnicode(2),
            color = chatMessage.textColor,
        )
        Text(
            text = chatMessage.message,
            color = chatMessage.textColor,
        )
    }
}

fun String.insertSpacesBetweenUnicode(spaceCount: Int = 1): String {
    return codePoints()
        .toArray()
        .map { String(Character.toChars(it)) }
        .joinToString(" ".repeat(spaceCount))
}

private val ChatMessage.backgroundColor: Color
    @Composable get() = when {
        this is ChatMessage.RegularMessage && isFromUser -> MaterialTheme.colorScheme.primaryContainer
        this is ChatMessage.RegularMessage -> MaterialTheme.colorScheme.surfaceVariant
        this is ChatMessage.Parsed -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }

private val ChatMessage.textColor: Color
    @Composable get() = when {
        this is ChatMessage.RegularMessage && isFromUser -> MaterialTheme.colorScheme.onPrimaryContainer
        this is ChatMessage.RegularMessage -> MaterialTheme.colorScheme.onSurfaceVariant
        this is ChatMessage.Parsed -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onErrorContainer
    }

private val OutputContent.text: String
    get() = when (this) {
        OutputContent.PLAIN_TEXT -> ""
        OutputContent.XML -> "XML"
        OutputContent.JSON -> "JSON"
    }