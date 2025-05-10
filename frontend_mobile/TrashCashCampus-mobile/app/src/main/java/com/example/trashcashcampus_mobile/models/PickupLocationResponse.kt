package com.example.trashcashcampus_mobile.models

/**
 * Model class for pickup location responses
 */
data class PickupLocationResponse(
    val locations: List<Map<String, Any>> = emptyList()
) 