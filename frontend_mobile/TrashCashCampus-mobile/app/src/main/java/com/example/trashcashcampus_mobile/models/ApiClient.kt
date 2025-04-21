package com.example.trashcashcampus_mobile.models

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * API Client for handling network requests to the backend server.
 */
class ApiClient {
    companion object {
        private const val TAG = "ApiClient"
        private const val BASE_URL = "http://10.0.2.2:8080" // 10.0.2.2 points to host's localhost from emulator
        
        // User endpoints
        private const val USER_ENDPOINT = "$BASE_URL/api/user"
        
        // Scan endpoints
        private const val SCAN_ENDPOINT = "$BASE_URL/bins/scan"
        
        // Rewards endpoints
        private const val REWARDS_ENDPOINT = "$BASE_URL/api/rewards"
        
        /**
         * Gets user data including points, badges, and weekly goal progress
         */
        suspend fun getUserData(userId: String): UserData = withContext(Dispatchers.IO) {
            try {
                val url = URL("$USER_ENDPOINT/$userId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = readResponse(connection)
                    parseUserData(response)
                } else {
                    Log.e(TAG, "Error fetching user data: $responseCode")
                    // Return default data in case of error
                    UserData(
                        totalPoints = 0,
                        recentPoints = 0,
                        weeklyGoal = 100,
                        weeklyProgress = 0
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getUserData", e)
                // Return default data in case of error
                UserData(
                    totalPoints = 0,
                    recentPoints = 0,
                    weeklyGoal = 100,
                    weeklyProgress = 0
                )
            }
        }
        
        /**
         * Submits a bin scan with QR code, waste type, and optional image
         */
        suspend fun scanBin(
            userId: String,
            qrCode: String,
            wasteType: String,
            imageBase64: String? = null
        ): ScanResult = withContext(Dispatchers.IO) {
            try {
                val requestJson = JSONObject()
                requestJson.put("userId", userId)
                requestJson.put("qrCode", qrCode)
                requestJson.put("wasteType", wasteType)
                if (imageBase64 != null) {
                    requestJson.put("imageBase64", imageBase64)
                }
                
                val url = URL(SCAN_ENDPOINT)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                
                // Write the request body
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(requestJson.toString())
                writer.flush()
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = readResponse(connection)
                    parseScanResult(response)
                } else {
                    Log.e(TAG, "Error scanning bin: $responseCode")
                    // Return default data in case of error
                    ScanResult(
                        success = false,
                        pointsEarned = 0,
                        message = "Failed to scan bin. Please try again.",
                        totalPoints = 0,
                        fact = ""
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in scanBin", e)
                // Return default data in case of error
                ScanResult(
                    success = false,
                    pointsEarned = 0,
                    message = "Error: ${e.message}",
                    totalPoints = 0,
                    fact = ""
                )
            }
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
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = readResponse(connection)
                    parseRewardsList(response)
                } else {
                    Log.e(TAG, "Error fetching rewards: $responseCode")
                    // Return empty list in case of error
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getRewards", e)
                // Return empty list in case of error
                emptyList()
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
        
        // Helper method to parse scan result from JSON
        private fun parseScanResult(jsonResponse: String): ScanResult {
            try {
                val json = JSONObject(jsonResponse)
                return ScanResult(
                    success = json.optString("status", "") == "success",
                    pointsEarned = json.optInt("pointsEarned", 0),
                    message = json.optString("message", ""),
                    totalPoints = json.optInt("totalPoints", 0),
                    fact = json.optString("fact", "")
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing scan result", e)
                return ScanResult(
                    success = false,
                    pointsEarned = 0,
                    message = "Error parsing response",
                    totalPoints = 0,
                    fact = ""
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
    }
} 