package com.example.samarpan.Model

import java.io.Serializable

data class LeaderBoardDonor(
    val userId: String? = null,         // Unique ID of the donor
    val name: String? = null,           // Name of the donor
    var profileImage: String? = null,   // URL of the donor's profile image
    var donationCount: Int = 0          // Total number of donations made
) : Serializable
