package com.example.trashcashcampus_mobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.trashcashcampus_mobile.utils.ApiClient
import com.example.trashcashcampus_mobile.utils.LoadingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This activity handles login through the backend API.
 * It receives login credentials from MainActivity, verifies them with the backend,
 * and returns results to MainActivity.
 */
class LoginActivity : AppCompatActivity() {
    private val tag = "LoginActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        try {
            // Check if we have data passed from MainActivity
            val email = intent.getStringExtra("email") ?: ""
            val password = intent.getStringExtra("password") ?: ""
            
            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Show loading overlay
                LoadingManager.showLoading(this, "Authenticating...")
                
                // If we have credentials, attempt login through the backend API
                loginUser(email, password)
            } else {
                // Go back to MainActivity if no credentials were provided
                Log.e(tag, "No login credentials provided")
                returnToMainActivity("Missing login credentials")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error in onCreate: ${e.message}", e)
            returnToMainActivity("Something went wrong. Please try again.")
        }
    }
    
    private fun loginUser(email: String, password: String) {
        // Validate inputs
        if (email.isEmpty()) {
            returnToMainActivity("Please enter your email")
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            returnToMainActivity("Please enter a valid email")
            return
        }
        
        if (password.isEmpty()) {
            returnToMainActivity("Please enter your password")
            return
        }
        
        // Launch a coroutine to make the API call
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Attempt login with backend API
                val loginResponse = ApiClient.login(this@LoginActivity, email, password)
                
                withContext(Dispatchers.Main) {
                    // Hide loading overlay
                    LoadingManager.hideLoading(this@LoginActivity)
                    
                    if (loginResponse != null) {
                        // Login successful
                        val userId = loginResponse.userId
                        val userEmail = loginResponse.email
                        val token = loginResponse.token
                        
                        // Save successful login to log
                        Log.d(tag, "Login successful for user: $userEmail")
                        
                        // Navigate back to MainActivity with success
                        returnToMainActivityWithSuccess(userId, userEmail, token)
                    } else {
                        // Login failed
                        Log.e(tag, "Login failed - loginResponse is null")
                        returnToMainActivity("Authentication failed. Please check your credentials.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Hide loading overlay
                    LoadingManager.hideLoading(this@LoginActivity)
                    
                    // Authentication failed
                    Log.e(tag, "Login failed with exception: ${e.message}")
                    returnToMainActivity("Authentication failed. Please check your credentials.")
                }
            }
        }
    }
    
    private fun returnToMainActivity(message: String) {
        // Hide any loading overlay before returning
        LoadingManager.hideLoading(this)
        
        val intent = Intent()
        intent.putExtra("message", message)
        intent.putExtra("success", false)
        setResult(RESULT_OK, intent)
        finish()
    }
    
    private fun returnToMainActivityWithSuccess(userId: String, email: String?, token: String?) {
        // Hide any loading overlay before returning
        LoadingManager.hideLoading(this)
        
        val intent = Intent()
        intent.putExtra("message", "Login successful")
        intent.putExtra("success", true)
        intent.putExtra("userId", userId)
        intent.putExtra("email", email)
        intent.putExtra("token", token)
        setResult(RESULT_OK, intent)
        finish()
    }
} 