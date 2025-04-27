package com.example.trashcashcampus_mobile.models

/**
 * Data class representing a campus trash bin location
 */
data class CampusLocation(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val description: String = "",
    val binType: String = ""
) 