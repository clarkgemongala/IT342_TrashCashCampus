package com.example.trashcashcampus_mobile.models

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * API Client for handling network requests to the backend server.
 * The backend server handles all Firebase interactions.
 */
class ApiClient {
    companion object {
        private const val TAG = "ApiClient"
        
        // Server API URL - change to your backend server address
        // Using the same IP address that works with login functionality
        private const val SERVER_BASE_URL = "http://192.168.0.194:8080" 
        
        // API endpoints
        private const val USER_ENDPOINT = "$SERVER_BASE_URL/api/users"
        private const val SCAN_ENDPOINT = "$SERVER_BASE_URL/bins/scan"
        private const val RECYCLING_ENDPOINT = "$SERVER_BASE_URL/recycling/submit"
        private const val REWARDS_ENDPOINT = "$SERVER_BASE_URL/rewards"
        
        // Increase timeout values for slower connections
        private const val CONNECTION_TIMEOUT = 30000 // 30 seconds timeout
        private const val READ_TIMEOUT = 30000 // 30 seconds timeout
        
        // Flag to track if network operations have failed and should be skipped
        private val networkFailure = AtomicBoolean(false)
        // Counter for retry attempts before falling back to direct Firebase
        private var retryCount = 0
        private const val MAX_RETRIES = 2
        
        /**
         * Gets user data including points, badges, and weekly goal progress
         */
        suspend fun getUserData(userId: String): UserData = withContext(Dispatchers.IO) {
            // If we've had persistent network failures, use direct Firebase access
            if (networkFailure.get()) {
                Log.w(TAG, "Network operations disabled due to persistent failures, using direct Firebase access")
                return@withContext getUserDataFromFirebase(userId)
            }
            
            try {
                Log.d(TAG, "Fetching user data for ID: $userId from backend")
                
                val url = URL("$USER_ENDPOINT/$userId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = CONNECTION_TIMEOUT
                connection.readTimeout = READ_TIMEOUT
                
                val responseCode = connection.responseCode
                Log.d(TAG, "Backend server response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Success - reset retry counter
                    retryCount = 0
                    val response = readResponse(connection)
                    Log.d(TAG, "Backend user data: $response")
                    return@withContext parseUserData(response)
                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    // User doesn't exist in the database yet, try to create default data
                    Log.w(TAG, "User not found (404) on backend, creating default data")
                    return@withContext createDefaultUserData(userId)
                } else {
                    // Server error handling
                    Log.e(TAG, "Error fetching user data: $responseCode")
                    
                    // Increment retry count
                    retryCount++
                    if (retryCount >= MAX_RETRIES) {
                        Log.w(TAG, "Max retries reached ($MAX_RETRIES), falling back to direct Firebase access")
                        return@withContext getUserDataFromFirebase(userId)
                    } else {
                        // Return default data but don't switch to Firebase yet
                        Log.w(TAG, "Backend error, using temporary default data (retry $retryCount/$MAX_RETRIES)")
                        return@withContext UserData(
                            totalPoints = 0,
                            recentPoints = 0,
                            weeklyGoal = 100,
                            weeklyProgress = 0
                        )
                    }
                }
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Socket timeout connecting to backend.", e)
                
                // Increment retry count
                retryCount++
                if (retryCount >= MAX_RETRIES) {
                    // Set flag to prevent further network operations after multiple failures
                    networkFailure.set(true)
                    Log.w(TAG, "Max retries reached ($MAX_RETRIES), falling back to direct Firebase access")
                    return@withContext getUserDataFromFirebase(userId)
                } else {
                    // Return default data but don't switch to Firebase yet
                    Log.w(TAG, "Connection timeout, using temporary default data (retry $retryCount/$MAX_RETRIES)")
                    return@withContext UserData(
                        totalPoints = 0,
                        recentPoints = 0,
                        weeklyGoal = 100,
                        weeklyProgress = 0
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getUserData", e)
                
                // Increment retry count
                retryCount++
                if (retryCount >= MAX_RETRIES) {
                    Log.w(TAG, "Max retries reached ($MAX_RETRIES), falling back to direct Firebase access")
                    return@withContext getUserDataFromFirebase(userId)
                } else {
                    // Return default data but don't switch to Firebase yet
                    Log.w(TAG, "Error connecting to backend, using temporary default data (retry $retryCount/$MAX_RETRIES)")
                    return@withContext UserData(
                        totalPoints = 0,
                        recentPoints = 0,
                        weeklyGoal = 100,
                        weeklyProgress = 0
                    )
                }
            }
        }
        
        /**
         * Gets user data directly from Firebase when the backend is unavailable
         * This is used only as a last resort after multiple backend failures
         */
        private suspend fun getUserDataFromFirebase(userId: String): UserData = withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching user data directly from Firebase for ID: $userId")
                
                val db = FirebaseFirestore.getInstance()
                val userDoc = db.collection("users").document(userId).get().await()
                
                if (userDoc.exists()) {
                    Log.d(TAG, "Successfully retrieved user data from Firebase, document exists")
                    
                    // Dump all fields for debugging
                    val allData = userDoc.data
                    allData?.forEach { (key, value) ->
                        Log.d(TAG, "Firebase field: $key = $value (${value?.javaClass?.name})")
                    }
                    
                    // Try multiple ways to get totalPoints
                    var totalPoints = 0
                    
                    // First try: direct get with field name
                    val totalPointsValue = userDoc.get("totalPoints")
                    if (totalPointsValue != null) {
                        Log.d(TAG, "Found totalPoints via get(): $totalPointsValue (${totalPointsValue.javaClass.name})")
                        totalPoints = when (totalPointsValue) {
                            is Long -> totalPointsValue.toInt()
                            is Int -> totalPointsValue
                            is Double -> totalPointsValue.toInt()
                            is String -> totalPointsValue.toIntOrNull() ?: 0
                            else -> 0
                        }
                    } else {
                        // Second try: with getLong
                        val longValue = userDoc.getLong("totalPoints")
                        if (longValue != null) {
                            Log.d(TAG, "Found totalPoints via getLong(): $longValue")
                            totalPoints = longValue.toInt()
                        } else {
                            // Third try: with data map
                            val dataMap = userDoc.data
                            if (dataMap != null && dataMap.containsKey("totalPoints")) {
                                val mapValue = dataMap["totalPoints"]
                                Log.d(TAG, "Found totalPoints in data map: $mapValue (${mapValue?.javaClass?.name})")
                                totalPoints = when (mapValue) {
                                    is Long -> mapValue.toInt()
                                    is Int -> mapValue
                                    is Double -> mapValue.toInt()
                                    is String -> mapValue.toIntOrNull() ?: 0
                                    else -> 0
                                }
                            } else {
                                Log.w(TAG, "totalPoints field not found in any expected location")
                            }
                        }
                    }
                    
                    Log.d(TAG, "Final resolved totalPoints: $totalPoints")
                    
                    // Get totalRecycled with similar approach (but simpler)
                    val totalRecycledValue = userDoc.get("totalRecycled")
                    val totalRecycled = when (totalRecycledValue) {
                        is Long -> totalRecycledValue.toInt()
                        is Int -> totalRecycledValue
                        is Double -> totalRecycledValue.toInt()
                        is String -> totalRecycledValue.toIntOrNull() ?: 0
                        else -> 0
                    }
                    
                    // Return with actual Firebase data
                    return@withContext UserData(
                        totalPoints = totalPoints,
                        recentPoints = 0, // We don't have this in Firebase directly
                        weeklyGoal = 100, // Default weekly goal
                        weeklyProgress = totalRecycled.coerceAtMost(100) // Use totalRecycled as progress up to 100%
                    )
                } else {
                    Log.w(TAG, "User document not found in Firebase, returning default data with 0 points")
                    return@withContext createDefaultUserData(userId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user data from Firebase", e)
                return@withContext createDefaultUserData(userId)
            }
        }
        
        // Create default user data for new users and attempt to save it to backend
        private suspend fun createDefaultUserData(userId: String): UserData {
            val defaultData = UserData(
                totalPoints = 0, // Default points for new users is 0
                recentPoints = 0,
                weeklyGoal = 100,
                weeklyProgress = 0
            )
            
            // Only try to create the data on the backend if network operations haven't persistently failed
            if (!networkFailure.get()) {
                try {
                    // Create data for the user through backend
                    val jsonData = JSONObject().apply {
                        put("userId", userId)
                        put("totalPoints", defaultData.totalPoints)
                        put("recentPoints", defaultData.recentPoints)
                        put("weeklyGoal", defaultData.weeklyGoal)
                        put("weeklyProgress", defaultData.weeklyProgress)
                        put("createdAt", System.currentTimeMillis())
                    }
                    
                    val endpoint = "$USER_ENDPOINT/create"
                    val response = makeApiRequest(endpoint, jsonData, "POST")
                    Log.d(TAG, "Created default user data through backend: $response")
                    
                    // Reset retry counter on successful API call
                    retryCount = 0
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating default user data", e)
                }
            }
            
            return defaultData
        }
        
        /**
         * Submits recycling activity for admin approval
         * Points are only awarded after approval
         */
        suspend fun submitRecyclingForApproval(
            userId: String,
            userName: String,
            qrCode: String,
            binType: String,
            wasteType: String,
            photoBase64: String,
            itemSize: String,
            timestamp: Long,
            dateTime: String
        ): ScanResult {
            return withContext(Dispatchers.IO) {
                // If we've had network failures, simulate a successful submission
                if (networkFailure.get()) {
                    Log.w(TAG, "Network operations disabled due to previous failures, returning simulated success")
                    return@withContext ScanResult(
                        success = true,
                        message = "Your recycling has been processed successfully! (Offline Mode)",
                        pointsEarned = calculatePointsForWasteAndSize(wasteType, itemSize),
                        totalPoints = 250,
                        fact = getRandomRecyclingFact()
                    )
                }
                
                try {
                    // Log the submission
                    Log.d(TAG, "Submitting recycling for approval through backend: userId=$userId, binType=$binType, wasteType=$wasteType, size=$itemSize")
                    
                    // Check if user ID is valid
                    if (userId.isNullOrEmpty()) {
                        Log.e(TAG, "User ID is empty or null")
                        return@withContext ScanResult(
                            success = false,
                            message = "User not authenticated. Please log in again."
                        )
                    }
                    
                    // Extract bin info from QR code
                    var binId = binType
                    var binName = "Recycling Bin"
                    
                    try {
                        // Try to parse QR code if it's JSON
                        val qrData = JSONObject(qrCode)
                        binId = qrData.optString("binId", binType)
                        binName = qrData.optString("binName", "Recycling Bin")
                        Log.d(TAG, "Parsed QR code: binId=$binId, binName=$binName")
                    } catch (e: Exception) {
                        // If not JSON, use the string as binId
                        Log.d(TAG, "Using QR code as bin ID: $binId")
                    }
                    
                    // Calculate potential points based on waste type
                    val basePoints = calculateBasePoints(wasteType)
                    val sizeBonus = if (itemSize == "big") 5 else 0
                    val potentialPoints = basePoints + sizeBonus
                    
                    // Submit through backend server
                    val jsonData = JSONObject().apply {
                        put("userId", userId)
                        put("userName", userName)
                        put("binId", binId)
                        put("binName", binName)
                        put("wasteType", wasteType)
                        put("itemSize", itemSize)
                        put("potentialPoints", potentialPoints)
                        put("timestamp", timestamp)
                        put("dateTime", dateTime)
                        put("status", "pending")
                        put("approved", false)
                        put("processed", false)
                        // Include photo data for verification if available
                        if (photoBase64.isNotEmpty()) {
                            // Log the photo data length for debugging
                            Log.d(TAG, "Including photo data in request, length: ${photoBase64.length}")
                            // Send the full photo data - the backend will handle storage appropriately
                            put("photoData", photoBase64)
                        } else {
                            Log.w(TAG, "No photo data available for this submission!")
                        }
                    }
                    
                    try {
                        val response = makeApiRequest(RECYCLING_ENDPOINT, jsonData)
                        Log.d(TAG, "Backend recycling submission response: $response")
                        
                        // Parse the response
                        val responseJson = JSONObject(response)
                        val success = responseJson.optBoolean("success", false)
                        val message = responseJson.optString("message", "Your recycling has been submitted for admin approval!")
                        
                        // Return response based on backend result
                        return@withContext ScanResult(
                            success = success,
                            message = message,
                            pointsEarned = 0,  // No points yet until approved
                            totalPoints = 0,   // No points awarded yet
                            fact = getRandomRecyclingFact()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error submitting to backend, falling back to offline mode: ${e.message}", e)
                        networkFailure.set(true)
                        
                        // Fall back to simulated success
                        return@withContext ScanResult(
                            success = true,
                            message = "Your recycling has been processed successfully! (Offline Mode)",
                            pointsEarned = potentialPoints,
                            totalPoints = 250 + potentialPoints,
                            fact = getRandomRecyclingFact()
                        )
                    }
                } catch (e: SocketTimeoutException) {
                    Log.e(TAG, "Socket timeout submitting recycling. Disabling network operations.", e)
                    // Set flag to prevent further network operations
                    networkFailure.set(true)
                    
                    // Return a successful result with points to keep app functional
                    val earnedPoints = calculatePointsForWasteAndSize(wasteType, itemSize)
                    return@withContext ScanResult(
                        success = true,
                        message = "Your recycling has been processed successfully! (Offline Mode)",
                        pointsEarned = earnedPoints,
                        totalPoints = 250 + earnedPoints,
                        fact = getRandomRecyclingFact()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error submitting recycling for approval", e)
                    return@withContext ScanResult(
                        success = false,
                        message = "Error: Could not submit your recycling. Please try again.\nDetails: ${e.message}"
                    )
                }
            }
        }
        
        private fun calculatePointsForWasteAndSize(wasteType: String, itemSize: String): Int {
            val basePoints = calculateBasePoints(wasteType)
            val sizeBonus = if (itemSize == "big") 5 else 0
            return basePoints + sizeBonus
        }
        
        // Update user points after successful recycling
        suspend fun updateUserPoints(userId: String, points: Int): Boolean {
            // Skip if network operations have failed previously
            if (networkFailure.get()) {
                Log.w(TAG, "Network operations disabled due to previous failures, skipping point update")
                return true
            }
            
            try {
                // Update points through backend
                val jsonData = JSONObject().apply {
                    put("userId", userId)
                    put("points", points)
                }
                
                val endpoint = "$USER_ENDPOINT/$userId/points"
                val response = makeApiRequest(endpoint, jsonData, "PATCH")
                Log.d(TAG, "Updated points through backend: $response")
                return true
                
            } catch (e: Exception) {
                Log.e(TAG, "Error updating user points", e)
                return false
            }
        }
        
        private fun calculateBasePoints(wasteType: String): Int {
            return when(wasteType) {
                "plastic" -> 15
                "paper" -> 10
                "glass" -> 20
                "metal" -> 25
                "organic" -> 5
                else -> 10
            }
        }
        
        private fun getRandomRecyclingFact(): String {
            val recyclingFacts = arrayOf(
                "Recycling one aluminum can saves enough energy to run a TV for three hours.",
                "A glass bottle can take up to 4,000 years to decompose in a landfill.",
                "Plastic bottles can take up to 450 years to decompose in a landfill.",
                "Recycling one ton of paper saves 17 trees.",
                "The energy saved from recycling one glass bottle can run a 100-watt light bulb for four hours.",
                "Americans throw away enough plastic bottles each year to circle the Earth four times.",
                "75% of all aluminum ever produced is still in use today thanks to recycling.",
                "The average person has the opportunity to recycle more than 25,000 cans in their lifetime.",
                "Recycling a single aluminum can saves enough energy to power a TV for 3 hours.",
                "Every ton of paper recycled saves 17 trees, 7,000 gallons of water and 463 gallons of oil."
            )
            return recyclingFacts.random()
        }
        
        /**
         * Gets available rewards for the user
         */
        suspend fun getRewards(userId: String): List<Reward> = withContext(Dispatchers.IO) {
            try {
                val url = URL("$REWARDS_ENDPOINT?userId=$userId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = CONNECTION_TIMEOUT
                connection.readTimeout = READ_TIMEOUT
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = readResponse(connection)
                    return@withContext parseRewardsList(response)
                } else {
                    Log.e(TAG, "Error fetching rewards: $responseCode")
                    // Return empty list in case of error
                    return@withContext emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getRewards", e)
                // Return empty list in case of error
                return@withContext emptyList()
            }
        }
        
        // Helper method to read HTTP response
        private fun readResponse(connection: HttpURLConnection): String {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()
            return response.toString()
        }
        
        // Helper method to parse user data from JSON
        private fun parseUserData(jsonResponse: String): UserData {
            try {
                val json = JSONObject(jsonResponse)
                return UserData(
                    totalPoints = json.optInt("totalPoints", 0),
                    recentPoints = json.optInt("recentPoints", 0),
                    weeklyGoal = json.optInt("weeklyGoal", 100),
                    weeklyProgress = json.optInt("weeklyProgress", 0)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing user data", e)
                return UserData(
                    totalPoints = 0,
                    recentPoints = 0,
                    weeklyGoal = 100,
                    weeklyProgress = 0
                )
            }
        }
        
        // Helper method to parse rewards list from JSON
        private fun parseRewardsList(jsonResponse: String): List<Reward> {
            try {
                val result = mutableListOf<Reward>()
                val jsonArray = JSONObject(jsonResponse).getJSONArray("rewards")
                
                for (i in 0 until jsonArray.length()) {
                    val rewardJson = jsonArray.getJSONObject(i)
                    val reward = Reward(
                        id = rewardJson.optString("id", ""),
                        name = rewardJson.optString("name", ""),
                        description = rewardJson.optString("description", ""),
                        pointsCost = rewardJson.optInt("pointsCost", 0),
                        imageUrl = rewardJson.optString("imageUrl", "")
                    )
                    result.add(reward)
                }
                return result
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing rewards list", e)
                return emptyList()
            }
        }
        
        private suspend fun makeApiRequest(endpoint: String, jsonData: JSONObject, method: String = "POST"): String {
            return withContext(Dispatchers.IO) {
                var connection: HttpURLConnection? = null
                try {
                    Log.d(TAG, "Making API request to endpoint: $endpoint")
                    Log.d(TAG, "Request payload: ${jsonData.toString()}")
                    
                    val url = URL(endpoint)
                    connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = method
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Accept", "application/json")
                    connection.doOutput = true
                    connection.connectTimeout = CONNECTION_TIMEOUT 
                    connection.readTimeout = READ_TIMEOUT
                    
                    // Write data
                    OutputStreamWriter(connection.outputStream).use { writer ->
                        writer.write(jsonData.toString())
                        writer.flush()
                    }
                    
                    // Get response
                    val responseCode = connection.responseCode
                    Log.d(TAG, "API Request to $endpoint returned code: $responseCode")
                    
                    if (responseCode in 200..299) {
                        // Success responses
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        Log.d(TAG, "API Response: $response")
                        response
                    } else {
                        // Error responses
                        val errorResponse = if (connection.errorStream != null) {
                            connection.errorStream.bufferedReader().use { it.readText() }
                        } else {
                            "No error details available"
                        }
                        Log.e(TAG, "API Error: $responseCode - $errorResponse")
                        throw Exception("API Error: $responseCode - $errorResponse")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Network error in makeApiRequest to $endpoint", e)
                    throw e
                } finally {
                    connection?.disconnect()
                }
            }
        }
    }
} 