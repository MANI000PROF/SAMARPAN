package com.example.samarpan.Model

data class User(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val mobile: String = "",
    val profileImageUrl: String? = null,
    val score: Int = 0
)
