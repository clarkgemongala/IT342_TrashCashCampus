package com.example.trashcashcampus_mobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * This activity acts as a verification handler rather than a UI screen.
 * It receives login credentials from MainActivity, verifies them with Firebase,
 * checks email verification status, and returns results to MainActivity.
 */
class LoginActivity : AppCompatActivity() {
    private val tag = "LoginActivity"
    
    // Firebase Auth
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Initialize Firebase Auth
            auth = FirebaseAuth.getInstance()
            
            // Check if we have data passed from MainActivity
            val email = intent.getStringExtra("email") ?: ""
            val password = intent.getStringExtra("password") ?: ""
            
            if (email.isNotEmpty() && password.isNotEmpty()) {
                // If we have credentials, attempt login directly
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
        
        // Attempt login with Firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login successful
                    val user = auth.currentUser
                    checkEmailVerification(user)
                } else {
                    // Login failed
                    Log.w(tag, "signInWithEmail:failure", task.exception)
                    returnToMainActivity("Authentication failed: ${task.exception?.message}")
                }
            }
    }
    
    private fun checkEmailVerification(user: FirebaseUser?) {
        if (user != null) {
            // Check if the user's email is verified
            if (user.isEmailVerified) {
                // Update verification status in Firestore if needed
                updateVerificationStatusInFirestore(user.uid)
                
                // Navigate back to MainActivity with success
                returnToMainActivityWithSuccess(user)
            } else {
                // Email not verified - show message and sign out
                auth.signOut()
                returnToMainActivity("You need to verify your email first. Please check your inbox.")
            }
        } else {
            returnToMainActivity("User account not found")
        }
    }
    
    private fun updateVerificationStatusInFirestore(userId: String) {
        try {
            val db = Firebase.firestore
            
            // Update the isEmailVerified field to true
            db.collection("users").document(userId)
                .update("isEmailVerified", true)
                .addOnSuccessListener {
                    Log.d(tag, "Email verification status updated in Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e(tag, "Error updating email verification status: ${e.message}", e)
                }
        } catch (e: Exception) {
            Log.e(tag, "Error accessing Firestore: ${e.message}", e)
        }
    }
    
    private fun returnToMainActivity(message: String) {
        val intent = Intent()
        intent.putExtra("message", message)
        intent.putExtra("success", false)
        setResult(RESULT_OK, intent)
        finish()
    }
    
    private fun returnToMainActivityWithSuccess(user: FirebaseUser) {
        val intent = Intent()
        intent.putExtra("message", "Login successful")
        intent.putExtra("success", true)
        intent.putExtra("userId", user.uid)
        intent.putExtra("email", user.email)
        setResult(RESULT_OK, intent)
        finish()
    }
} 