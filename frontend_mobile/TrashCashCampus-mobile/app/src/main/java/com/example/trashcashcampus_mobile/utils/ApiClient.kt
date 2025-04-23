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
            null
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
            val errorMessage = "Error: ${response.code()} - ${response.message()}"
            Log.e(TAG, "$operation failed: $errorMessage")
            showToast(context, "$operation failed: ${response.message()}")
            null
        }
    }
    
    private suspend fun handleError(context: Context, e: Exception, operation: String) {
        val errorMessage = when (e) {
            is SocketTimeoutException -> "Backend server not responding. Please make sure your backend is running."
            is ConnectException -> "Cannot connect to backend server. Please check your backend URL."
            is UnknownHostException -> "Backend host not found. Please check your network connection."
            is IOException -> "Network error while $operation. Please check your connection."
            else -> "Error while $operation: ${e.message}"
        }
        Log.e(TAG, errorMessage, e)
        showToast(context, errorMessage)
    }
    
    private suspend fun showToast(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
} 