package ru.chtcholeg.aichat.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

object ChatRepository {
    private val database get() = DatabaseManager.instance.appDatabase
    private val queries get() = database.appDatabaseQueries

    fun getAllChats(): Flow<List<Chat>> =
        queries.getAllChats()
            .asFlow()
            .mapToList(Dispatchers.Default)


    suspend fun getChatById(id: String): Chat? = withContext(Dispatchers.IO) {
        queries.getChatById(id)
            .executeAsOneOrNull()
    }

    suspend fun createChat(
        id: String,
        name: String,
        type: String,
        createdAt: Long,
    ) = withContext(Dispatchers.IO) {
        queries.insertChat(id, name, type, createdAt, createdAt)
    }

    suspend fun updateChatName(chatId: String, newName: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        queries.updateChatName(newName, now, chatId)
    }

    suspend fun deleteChat(chatId: String) = withContext(Dispatchers.IO) {
        database.transaction {
            queries.deleteChat(chatId)
            queries.deleteMessagesByChatId(chatId)
        }
    }

    fun getMessagesByChatId(chatId: String): Flow<List<Message>> =
        queries.getMessagesByChatId(chatId)
            .asFlow()
            .mapToList(Dispatchers.Default)

    suspend fun addMessage(
        chatId: String,
        role: String,
        content: String,
        contentType: String,
    ) = withContext(Dispatchers.IO) {
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

    suspend fun deleteMessage(messageId: String) = withContext(Dispatchers.IO) {
        queries.deleteMessage(messageId)
    }
}