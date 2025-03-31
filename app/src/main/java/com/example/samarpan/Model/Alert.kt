package com.example.samarpan.Model

data class Alert(
    val postId: String = "",
    val donorId: String = "",
    val requesterId: String = "",
    val status: String = "",  // Example: "Pending", "Accepted", "Declined"
    val timestamp: Long = 0L, // Firebase stores timestamps as Long
    val message: String = "", // Optional: Message for UI display
    val title: String = ""    // Optional: Alert title for UI display
)
