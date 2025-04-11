package com.example.samarpan.Model

data class UnifiedPost(
    var postId: String? = null,
    val donorId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val location: String? = null,
    val profileName: String? = null,
    val timestamp: Long = 0L,
    var category: String? = null  // "Food", "Clothes", "Electronics"
)
