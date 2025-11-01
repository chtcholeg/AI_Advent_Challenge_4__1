package ru.chtcholeg.aichat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ru.chtcholeg.aichat.core.ChatCore
import ru.chtcholeg.aichat.ui.ChatScreen
import ru.chtcholeg.aichat.ui.theme.AIChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ChatCore.initChat()

        enableEdgeToEdge()
        setContent {
            AIChatTheme {
                ChatScreen()
            }
        }
    }
}
