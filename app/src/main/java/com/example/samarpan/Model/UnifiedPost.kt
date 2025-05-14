package com.example.samarpan.Model

import java.io.Serializable

data class UnifiedPost(
    var postId: String? = null,
    val donorId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val location: String? = null,
    val profileName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long = 0L,
    val profileImageUrl: String? = null,
    val userId: String? = null,
    var category: String? = null  // "Food", "Clothes", "Electronics"
): Serializable
