package com.example.trashcashcampus_mobile.utils

import java.util.Random

/**
 * Utility class for generating random voucher codes
 */
object VoucherGenerator {
    
    private const val CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private const val VOUCHER_LENGTH = 10
    
    /**
     * Generates a random alphanumeric voucher code
     * @param prefix Optional prefix for the voucher (e.g. category code)
     * @return Random voucher code in format: PREFIX-XXXX-XXXX-XXXX
     */
    fun generateVoucherCode(prefix: String = ""): String {
        val random = Random()
        val codeBuilder = StringBuilder()
        
        // Add prefix if provided
        if (prefix.isNotEmpty()) {
            codeBuilder.append(prefix).append("-")
        }
        
        // Generate 3 groups of 4 characters
        for (group in 0 until 3) {
            // Add separator between groups
            if (group > 0) {
                codeBuilder.append("-")
            }
            
            // Generate 4 random characters for this group
            for (i in 0 until 4) {
                val randomIndex = random.nextInt(CHARACTERS.length)
                codeBuilder.append(CHARACTERS[randomIndex])
            }
        }
        
        return codeBuilder.toString()
    }
} 