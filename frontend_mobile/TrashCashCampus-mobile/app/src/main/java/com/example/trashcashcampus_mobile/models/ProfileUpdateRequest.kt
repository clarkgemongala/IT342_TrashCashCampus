package com.example.trashcashcampus_mobile.models

/**
 * Model class for profile update requests
 */
data class ProfileUpdateRequest(
    val name: String,
    val email: String? = null
) 