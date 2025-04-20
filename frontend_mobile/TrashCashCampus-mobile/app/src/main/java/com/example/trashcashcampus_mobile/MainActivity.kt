package com.example.trashcashcampus_mobile
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
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
    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Setup password visibility toggle
        setupPasswordVisibilityToggle()

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

    private fun setupPasswordVisibilityToggle() {
        val passwordToggle = findViewById<ImageView>(R.id.ivPasswordToggle)
        val passwordEditText = findViewById<EditText>(R.id.etPassword)

        passwordToggle.setOnClickListener {
            passwordVisible = !passwordVisible

            if (passwordVisible) {
                // Show password
                passwordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                passwordToggle.setImageResource(R.drawable.ic_visibility)
            } else {
                // Hide password
                passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                passwordToggle.setImageResource(R.drawable.ic_visibility_off)
            }

            // Move cursor to the end of text
            passwordEditText.setSelection(passwordEditText.text.length)
        }
    }

    private fun signInWithEmailPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, check if email is verified
                    val user = auth.currentUser
                    val userId = user?.uid

                    if (user != null && userId != null) {
                        // First verify the user still exists in Firestore
                        db.collection("users").document(userId)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    // User exists in database, now check email verification
                                    if (user.isEmailVerified) {
                                        // Email is verified, proceed to home screen
                                        Log.d(TAG, "Login successful for user: $email")

                                        // Save user info to shared preferences
                                        saveUserSession(userId, email)

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
                                    // User doesn't exist in database anymore
                                    Log.w(TAG, "User record not found in Firestore: $userId")
                                    Toast.makeText(this,
                                        "Account no longer exists. Please register again.",
                                        Toast.LENGTH_LONG).show()

                                    // Sign out the user and clear any local data
                                    auth.signOut()
                                    clearUserSession()
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error checking user in database", e)
                                Toast.makeText(this,
                                    "Login error: Unable to verify account",
                                    Toast.LENGTH_SHORT).show()

                                // Sign out to be safe
                                auth.signOut()
                            }
                    } else {
                        // User is null or userId is null (shouldn't happen normally)
                        Toast.makeText(this, "Login error: Invalid user data", Toast.LENGTH_SHORT).show()
                        auth.signOut()
                    }
                } else {
                    // Sign in failed
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(this,
                        "Email or password is incorrect",
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

    private fun clearUserSession() {
        val prefs = getSharedPreferences("TrashCashCampusPrefs", MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    override fun onStart() {
        super.onStart()

        // Check if user is already logged in via SharedPreferences
        val prefs = getSharedPreferences("TrashCashCampusPrefs", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("IS_LOGGED_IN", false)
        val userId = prefs.getString("USER_ID", null)

        // Check if user is already logged in via Firebase Auth
        val currentUser = auth.currentUser

        if (currentUser != null && isLoggedIn && userId != null) {
            // Verify the user still exists in the database
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists() && currentUser.isEmailVerified) {
                        // User exists in database and email is verified
                        val intent = Intent(this, HomeActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // User no longer exists in database or email not verified
                        auth.signOut()
                        clearUserSession()
                    }
                }
                .addOnFailureListener { e ->
                    // Error checking database
                    Log.w(TAG, "Error checking user in database", e)
                    auth.signOut()
                    clearUserSession()
                }
        } else if (currentUser != null) {
            // User is logged in but email might not be verified
            // Sign them out to be safe
            auth.signOut()
            clearUserSession()
        }
    }
}