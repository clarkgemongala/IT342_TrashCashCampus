package com.example.trashcashcampus_mobile.models

/**
 * Data class representing the result of a bin scan
 */
data class ScanResult(
    val success: Boolean,
    val pointsEarned: Int,
    val message: String,
    val totalPoints: Int,
    val fact: String
) 