package com.example.trashcashcampus_mobile.models

/**
 * Model class for login responses
 */
data class LoginResponse(
    val userId: String,
    val email: String,
    val token: String? = null
) 