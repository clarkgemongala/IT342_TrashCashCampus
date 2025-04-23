package com.example.trashcashcampus_mobile.models

/**
 * Model class for registration requests
 */
data class RegistrationRequest(
    val email: String,
    val password: String,
    val name: String
) 