package com.example.trashcashcampus_mobile.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.trashcashcampus_mobile.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Utility class to make API calls easier
 */
object ApiClient {
    private val TAG = "ApiClient"
    private val api by lazy { ApiService.create() }
    
    // Connection testing function
    
    suspend fun checkBackendConnection(context: Context): Boolean {
        return try {
            val response = api.ping()
            // Even if we get a 404, that means we connected to the server
            return response.code() < 500
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is SocketTimeoutException -> "Backend server not responding. Please make sure your backend is running."
                is ConnectException -> "Cannot connect to backend server. Please check your backend URL."
                is UnknownHostException -> "Backend host not found. Please check your network connection."
                is IOException -> "Network error while checking backend. Please check your connection."
                else -> "Error checking backend: ${e.message}"
            }
            Log.e(TAG, errorMessage, e)
            
            // Only show error if context is provided
            if (context != null) {
                showToast(context, errorMessage)
            }
            false
        }
    }
    
    // URL information for diagnostics
    
    fun getBackendUrl(): String {
        return ApiService.getBaseUrl()
    }
    
    // Authentication functions
    
    suspend fun login(context: Context, email: String, password: String): LoginResponse? {
        // First check if backend is reachable
        if (!checkBackendConnection(context)) {
            showToast(context, "Cannot connect to backend server. Please check your connection.")
            return null
        }
        
        return try {
            val response = api.login(LoginRequest(email, password))
            handleResponse(context, response, "Login")
        } catch (e: Exception) {
            handleError(context, e, "logging in")
            // Return null but let the exception propagate to the caller
            throw e
        }
    }
    
    suspend fun register(context: Context, email: String, password: String, name: String): Map<String, Any>? {
        // First check if backend is reachable
        if (!checkBackendConnection(context)) {
            showToast(context, "Cannot connect to backend server. Please check your connection.")
            return null
        }
        
        return try {
            val response = api.register(RegistrationRequest(email, password, name))
            handleResponse(context, response, "Registration")
        } catch (e: Exception) {
            handleError(context, e, "registering")
            null
        }
    }
    
    suspend fun requestPasswordReset(context: Context, email: String): Map<String, Any>? {
        return try {
            val response = api.requestPasswordReset(mapOf("email" to email))
            handleResponse(context, response, "Password reset request")
        } catch (e: Exception) {
            handleError(context, e, "requesting password reset")
            null
        }
    }
    
    // New method to request email verification
    suspend fun requestEmailVerification(context: Context, email: String, userId: String): Map<String, Any>? {
        return try {
            // For now, we're reusing the password reset endpoint
            // In a production app, we should have a dedicated email verification endpoint
            val response = api.requestEmailVerification(mapOf("email" to email, "userId" to userId))
            handleResponse(context, response, "Email verification request")
        } catch (e: Exception) {
            handleError(context, e, "requesting email verification")
            null
        }
    }
    
    // User profile functions
    
    suspend fun getProfile(context: Context, userId: String, token: String): ProfileResponse? {
        return try {
            val response = api.getProfile(userId, "Bearer $token")
            handleResponse(context, response, "Get profile")
        } catch (e: Exception) {
            handleError(context, e, "fetching profile")
            null
        }
    }
    
    suspend fun updateProfile(context: Context, userId: String, token: String, name: String): ProfileResponse? {
        return try {
            val response = api.updateProfile(userId, "Bearer $token", ProfileUpdateRequest(name))
            handleResponse(context, response, "Update profile")
        } catch (e: Exception) {
            handleError(context, e, "updating profile")
            null
        }
    }
    
    suspend fun updateEmail(context: Context, userId: String, token: String, email: String): EmailResponse? {
        return try {
            val response = api.updateUserEmail(userId, "Bearer $token", EmailRequest(email))
            handleResponse(context, response, "Update email")
        } catch (e: Exception) {
            handleError(context, e, "updating email")
            null
        }
    }
    
    suspend fun updatePassword(context: Context, userId: String, token: String, oldPassword: String, newPassword: String): Map<String, String>? {
        return try {
            val response = api.updatePassword(userId, "Bearer $token", PasswordUpdateRequest(oldPassword, newPassword))
            handleResponse(context, response, "Update password")
        } catch (e: Exception) {
            handleError(context, e, "updating password")
            null
        }
    }
    
    // User data functions
    
    suspend fun getUserData(context: Context, userId: String): UserData {
        Log.d(TAG, "Fetching user data for userId: $userId")
        
        return try {
            // First try to get user data from the backend API
            val response = api.getUserPoints(userId)
            
            if (response.isSuccessful) {
                val body = response.body()
                Log.d(TAG, "Successfully retrieved user data from API: $body")
                
                if (body != null) {
                    // Parse the points from the response
                    val totalPoints = body["totalPoints"] as? Int ?: 0
                    val recentPoints = body["recentPoints"] as? Int ?: 0
                    val weeklyGoal = body["weeklyGoal"] as? Int ?: 100
                    val weeklyProgress = body["weeklyProgress"] as? Int ?: 0
                    
                    UserData(
                        totalPoints = totalPoints,
                        recentPoints = recentPoints,
                        weeklyGoal = weeklyGoal,
                        weeklyProgress = weeklyProgress
                    )
                } else {
                    // Fallback to Firestore if the response body is null
                    getUserDataFromFirestore(userId)
                }
            } else {
                // Handle unsuccessful response
                Log.e(TAG, "Error getting user data: ${response.code()} - ${response.message()}")
                getUserDataFromFirestore(userId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching user data", e)
            getUserDataFromFirestore(userId)
        }
    }
    
    private suspend fun getUserDataFromFirestore(userId: String): UserData {
        Log.d(TAG, "Fetching user data from Firestore for userId: $userId")
        
        // This is a simplified implementation - it should be expanded with 
        // actual Firestore query, but for now we'll return a default object
        return UserData(
            totalPoints = 0,
            recentPoints = 0,
            weeklyGoal = 100,
            weeklyProgress = 0
        )
    }
    
    // Pickup location functions
    
    suspend fun getPickupLocations(context: Context): PickupLocationResponse? {
        return try {
            val response = api.getPickupLocations()
            handleResponse(context, response, "Get pickup locations")
        } catch (e: Exception) {
            handleError(context, e, "fetching pickup locations")
            null
        }
    }
    
    suspend fun getPickupLocationById(context: Context, id: String): PickupLocationResponse? {
        return try {
            val response = api.getPickupLocationById(id)
            handleResponse(context, response, "Get pickup location")
        } catch (e: Exception) {
            handleError(context, e, "fetching pickup location")
            null
        }
    }
    
    // Helper functions
    
    private suspend fun <T> handleResponse(context: Context, response: Response<T>, operation: String): T? {
        return if (response.isSuccessful) {
            response.body()
        } else {
            val errorCode = response.code()
            val errorBody = response.errorBody()?.string() ?: "No error details"
            
            // Try to parse the error message from the response
            val errorMessage = try {
                // Check if the error body contains a JSON response with a message field
                if (errorBody.contains("message")) {
                    val jsonObject = org.json.JSONObject(errorBody)
                    jsonObject.optString("message", "Unknown error")
                } else {
                    "Error: $errorCode - ${response.message()}"
                }
            } catch (e: Exception) {
                "Error: $errorCode - ${response.message()}"
            }
            
            // Log the detailed error
            Log.e(TAG, "$operation failed: $errorMessage (Body: $errorBody)")
            
            // Show a user-friendly message
            val userMessage = when (errorCode) {
                401 -> "Invalid credentials. Please check your email and password."
                403 -> "Access denied. You don't have permission for this operation."
                404 -> "Resource not found. Please try again later."
                429 -> "Too many requests. Please try again later."
                500, 502, 503, 504 -> "Server error. Please try again later."
                else -> "$operation failed: $errorMessage"
            }
            
            showToast(context, userMessage)
            
            // For authentication errors, always throw an exception so the calling code can handle it
            if (operation == "Login") {
                throw Exception(errorMessage)
            }
            
            null
        }
    }
    
    private suspend fun handleError(context: Context, e: Exception, operation: String) {
        val errorMessage = when (e) {
            is SocketTimeoutException -> "Backend server not responding. Please try again later."
            is ConnectException -> "Cannot connect to backend server. Please check your network connection."
            is UnknownHostException -> "Backend host not found. Please check your network connection."
            is IOException -> "Network error while $operation. Please check your connection."
            else -> {
                // Check if this is an authentication error
                if (e.message?.contains("Invalid credentials") == true || 
                    e.message?.contains("Authentication failed") == true) {
                    "Invalid credentials. Please check your email and password."
                } else {
                    "Error while $operation: ${e.message}"
                }
            }
        }
        
        Log.e(TAG, "Error while $operation", e)
        showToast(context, errorMessage)
        
        // Re-throw authentication errors so they can be handled by the UI
        if (operation == "logging in" && 
            (e.message?.contains("Invalid credentials") == true || 
             e.message?.contains("Authentication failed") == true)) {
            throw e
        }
    }
    
    private suspend fun showToast(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
} 