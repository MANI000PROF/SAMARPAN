package com.example.samarpan.Model

import java.io.Serializable

data class Alert(
    val requestId: String? = null,  // <-- ADD THIS LINE
    val postId: String? = null,        // ID of the post related to the alert
    val donorId: String? = null,       // ID of the donor who receives the request
    val requesterId: String? = null,   // ID of the user who sent the request
    val status: String? = null,        // Status of the alert: "Pending", "Accepted", "Declined"
    val timestamp: Long = 0L,          // Timestamp of the request
    val message: String? = null,       // Optional message for the alert display
    val title: String? = null,          // Optional title for the alert display
    val postImageUrl: String = "",
) : Serializable
