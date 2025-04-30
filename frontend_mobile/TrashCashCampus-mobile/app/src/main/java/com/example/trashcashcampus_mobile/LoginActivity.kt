package com.example.trashcashcampus_mobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.trashcashcampus_mobile.utils.ApiClient
import com.example.trashcashcampus_mobile.utils.LoadingManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception
import android.widget.RelativeLayout

/**
 * This activity handles login through the backend API.
 * It receives login credentials from MainActivity, verifies them with the backend,
 * and returns results to MainActivity.
 */
class LoginActivity : AppCompatActivity() {
    private val tag = "LoginActivity"
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        auth = FirebaseAuth.getInstance()
        db = Firebase.firestore
        
        try {
            // Check if we have data passed from MainActivity
            val email = intent.getStringExtra("email") ?: ""
            val password = intent.getStringExtra("password") ?: ""
            
            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Show loading overlay
                LoadingManager.showLoading(this, "Authenticating...")
                
                // If we have credentials, attempt login through the backend API
                performLogin(email, password)
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
    
    private fun performLogin(email: String, password: String) {
        // Show loading state
        LoadingManager.showLoading(this, "Authenticating...")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // First attempt to authenticate with Firebase
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                
                if (firebaseUser != null) {
                    // Check if email is verified in Firebase Auth
                    if (firebaseUser.isEmailVerified) {
                        // Email is verified in Auth, make sure it's also updated in Firestore
                        updateFirestoreVerificationStatus(firebaseUser.uid)
                        
                        // Now authenticate with our backend
                        val loginResponse = ApiClient.login(this@LoginActivity, email, password)
                        
                        // Authentication successful
                        withContext(Dispatchers.Main) {
                            LoadingManager.hideLoading(this@LoginActivity)
                            setResult(RESULT_OK, Intent().apply {
                                putExtra("success", true)
                                putExtra("userId", loginResponse?.userId ?: firebaseUser.uid)
                                putExtra("email", firebaseUser.email)
                                putExtra("message", "Login successful")
                            })
                            finish()
                        }
                    } else {
                        // Email not verified, redirect to verification waiting screen
                        withContext(Dispatchers.Main) {
                            LoadingManager.hideLoading(this@LoginActivity)
                            
                            // Show a brief message
                            Toast.makeText(
                                this@LoginActivity,
                                "Please verify your email before logging in.",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            // Redirect to verification waiting screen
                            val intent = Intent(this@LoginActivity, VerificationWaitingActivity::class.java)
                            intent.putExtra("userId", firebaseUser.uid)
                            intent.putExtra("email", firebaseUser.email)
                            startActivity(intent)
                            
                            // Return to MainActivity with verification required message
                            setResult(RESULT_OK, Intent().apply {
                                putExtra("success", false)
                                putExtra("message", "EMAIL_VERIFICATION_REQUIRED")
                            })
                            finish()
                        }
                    }
                } else {
                    // No user returned from Firebase Auth
                    withContext(Dispatchers.Main) {
                        LoadingManager.hideLoading(this@LoginActivity)
                        Toast.makeText(
                            this@LoginActivity,
                            "Login failed: No user found",
                            Toast.LENGTH_LONG
                        ).show()
                        
                        // Return to MainActivity with failure message
                        returnToMainActivity("Login failed: No user found")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error during login: ${e.message}", e)
                
                // Handle different error cases
                val errorMessage = when {
                    e.message?.contains("network error") == true -> "Network error. Please check your connection."
                    e.message?.contains("password is invalid") == true -> "Invalid password. Please try again."
                    e.message?.contains("no user record") == true -> "No account found with this email."
                    e.message?.contains("EMAIL_VERIFICATION_REQUIRED") == true -> "Please verify your email before logging in."
                    else -> "Login failed: ${e.message}"
                }
                
                withContext(Dispatchers.Main) {
                    LoadingManager.hideLoading(this@LoginActivity)
                    Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
                    
                    // Return to MainActivity with error message
                    returnToMainActivity(errorMessage)
                }
            }
        }
    }
    
    // Function to update Firestore when user is verified in Firebase Auth
    private fun updateFirestoreVerificationStatus(uid: String) {
        try {
            // Update the Firestore document directly
            val userDoc = db.collection("users").document(uid)
            
            // First check if the document exists and if isEmailVerified is already true
            userDoc.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val isEmailVerified = document.getBoolean("isEmailVerified") ?: false
                        
                        // Only update if needed
                        if (!isEmailVerified) {
                            Log.d(tag, "Updating Firestore isEmailVerified to true for user $uid")
                            userDoc.update("isEmailVerified", true)
                                .addOnSuccessListener {
                                    Log.d(tag, "Firestore isEmailVerified updated successfully")
                                }
                                .addOnFailureListener { e ->
                                    Log.e(tag, "Error updating Firestore isEmailVerified: ${e.message}", e)
                                }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "Error checking/updating Firestore verification status: ${e.message}", e)
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
} 