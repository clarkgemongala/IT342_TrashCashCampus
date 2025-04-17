package com.example.trashcashcampus_mobile
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

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

            // Check credentials against Firestore
            loginWithFirestore(email, password)
        }
    }

    private fun loginWithFirestore(email: String, password: String) {
        try {
            // Query Firestore for user with matching email
            db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        // No user found with that email
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                    } else {
                        // User found, check password
                        val userDoc = documents.documents[0]
                        val storedPassword = userDoc.getString("password")

                        if (storedPassword == password) {
                            // Password matches - login successful
                            Log.d(TAG, "Login successful for user: $email")

                            // Save user info to shared preferences or in-memory for the session
                            saveUserSession(userDoc.id, email)

                            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()

                            // Navigate to HomeActivity
                            val intent = Intent(this, HomeActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish() // Close login activity
                        } else {
                            // Password doesn't match
                            Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error checking credentials in Firestore", e)
                    Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            // Handle any exceptions
            Log.e(TAG, "Error during Firestore login", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
        // Check if user is already logged in via shared preferences
        val prefs = getSharedPreferences("TrashCashCampusPrefs", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("IS_LOGGED_IN", false)

        if (isLoggedIn) {
            // User is already logged in, go directly to HomeActivity
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}