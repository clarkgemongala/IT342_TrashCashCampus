package com.example.trashcashcampus_mobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.trashcashcampus_mobile.utils.ApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class VerificationWaitingActivity : AppCompatActivity() {
    private val TAG = "VerificationActivity"
    private var userEmail: String = ""
    private var userId: String = ""
    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification_waiting)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        functions = Firebase.functions

        // Get the user email and ID from intent
        userEmail = intent.getStringExtra("email") ?: ""
        userId = intent.getStringExtra("userId") ?: ""

        // Set the email in the UI
        val tvUserEmail = findViewById<TextView>(R.id.tvUserEmail)
        tvUserEmail.text = userEmail

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