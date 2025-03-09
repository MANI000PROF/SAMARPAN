package com.example.samarpan.Model

import com.google.firebase.Timestamp
import java.io.Serializable

data class DonationPost(
    val profileName: String? = null,   // Name of the donor
    val location: String? = null,      // Post location
    val foodTitle: String? = null,     // Title of the donation
    val foodDescription: String? = null, // Description of the food
    val foodImage: String? = null,     // Image URL of the food
    val latitude: Double? = null,      // Latitude of the location
    val longitude: Double? = null,     // Longitude of the location
    val userId: String? = null,        // ID of the user who created the post
    val timestamp: Long = 0L           // Timestamp for the post
) : Serializable
