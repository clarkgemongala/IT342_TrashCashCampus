package com.example.trashcashcampus_mobile.models

/**
 * Data class for QR code scan results from the backend
 */
data class ScanResult(
    val success: Boolean = false,
    val status: String = "pending",
    val message: String,
    val pointsEarned: Int = 0,
    val totalPoints: Int = 0,
    val fact: String = ""
) 