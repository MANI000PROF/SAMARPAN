package com.example.samarpan.Model

data class SavedLocation(
    var name: String = "",
    var address: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var key: String = "",
    var primary: Boolean = false
)
