package com.example.trashcashcampus_mobile.models

/**
 * Model class for password update requests
 */
data class PasswordUpdateRequest(
    val oldPassword: String,
    val newPassword: String
) 