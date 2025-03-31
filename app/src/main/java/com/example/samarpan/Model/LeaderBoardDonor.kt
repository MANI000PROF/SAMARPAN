package com.example.samarpan.Model

import java.io.Serializable

data class LeaderBoardDonor(
    val userId: String = "",
    val name: String = "",
    val profileImage: String = "",
    var donationCount: Int = 0
) : Serializable
