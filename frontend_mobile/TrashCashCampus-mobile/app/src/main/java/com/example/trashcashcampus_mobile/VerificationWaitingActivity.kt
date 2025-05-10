package com.example.trashcashcampus_mobile

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.trashcashcampus_mobile.utils.ApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception

class VerificationWaitingActivity : AppCompatActivity() {
    private val TAG = "VerificationActivity"
    private var userEmail: String = ""
    private var userId: String = ""
    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions
    private val db = Firebase.firestore
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private val POLLING_INTERVAL = 10000L // 10 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification_waiting)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        functions = Firebase.functions

        // Get the user email and ID from intent
        userEmail = intent.getStringExtra("email") ?: ""
        userId = intent.getStringExtra("userId") ?: ""

        Log.d(TAG, "Verification waiting for user: $userEmail ($userId)")

        // Set the email in the UI
        val tvUserEmail = findViewById<TextView>(R.id.tvUserEmail)
        tvUserEmail.text = userEmail

        // Initial check of verification status
        checkVerificationStatus()

        // Create a runnable for periodic verification checks
        runnable = Runnable {
            Log.d(TAG, "Running periodic verification check")
            checkVerificationStatus()
            handler.postDelayed(runnable, POLLING_INTERVAL)
        }

        // Add refresh button
        val btnRefreshStatus = findViewById<AppCompatButton>(R.id.btnRefreshStatus)
        btnRefreshStatus?.setOnClickListener {
            Toast.makeText(
                this@VerificationWaitingActivity,
                "Checking verification status...",
                Toast.LENGTH_SHORT
            ).show()
            checkVerificationStatus()
        }

        // Set up the resend button
        val btnResendEmail = findViewById<AppCompatButton>(R.id.btnResendEmail)
        btnResendEmail.setOnClickListener {
            resendVerificationEmail()
        }

