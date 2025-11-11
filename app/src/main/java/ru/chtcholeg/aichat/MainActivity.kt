package ru.chtcholeg.aichat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.chtcholeg.aichat.ui.chatscreen.ChatScreen
import ru.chtcholeg.aichat.ui.theme.AIChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            AIChatTheme {
                ChatScreen()
            }
        }
    }
}
