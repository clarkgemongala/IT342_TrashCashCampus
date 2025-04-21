package com.example.trashcashcampus_mobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class HomeActivity : AppCompatActivity() {
    private val tag = "HomeActivity"

    // UI elements
    private lateinit var tvUserName: TextView
    private lateinit var tvTotalPoints: TextView
    private lateinit var tvPointsIncrement: TextView
    private lateinit var tvWeeklyGoal: TextView
    private lateinit var tvGoalProgress: TextView
    private lateinit var btnLogout: ImageButton
    private lateinit var btnViewAllRewards: Button
    private lateinit var btnScanQR: Button
    
    // Firebase
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        
        // Initialize UI elements
        initializeUI()
        
        // Load user data
        loadUserData()
        
        // Set up button click listeners
        setupListeners()
    }
    
    private fun initializeUI() {
        // Set up user info UI elements
        tvUserName = findViewById(R.id.tvUserName)
        tvTotalPoints = findViewById(R.id.tvTotalPoints)
        tvPointsIncrement = findViewById(R.id.tvPointsIncrement)
        tvWeeklyGoal = findViewById(R.id.tvWeeklyGoal)
        tvGoalProgress = findViewById(R.id.tvGoalProgress)
        
        // Set up buttons
        btnLogout = findViewById(R.id.btnLogout)
        btnViewAllRewards = findViewById(R.id.btnViewAllRewards)
        btnScanQR = findViewById(R.id.btnScanQR)
    }
    
    private fun loadUserData() {
        // Get current user
        val currentUser = auth.currentUser
        
        if (currentUser != null) {
            // Get user details from Firestore
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Extract user data
                        val userData = document.data
                        val fullName = userData?.get("fullName") as? String ?: "User"
                        
                        // Update UI with user data
                        tvUserName.text = fullName
                        
                        // For now, set default values for other fields
                        // In a real app, these would come from the database
                        tvTotalPoints.text = "250"
                        tvPointsIncrement.text = "+35"
                        tvWeeklyGoal.text = "1000"
                        tvGoalProgress.text = "25% completed"
                        
                        Log.d(tag, "User data loaded successfully")
                    } else {
                        Log.d(tag, "No such document")
                        tvUserName.text = currentUser.email?.split("@")?.get(0) ?: "User"
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(tag, "Error getting user data", exception)
                    tvUserName.text = currentUser.email?.split("@")?.get(0) ?: "User"
                    
                    // Show error message
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
                }
        } else {
            // No user signed in - should not happen, but handle it anyway
            Log.e(tag, "No user signed in")
            navigateToLogin()
        }
    }
    
    private fun setupListeners() {
        btnViewAllRewards.setOnClickListener {
            Toast.makeText(this, "View All Rewards clicked", Toast.LENGTH_SHORT).show()
            // Navigate to rewards page
        }

        btnScanQR.setOnClickListener {
            Toast.makeText(this, "Scan QR clicked", Toast.LENGTH_SHORT).show()
            // Open camera for QR scanning
        }

        // Set up logout button
        btnLogout.setOnClickListener {
            // Clear user session data
            clearUserSession()

            // Show logout message
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

            // Navigate back to login screen
            navigateToLogin()
        }
    }
    
    private fun navigateToLogin() {
        // Navigate back to MainActivity (login screen)
        val intent = Intent(this, MainActivity::class.java)
        // Clear back stack so user can't go back to HomeActivity after logout
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun clearUserSession() {
        // Clear SharedPreferences data
        val prefs = getSharedPreferences("TrashCashPrefs", MODE_PRIVATE)
        prefs.edit().apply {
            remove("userId")
            remove("email")
            apply()
        }

        // Sign out from Firebase Auth
        auth.signOut()
    }
}