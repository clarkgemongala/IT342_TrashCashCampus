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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification_waiting)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

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
                    // We already have the right user, just send verification email
                    currentUser.sendEmailVerification().await()
                    showSuccess()
                } else {
                    // Try a temporary login to send verification
                    try {
                        // Try accessing our backend to request verification instead
                        val response = ApiClient.requestEmailVerification(this@VerificationWaitingActivity, userEmail)
                        if (response != null) {
                            showSuccess()
                        } else {
                            showError("Failed to send verification email through the server.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending verification email: ${e.message}", e)
                        showError("Failed to send verification email: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending verification email: ${e.message}", e)
                showError("Failed to send verification email: ${e.message}")
            }
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