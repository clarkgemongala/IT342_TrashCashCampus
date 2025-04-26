package com.example.trashcashcampus_mobile.models

/**
 * Data class representing user information and statistics
 */
data class UserData(
    val totalPoints: Int,          // Total accumulated points (overall)
    val recentPoints: Int,         // Points accumulated for the current day (resets daily)
    val weeklyGoal: Int,           // User's weekly recycling goal
    val weeklyProgress: Int        // Progress towards weekly goal
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