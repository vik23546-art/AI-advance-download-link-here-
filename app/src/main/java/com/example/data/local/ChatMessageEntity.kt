package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String, // "user" or "ai"
    val text: String,
    val imageBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
