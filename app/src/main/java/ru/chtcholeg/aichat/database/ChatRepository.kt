package ru.chtcholeg.aichat.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import java.util.UUID

object ChatRepository {
    val scope = CoroutineScope(Dispatchers.IO.limitedParallelism(1) + SupervisorJob())

    private val database get() = DatabaseManager.instance.appDatabase
    private val queries get() = database.appDatabaseQueries

    fun getAllChats(): Flow<List<Chat>> =
        queries.getAllChats()
            .asFlow()
            .mapToList(Dispatchers.Default)


    fun getChatById(id: String): Chat? = database.transactionWithResult {
        queries.getChatById(id).executeAsOneOrNull()
    }

    fun createChat(
        id: String,
        name: String,
        type: String,
        createdAt: Long,
    ) = queries.insertChat(id, name, type, createdAt, createdAt)

    fun updateChatName(chatId: String, newName: String) {
        val now = System.currentTimeMillis()
        queries.updateChatName(newName, now, chatId)
    }

    fun deleteChat(chatId: String) {
        database.transaction {
            queries.deleteChat(chatId)
            queries.deleteMessagesByChatId(chatId)
        }
    }

    fun getMessagesByChatId(chatId: String): List<Message> =
        queries.getMessagesByChatId(chatId).executeAsList()

    fun addMessage(
        chatId: String,
        role: String,
        content: String,
        contentType: String,
    ) {
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        database.transaction {
            queries.insertMessage(
                messageId, chatId, role, content, timestamp,
                contentType, null
            )
            queries.updateChatTimestamp(timestamp, chatId)
        }
    }

    fun deleteMessage(messageId: String) = {
        queries.deleteMessage(messageId)
    }
}