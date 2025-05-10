package com.example.samarpan.Model

data class ChatMessage(
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val audioUrl: String? = null // nullable audio URL
)
