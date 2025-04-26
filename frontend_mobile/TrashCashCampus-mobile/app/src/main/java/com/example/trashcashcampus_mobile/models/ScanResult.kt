package com.example.trashcashcampus_mobile.models

/**
 * Data class representing the result of a bin scanning operation.
 * 
 * @property success Whether the scan was successful
 * @property message User-friendly message about the scan result
 * @property pointsEarned Points earned from this recycling activity
 * @property totalPoints Total points the user now has
 * @property fact An educational fact about recycling
 */
data class ScanResult(
    val success: Boolean,
    val message: String,
    val pointsEarned: Int = 0,
    val totalPoints: Int = 0,
    val fact: String = ""
) 