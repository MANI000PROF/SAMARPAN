package com.example.samarpan.Model

data class ChatMessage(
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    var replyToMessageId: String? = null,
    val audioUrl: String? = null // nullable audio URL
)