        // Set up the continue to login button
        val btnContinueToLogin = findViewById<AppCompatButton>(R.id.btnContinueToLogin)
        btnContinueToLogin.setOnClickListener {
            // Go to login activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close this activity
        }
    }

    override fun onResume() {
        super.onResume()
        // Start periodic checking when activity is resumed
        handler.postDelayed(runnable, POLLING_INTERVAL)
    }

    override fun onPause() {
        super.onPause()
        // Stop periodic checking when activity is paused
        handler.removeCallbacks(runnable)
    }

    private fun checkVerificationStatus() {
        if (userEmail.isEmpty() || userId.isEmpty()) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Try to get the current user
                var currentUser = auth.currentUser
                
                // If not logged in, try to sign in with the provided email
                if (currentUser == null || currentUser.email != userEmail) {
                    try {
                        // Try to get user through Firebase admin SDK via our cloud function
                        val data = hashMapOf(
                            "email" to userEmail,
                            "userId" to userId
                        )
                        
                        val result = functions
                            .getHttpsCallable("checkEmailVerificationStatus")
                            .call(data)
                            .await()
                        
                        val isVerified = (result.data as? Map<*, *>)?.get("isVerified") as? Boolean ?: false
                        
                        if (isVerified) {
                            // Update Firestore
                            updateFirestoreVerificationStatus(userId)
                            
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@VerificationWaitingActivity,
                                    "Your email has been verified! You can now log in.",
                                    Toast.LENGTH_LONG
                                ).show()
                                
                                // Change the button text
                                val btnContinueToLogin = findViewById<AppCompatButton>(R.id.btnContinueToLogin)
                                btnContinueToLogin.text = "Continue to Login"
                            }
                            return@launch
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking verification status via function: ${e.message}", e)
                    }
                } else {
                    // Reload the Firebase user to get the latest verification status
                    currentUser.reload().await()
                    currentUser = auth.currentUser // Get fresh instance after reload
                    
                    if (currentUser != null && currentUser.isEmailVerified) {
                        // User is verified in Firebase Auth, update Firestore
                        updateFirestoreVerificationStatus(userId)
                        
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@VerificationWaitingActivity,
                                "Your email has been verified! You can now log in.",
                                Toast.LENGTH_LONG
                            ).show()
                            
                            // Change the button text
                            val btnContinueToLogin = findViewById<AppCompatButton>(R.id.btnContinueToLogin)
                            btnContinueToLogin.text = "Continue to Login"
                        }
                    }
                }
                
                // Also directly check Firestore to see if another client has already updated it
                val userDoc = db.collection("users").document(userId).get().await()
                if (userDoc != null && userDoc.exists()) {
                    val isEmailVerified = userDoc.getBoolean("isEmailVerified") ?: false
                    if (isEmailVerified) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@VerificationWaitingActivity,
                                "Your account is already verified in our database. You can now log in.",
                                Toast.LENGTH_LONG
                            ).show()
                            
                            // Change the button text
                            val btnContinueToLogin = findViewById<AppCompatButton>(R.id.btnContinueToLogin)
                            btnContinueToLogin.text = "Continue to Login"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking verification status: ${e.message}", e)
            }
        }
    }

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
                            Log.d(TAG, "Updating Firestore isEmailVerified to true for user $uid")
                            userDoc.update(
                                mapOf(
                                    "isEmailVerified" to true,
                                    "lastUpdated" to com.google.firebase.Timestamp.now()
                                )
                            )
                                .addOnSuccessListener {
                                    Log.d(TAG, "Firestore isEmailVerified updated successfully")
                                    
                                    // Also notify the backend of the change
                                    notifyBackendOfVerification(userEmail, uid)
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error updating Firestore isEmailVerified: ${e.message}", e)
                                }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking/updating Firestore verification status: ${e.message}", e)
        }
    }
    
    private fun notifyBackendOfVerification(email: String, uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Call backend API to update verification status
                val response = ApiClient.verifyEmail(this@VerificationWaitingActivity, email, "verified")
                if (response != null) {
                    Log.d(TAG, "Backend notified of email verification: $response")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying backend of verification: ${e.message}", e)
            }
        }
    }

    private fun resendVerificationEmail() {
        // Show loading toast
        Toast.makeText(
            this@VerificationWaitingActivity,
            "Sending verification email...",
            Toast.LENGTH_SHORT
        ).show()
        
        // First check if we have a logged-in user or need to sign in first
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = auth.currentUser
                
                if (currentUser != null && currentUser.email == userEmail) {
                    // We already have the right user, try to send verification email via Firebase Auth
                    try {
                        currentUser.sendEmailVerification().await()
                        showSuccess()
                    } catch (e: Exception) {
                        Log.w(TAG, "Firebase Auth sendEmailVerification failed, trying custom function", e)
                        sendVerificationViaFunction()
                    }
                } else {
                    // Try sending via our custom Firebase function
                    sendVerificationViaFunction()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending verification email: ${e.message}", e)
                // Fall back to API server if Firebase methods fail
                fallbackToApiServer()
            }
        }
    }

    private suspend fun sendVerificationViaFunction() {
        try {
            // Call our custom Firebase function to send the verification email
            val data = hashMapOf(
                "email" to userEmail,
                "continueUrl" to "https://trashcash-campus.netlify.app/emailVerified"
            )
            
            val result = functions
                .getHttpsCallable("sendCustomVerificationEmail")
                .call(data)
                .await()
            
            val success = (result.data as? Map<*, *>)?.get("success") as? Boolean ?: false
            
            if (success) {
                showSuccess()
            } else {
                Log.w(TAG, "Function result indicated failure, trying API server")
                fallbackToApiServer()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Firebase function: ${e.message}", e)
            fallbackToApiServer()
        }
    }
    
    private suspend fun fallbackToApiServer() {
        try {
            // Try accessing our backend to request verification
            val response = ApiClient.requestEmailVerification(this@VerificationWaitingActivity, userEmail)
            if (response != null) {
                showSuccess()
            } else {
                showError("Failed to send verification email through the server.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending verification email via API: ${e.message}", e)
            showError("Failed to send verification email: ${e.message}")
        }
    }
    
    private suspend fun showSuccess() {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                this@VerificationWaitingActivity,
                "Verification email sent to $userEmail. Please check your inbox and spam folder.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private suspend fun showError(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                this@VerificationWaitingActivity,
                message,
                Toast.LENGTH_LONG
            ).show()
        }
    }
}