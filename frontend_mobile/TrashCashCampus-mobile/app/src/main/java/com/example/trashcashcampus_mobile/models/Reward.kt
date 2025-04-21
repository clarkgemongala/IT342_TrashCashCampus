package com.example.trashcashcampus_mobile.models

/**
 * Data class representing a reward item
 */
data class Reward(
    val id: String,
    val name: String,
    val description: String,
    val pointsCost: Int,
    val imageUrl: String
) 