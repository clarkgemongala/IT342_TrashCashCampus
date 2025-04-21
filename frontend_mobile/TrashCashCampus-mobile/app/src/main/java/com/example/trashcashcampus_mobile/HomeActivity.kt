package com.example.trashcashcampus_mobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.trashcashcampus_mobile.models.ApiClient
import com.example.trashcashcampus_mobile.models.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

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
    private lateinit var progressBarLoading: ProgressBar
    
    // Firebase
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    
    // User data
    private var userData: UserData? = null
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        
        // Initialize UI elements
        initializeUI()
        
        // Get current user ID
        userId = auth.currentUser?.uid
        
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
        progressBarLoading = findViewById(R.id.progressBarLoading)
        
        // Set up buttons
        btnLogout = findViewById(R.id.btnLogout)
        btnViewAllRewards = findViewById(R.id.btnViewAllRewards)
        btnScanQR = findViewById(R.id.btnScanQR)
        
        // Show loading state
        showLoading(true)
    }
    
    private fun loadUserData() {
        // First, get the user's name from Firestore
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
                        
                        // Update UI with user's name
                        tvUserName.text = fullName
                        
                        // Now fetch stats from our API
                        fetchUserStats(currentUser.uid)
                    } else {
                        Log.d(tag, "No such document")
                        tvUserName.text = currentUser.email?.split("@")?.get(0) ?: "User"
                        
                        // Still fetch stats from API
                        fetchUserStats(currentUser.uid)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(tag, "Error getting user data", exception)
                    tvUserName.text = currentUser.email?.split("@")?.get(0) ?: "User"
                    
                    // Still try to fetch stats from API
                    fetchUserStats(currentUser.uid)
                    
                    // Show error message
                    Toast.makeText(this, "Failed to load user profile", Toast.LENGTH_SHORT).show()
                }
        } else {
            // No user signed in - should not happen, but handle it anyway
            Log.e(tag, "No user signed in")
            navigateToLogin()
        }
    }
    
    private fun fetchUserStats(userId: String) {
        lifecycleScope.launch {
            try {
                // Try to fetch user data from API
                val userDataFromApi = ApiClient.getUserData(userId)
                
                // Update our class level user data
                userData = userDataFromApi
                
                // Update UI with fetched data
                updateUIWithUserData(userDataFromApi)
                
                // Hide loading indicator
                showLoading(false)
            } catch (e: Exception) {
                Log.e(tag, "Error fetching user stats", e)
                
                // Use default data in case of error
                val defaultData = UserData(
                    totalPoints = 250,
                    recentPoints = 35,
                    weeklyGoal = 1000,
                    weeklyProgress = 250
                )
                
                // Update UI with default data
                updateUIWithUserData(defaultData)
                
                // Hide loading indicator
                showLoading(false)
                
                // Show error message
                Toast.makeText(
                    this@HomeActivity,
                    "Could not connect to server. Using stored data instead.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun updateUIWithUserData(userData: UserData) {
        // Update UI elements with user data
        tvTotalPoints.text = userData.totalPoints.toString()
        tvPointsIncrement.text = "+${userData.recentPoints}"
        tvWeeklyGoal.text = userData.weeklyGoal.toString()
        tvGoalProgress.text = userData.getProgressText()
    }
    
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBarLoading.visibility = View.VISIBLE
        } else {
            progressBarLoading.visibility = View.GONE
        }
    }
    
    private fun setupListeners() {
        btnViewAllRewards.setOnClickListener {
            // Navigate to rewards activity
            Toast.makeText(this, "View All Rewards clicked", Toast.LENGTH_SHORT).show()
            // TODO: Create and navigate to RewardsActivity
        }

        btnScanQR.setOnClickListener {
            // Navigate to QR scanner
            val intent = Intent(this, QRScannerActivity::class.java)
            startActivity(intent)
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
    
    override fun onResume() {
        super.onResume()
        
        // Refresh data when coming back to this screen
        userId?.let { fetchUserStats(it) }
    }
}