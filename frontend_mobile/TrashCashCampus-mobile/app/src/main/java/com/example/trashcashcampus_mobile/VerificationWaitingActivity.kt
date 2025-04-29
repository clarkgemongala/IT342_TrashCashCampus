package com.example.trashcashcampus_mobile

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.trashcashcampus_mobile.utils.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerificationWaitingActivity : AppCompatActivity() {
    private var userEmail: String = ""
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification_waiting)

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
        
        // Use the API client to request a new verification email
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // For now we're using the password reset endpoint since it sends an email
                // In a production app, we should create a dedicated endpoint for email verification
                val response = ApiClient.requestEmailVerification(this@VerificationWaitingActivity, userEmail, userId)
                
                withContext(Dispatchers.Main) {
                    if (response != null) {
                        Toast.makeText(
                            this@VerificationWaitingActivity,
                            "Verification email sent to $userEmail. Please check your inbox and spam folder.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@VerificationWaitingActivity,
                            "Failed to send verification email. Please try again later.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@VerificationWaitingActivity,
                        "Failed to send verification email: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}