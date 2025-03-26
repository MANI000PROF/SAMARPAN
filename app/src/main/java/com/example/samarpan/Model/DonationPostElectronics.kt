package com.example.samarpan.Model

import java.io.Serializable

data class DonationPostsElectronics(
    val postId: String? = null,        // Unique ID for the post
    val profileName: String? = null,   // Name of the donor
    val location: String? = null,      // Post location
    val electronicsTitle: String? = null,     // Title of the donation
    val electronicsDescription: String? = null, // Description of the food
    val electronicsImage: String? = null,     // Image URL of the food
    val latitude: Double? = null,      // Latitude of the location
    val longitude: Double? = null,     // Longitude of the location
    val donorId: String? = null,       // ID of the user (donor) who created the post
    val timestamp: Long = 0L           // Timestamp for the post
) : Serializable
