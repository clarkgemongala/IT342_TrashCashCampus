package com.example.trashcashcampus_mobile

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class VerificationWaitingActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification_waiting)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Get the user email from intent
        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""

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
        val user = auth.currentUser
        user?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this,
                        "Verification email resent to $userEmail",
                        Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this,
                        "Failed to resend verification email: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }
}