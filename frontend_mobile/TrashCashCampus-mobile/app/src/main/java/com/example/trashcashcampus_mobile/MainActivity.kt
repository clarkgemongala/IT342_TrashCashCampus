package com.example.trashcashcampus_mobile
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Handle sign up button click
        findViewById<TextView>(R.id.tvSignUp).setOnClickListener {
            try {
                val intent = Intent(this, RegisterActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                // Log the exception to see what's happening
                Log.e(TAG, "Error starting RegisterActivity", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Handle login button click
        findViewById<AppCompatButton>(R.id.btnLogin).setOnClickListener {
            val email = findViewById<EditText>(R.id.etEmail).text.toString().trim()
            val password = findViewById<EditText>(R.id.etPassword).text.toString().trim()

            // Basic validation
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading indicator (if you have one)
            // loadingProgressBar.visibility = View.VISIBLE

            // Login with Firebase Authentication
            signInWithEmailPassword(email, password)
        }
    }

    private fun signInWithEmailPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, check if email is verified
                    val user = auth.currentUser

                    if (user != null && user.isEmailVerified) {
                        // Email is verified, proceed to home screen
                        Log.d(TAG, "Login successful for user: $email")

                        // Save user info to shared preferences
                        saveUserSession(user.uid, email)

                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()

                        // Go to home activity
                        val intent = Intent(this, HomeActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        // Email not verified, show message and log out
                        Toast.makeText(this,
                            "Please verify your email before logging in",
                            Toast.LENGTH_LONG).show()

                        // Take them to the verification waiting screen
                        val intent = Intent(this, VerificationWaitingActivity::class.java)
                        intent.putExtra("USER_EMAIL", email)
                        startActivity(intent)

                        // Sign out the user
                        auth.signOut()
                    }
                } else {
                    // Sign in failed
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(this,
                        "Login failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserSession(userId: String, email: String) {
        // Use SharedPreferences to save user login state
        val prefs = getSharedPreferences("TrashCashCampusPrefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("USER_ID", userId)
            putString("USER_EMAIL", email)
            putBoolean("IS_LOGGED_IN", true)
            apply()
        }
    }

    override fun onStart() {
        super.onStart()

        // Check if user is already logged in via Firebase Auth
        val currentUser = auth.currentUser

        if (currentUser != null && currentUser.isEmailVerified) {
            // User is already logged in and email is verified
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        } else if (currentUser != null) {
            // User is logged in but email might not be verified
            // Sign them out to be safe
            auth.signOut()

            // Clear shared preferences
            val prefs = getSharedPreferences("TrashCashCampusPrefs", MODE_PRIVATE)
            prefs.edit().clear().apply()
        }
    }
}