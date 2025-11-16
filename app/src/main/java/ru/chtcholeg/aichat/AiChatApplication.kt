package ru.chtcholeg.aichat

import android.app.Application
import ru.chtcholeg.aichat.database.DatabaseManager

class AiChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        DatabaseManager.init(this)
    }
}
