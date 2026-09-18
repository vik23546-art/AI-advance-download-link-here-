package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.remote.GeminiApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class VesperaRepository(
    private val chatDao: ChatDao,
    private val geminiApiClient: GeminiApiClient = GeminiApiClient()
) {
    val allMessages: Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()

    suspend fun checkAndSeedInitialGreeting(defaultGreeting: String) {
        if (chatDao.getMessageCount() == 0) {
            chatDao.insertMessage(
                ChatMessageEntity(
                    sender = "ai",
                    text = defaultGreeting
                )
            )
        }
    }

    suspend fun sendUserMessage(
        text: String,
        base64Image: String?,
        apiKey: String,
        languageMode: String = "Hinglish"
    ): Result<ChatMessageEntity> {
        // 1. Insert user message into local database
        val userEntity = ChatMessageEntity(
            sender = "user",
            text = text,
            imageBase64 = base64Image
        )
        chatDao.insertMessage(userEntity)

        // 2. Fetch past conversation turns for context
        val currentMessages = chatDao.getAllMessages().first()
        val historyTurns = currentMessages.dropLast(1).map { it.sender to it.text }

        // 3. Call Gemini API
        val apiResult = geminiApiClient.generateReply(
            apiKey = apiKey,
            userPrompt = text,
            base64Image = base64Image,
            languageMode = languageMode,
            conversationHistory = historyTurns
        )

        return if (apiResult.isSuccess) {
            val replyText = apiResult.getOrThrow()
            val aiEntity = ChatMessageEntity(
                sender = "ai",
                text = replyText
            )
            chatDao.insertMessage(aiEntity)
            Result.success(aiEntity)
        } else {
            val errorMsg = apiResult.exceptionOrNull()?.message ?: "Error connecting to Vespera"
            val errorEntity = ChatMessageEntity(
                sender = "ai",
                text = "⚠️ $errorMsg"
            )
            chatDao.insertMessage(errorEntity)
            Result.failure(apiResult.exceptionOrNull() ?: Exception(errorMsg))
        }
    }

    suspend fun clearHistory(defaultGreeting: String) {
        chatDao.clearAllMessages()
        chatDao.insertMessage(
            ChatMessageEntity(
                sender = "ai",
                text = defaultGreeting
            )
        )
    }
}
