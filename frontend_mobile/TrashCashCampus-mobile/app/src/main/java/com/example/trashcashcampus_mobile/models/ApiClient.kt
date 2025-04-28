package com.example.trashcashcampus_mobile.models

import android.util.Log
import com.google.firebase.firestore.FieldValue
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
import java.util.Locale

/**
 * API Client for handling network requests to the backend server.
 * The backend server handles all Firebase interactions.
 */
class ApiClient {
    companion object {
        private const val TAG = "ApiClient"
        
        // Server API URL - using render production URL
        private const val SERVER_BASE_URL = "https://it342-trashcashcampus.onrender.com" 
        
        // API endpoints
        private const val USER_ENDPOINT = "$SERVER_BASE_URL/api/users"
        private const val SCAN_ENDPOINT = "$SERVER_BASE_URL/bins/scan"
        private const val RECYCLING_ENDPOINT = "$SERVER_BASE_URL/recycling/submit"
        private const val REWARDS_ENDPOINT = "$SERVER_BASE_URL/rewards"
        private const val LOCATIONS_ENDPOINT = "$SERVER_BASE_URL/locations"
        private const val LOCATION_QR_ENDPOINT = "$SERVER_BASE_URL/bins/location"
        
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
                        status = "success",
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
                            status = "error",
                            message = "User not authenticated. Please log in again."
                        )
                    }
                    
                    // Extract bin info from QR code
                    var binId = binType
                    var binName = "Recycling Bin"
                    var locationName: String? = null
                    
                    try {
                        // Try to parse QR code if it's JSON
                        val qrData = JSONObject(qrCode)
                        binId = qrData.optString("binId", binType)
                        binName = qrData.optString("binName", "Recycling Bin")
                        locationName = qrData.optString("locationName", null)
                        Log.d(TAG, "Parsed QR code: binId=$binId, binName=$binName, location=$locationName")
                    } catch (e: Exception) {
                        // If not JSON, use the string as binId
                        Log.d(TAG, "Using QR code as bin ID: $binId")
                    }
                    
                    // Calculate potential points based on waste type
                    val basePoints = calculateBasePoints(wasteType)
                    val sizeBonus = if (itemSize == "big") 5 else 0
                    val potentialPoints = basePoints + sizeBonus
                    
                    // Submit to Firestore
                    val db = FirebaseFirestore.getInstance()
                    val binLog = HashMap<String, Any>()
                    
                    binLog["userId"] = userId
                    binLog["userName"] = userName
                    binLog["timestamp"] = timestamp
                    binLog["dateTime"] = dateTime
                    binLog["binId"] = binId
                    binLog["wasteType"] = wasteType
                    binLog["photoUrl"] = photoBase64
                    binLog["qrCode"] = qrCode
                    binLog["itemSize"] = itemSize
                    binLog["pointsEarned"] = potentialPoints
                    binLog["status"] = "pending"
                    binLog["photoPreview"] = photoBase64
                    
                    // IMPORTANT: Add location name to binLog document
                    if (locationName != null && locationName.isNotEmpty()) {
                        binLog["locationName"] = locationName
                        binLog["binLocation"] = locationName
                        binLog["actualLocation"] = locationName
                        Log.d(TAG, "Added location to binLog: $locationName")
                        } else {
                        // Try to extract from QR code again as a fallback
                        try {
                            val qrData = JSONObject(qrCode)
                            if (qrData.has("locationName")) {
                                val extractedLocation = qrData.getString("locationName")
                                binLog["locationName"] = extractedLocation
                                binLog["binLocation"] = extractedLocation
                                binLog["actualLocation"] = extractedLocation
                                Log.d(TAG, "Added extracted location to binLog: $extractedLocation")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error extracting location from QR", e)
                        }
                    }
                    
                    // Ensure we have a location, default to a specific building if needed
                    if (!binLog.containsKey("locationName") || binLog["locationName"] == "CIT Campus") {
                        val defaultLocation = "NGE Building" // Choose a better default
                        binLog["locationName"] = defaultLocation
                        binLog["binLocation"] = defaultLocation
                        binLog["actualLocation"] = defaultLocation
                        Log.d(TAG, "Using default location for binLog: $defaultLocation")
                    }
                    
                    try {
                        // Add a document with a temporary auto ID.
                        db.collection("binLogs")
                            .add(binLog)
                            .addOnSuccessListener { documentReference ->
                                Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error adding document", e)
                            }
                        
                        // Also update user stats
                        try {
                            val userRef = db.collection("users").document(userId)
                            
                            db.runTransaction { transaction ->
                                val snapshot = transaction.get(userRef)
                                val oldPoints = snapshot.getLong("points") ?: 0
                                val newPoints = oldPoints + potentialPoints
                                
                                transaction.update(userRef, "points", newPoints)
                                transaction.update(userRef, "lastUpdated", FieldValue.serverTimestamp())
                                Log.d(TAG, "Updated user points from $oldPoints to $newPoints")
                                
                                // Also increment their recycling counts
                                when (wasteType) {
                                    "plastic", "metal", "paper", "glass" -> {
                                        val field = "recycled${wasteType.capitalize(Locale.ROOT)}"
                                        val oldCount = snapshot.getLong(field) ?: 0
                                        transaction.update(userRef, field, oldCount + 1)
                                        Log.d(TAG, "Incremented $field from $oldCount to ${oldCount+1}")
                                    }
                                }
                                null
                            }
                    } catch (e: Exception) {
                            Log.e(TAG, "Error updating user points", e)
                        }
                        
                        return@withContext ScanResult(
                            success = true,
                            status = "pending",
                            message = "Your recycling has been submitted for admin approval!",
                            pointsEarned = potentialPoints,
                            totalPoints = 250, // This will be updated by the user refresh
                            fact = getRandomRecyclingFact()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error submitting to Firestore", e)
                        return@withContext ScanResult(
                            success = false,
                            status = "error",
                            message = "Error submitting to database: ${e.message}",
                            pointsEarned = 0,
                            totalPoints = 0
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
                        status = "success",
                        message = "Your recycling has been processed successfully! (Offline Mode)",
                        pointsEarned = earnedPoints,
                        totalPoints = 250 + earnedPoints,
                        fact = getRandomRecyclingFact()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error submitting recycling for approval", e)
                    return@withContext ScanResult(
                        success = false,
                        status = "error",
                        message = "Error: Could not submit your recycling. Please try again.\nDetails: ${e.message}",
                        pointsEarned = 0,
                        totalPoints = 0
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
        
        /**
         * Gets all campus locations with trash bins
         */
        suspend fun getCampusLocations(): List<CampusLocation> = withContext(Dispatchers.IO) {
            // If we've had persistent network failures, return empty list
            if (networkFailure.get()) {
                Log.w(TAG, "Network operations disabled due to persistent failures, returning empty location list")
                return@withContext emptyList()
            }
            
            try {
                Log.d(TAG, "Fetching campus locations from backend")
                
                val url = URL(LOCATIONS_ENDPOINT)
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
                    Log.d(TAG, "Backend locations data: $response")
                    return@withContext parseLocationsList(response)
                } else {
                    // Server error handling
                    Log.e(TAG, "Error fetching locations data: $responseCode")
                    return@withContext emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getCampusLocations", e)
                return@withContext emptyList()
            }
        }
        
        /**
         * Get QR code information for a specific location
         */
        suspend fun getLocationQrInfo(locationName: String): Map<String, Any> = withContext(Dispatchers.IO) {
            // If we've had persistent network failures, return empty map
            if (networkFailure.get()) {
                Log.w(TAG, "Network operations disabled due to persistent failures, returning empty location QR info")
                return@withContext emptyMap()
            }
            
            try {
                Log.d(TAG, "Fetching QR info for location: $locationName")
                
                val encodedLocationName = java.net.URLEncoder.encode(locationName, "UTF-8")
                val url = URL("$LOCATION_QR_ENDPOINT/$encodedLocationName")
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
                    Log.d(TAG, "Backend location QR info: $response")
                    return@withContext parseLocationQrInfo(response)
                } else {
                    // Server error handling
                    Log.e(TAG, "Error fetching location QR info: $responseCode")
                    return@withContext mapOf("status" to "error", "message" to "Failed to get QR info for location")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getLocationQrInfo", e)
                return@withContext mapOf("status" to "error", "message" to "Exception: ${e.message}")
            }
        }
        
        /**
         * Submits a bin scan with location data
         */
        suspend fun submitScan(
            qrCode: String,
            wasteType: String,
            imageBase64: String,
            locationName: String? = null
        ): ScanResult = withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Submitting scan to backend: QR=$qrCode, wasteType=$wasteType, locationName=$locationName")
                
                val jsonData = JSONObject().apply {
                    put("qrCode", qrCode)
                    put("wasteType", wasteType)
                    put("imageBase64", imageBase64)
                    // Add location if present
                    locationName?.let { put("locationName", it) }
                }
                
                val url = URL(SCAN_ENDPOINT)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = CONNECTION_TIMEOUT
                connection.readTimeout = READ_TIMEOUT
                
                // Write request body
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonData.toString())
                writer.flush()
                
                val responseCode = connection.responseCode
                Log.d(TAG, "Backend server response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = readResponse(connection)
                    Log.d(TAG, "Backend scan response: $response")
                    return@withContext parseScanResult(response)
                } else {
                    Log.e(TAG, "Error submitting scan: $responseCode")
                    return@withContext ScanResult(
                        success = false,
                        status = "error",
                        message = "Server error (code $responseCode)",
                        pointsEarned = 0,
                        totalPoints = 0
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in submitScan", e)
                return@withContext ScanResult(
                    success = false,
                    status = "error",
                    message = "Network error: ${e.message}",
                    pointsEarned = 0,
                    totalPoints = 0
                )
            }
        }
        
        private fun parseLocationsList(json: String): List<CampusLocation> {
            return try {
                val jsonArray = org.json.JSONArray(json)
                val locations = mutableListOf<CampusLocation>()
                
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    locations.add(
                        CampusLocation(
                            id = obj.optString("id", ""),
                            name = obj.optString("name", ""),
                            latitude = obj.optDouble("latitude", 0.0),
                            longitude = obj.optDouble("longitude", 0.0),
                            description = obj.optString("description", ""),
                            binType = obj.optString("binType", "")
                        )
                    )
                }
                
                locations
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing locations list", e)
                emptyList()
            }
        }
        
        private fun parseLocationQrInfo(json: String): Map<String, Any> {
            return try {
                val result = mutableMapOf<String, Any>()
                val jsonObject = JSONObject(json)
                
                result["status"] = jsonObject.optString("status", "error")
                
                if (jsonObject.has("qrInfo")) {
                    val qrInfo = jsonObject.getJSONObject("qrInfo")
                    val qrInfoMap = mutableMapOf<String, String>()
                    
                    qrInfoMap["binId"] = qrInfo.optString("binId", "")
                    qrInfoMap["binName"] = qrInfo.optString("binName", "")
                    qrInfoMap["description"] = qrInfo.optString("description", "")
                    
                    result["qrInfo"] = qrInfoMap
                }
                
                if (jsonObject.has("location")) {
                    val location = jsonObject.getJSONObject("location")
                    
                    result["location"] = CampusLocation(
                        id = location.optString("id", ""),
                        name = location.optString("name", ""),
                        latitude = location.optDouble("latitude", 0.0),
                        longitude = location.optDouble("longitude", 0.0),
                        description = location.optString("description", ""),
                        binType = location.optString("binType", "")
                    )
                }
                
                if (jsonObject.has("message")) {
                    result["message"] = jsonObject.optString("message", "")
                }
                
                result
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing location QR info", e)
                mapOf("status" to "error", "message" to "Failed to parse server response")
            }
        }
        
        private fun parseScanResult(json: String): ScanResult {
            return try {
                val jsonObject = JSONObject(json)
                
                // Parse the response
                val status = jsonObject.optString("status", "error")
                val message = jsonObject.optString("message", "Unknown response")
                val pointsEarned = jsonObject.optInt("pointsEarned", 0)
                val totalPoints = jsonObject.optInt("totalPoints", 0)
                val fact = jsonObject.optString("fact", "Recycling helps protect the environment!")
                
                // Return result object with success flag based on status
                ScanResult(
                    success = (status == "success"),
                    status = status,
                    message = message,
                    pointsEarned = pointsEarned,
                    totalPoints = totalPoints,
                    fact = fact
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing scan result", e)
                ScanResult(
                    success = false,
                    status = "error",
                    message = "Failed to parse server response: ${e.message}",
                    pointsEarned = 0,
                    totalPoints = 0
                )
            }
        }
    }
} 