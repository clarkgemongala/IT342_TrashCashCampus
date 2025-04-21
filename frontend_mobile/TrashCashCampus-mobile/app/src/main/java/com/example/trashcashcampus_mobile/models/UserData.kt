package com.example.trashcashcampus_mobile.models

/**
 * Data class representing user information and statistics
 */
data class UserData(
    val totalPoints: Int,
    val recentPoints: Int,
    val weeklyGoal: Int,
    val weeklyProgress: Int
) {
    /**
     * Calculate progress as a percentage of the weekly goal
     */
    fun getProgressPercentage(): Int {
        return if (weeklyGoal > 0) {
            (weeklyProgress * 100) / weeklyGoal
        } else {
            0
        }
    }
    
    /**
     * Format the progress percentage as a string
     */
    fun getProgressText(): String {
        return "${getProgressPercentage()}% completed"
    }
} 