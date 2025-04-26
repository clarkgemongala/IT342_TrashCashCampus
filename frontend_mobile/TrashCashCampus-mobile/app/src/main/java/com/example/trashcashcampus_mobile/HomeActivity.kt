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
import com.example.trashcashcampus_mobile.utils.ApiClient as ApiClientUtil
import com.example.trashcashcampus_mobile.utils.LoadingManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.HashMap

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
    
    // Firestore (still needed for user data)
    private val db = Firebase.firestore
    
    // User data
    private var userData: UserData? = null
    private var userId: String? = null
    private var userEmail: String? = null
    
    // Retry variables
    private var backendRetryCount = 0
    private val maxBackendRetries = 2
    
    // Firebase listener
    private var userPointsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)
        
        // Initialize UI elements
        initializeUI()
        
        // Get user ID from shared preferences (saved during login)
        val prefs = getSharedPreferences("TrashCashPrefs", MODE_PRIVATE)
        userId = prefs.getString("userId", null)
        userEmail = prefs.getString("email", null)
        
        if (userId.isNullOrEmpty()) {
            Log.e(tag, "No user signed in (userId is null)")
            navigateToLogin()
            return
        }
        
        Log.d(tag, "User ID from SharedPrefs: $userId")
        Log.d(tag, "User Email from SharedPrefs: $userEmail")
        
        // Try direct Firestore solution to ensure points are set
        fixUserPoints(userId!!)
        
        // Load user data
        loadUserData()
        
        // Set up real-time listener for user points
        setupUserPointsListener()
        
        // Set up button click listeners
        setupListeners()
    }
    
    override fun onDestroy() {
        // Remove the Firebase listener when activity is destroyed
        userPointsListener?.remove()
        super.onDestroy()
    }
    
    private fun setupUserPointsListener() {
        userId?.let { uid ->
            Log.d(tag, "Setting up real-time listener for user points changes")
            
            // Remove any existing listener
            userPointsListener?.remove()
            
            // Set up a listener for the user document
            userPointsListener = db.collection("users").document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(tag, "Error listening for user points changes", error)
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null && snapshot.exists()) {
                        Log.d(tag, "Received real-time update for user document")
                        
                        // Get the total points from the document
                        var totalPoints = 0
                        
                        // Try multiple ways to get totalPoints to handle different types
                        val totalPointsValue = snapshot.get("totalPoints")
                        if (totalPointsValue != null) {
                            totalPoints = when (totalPointsValue) {
                                is Long -> totalPointsValue.toInt()
                                is Int -> totalPointsValue
                                is Double -> totalPointsValue.toInt()
                                is String -> totalPointsValue.toIntOrNull() ?: 0
                                else -> 0
                            }
                            
                            Log.d(tag, "Real-time update: totalPoints = $totalPoints")
                            
                            // Check if this is an actual update (not the initial load)
                            val currentPoints = userData?.totalPoints ?: 0
                            if (currentPoints != totalPoints) {
                                // Points have changed
                                val pointsDiff = totalPoints - currentPoints
                                if (pointsDiff > 0) {
                                    // Points increased
                                    Toast.makeText(
                                        this@HomeActivity,
                                        "Points updated: +$pointsDiff points",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (pointsDiff < 0) {
                                    // Points decreased
                                    Toast.makeText(
                                        this@HomeActivity,
                                        "Points updated: $pointsDiff points",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            
                            // Update the UI with the new points
                            tvTotalPoints.text = totalPoints.toString()
                            
                            // Update our user data object with new points
                            userData = userData?.copy(totalPoints = totalPoints)?.also {
                                // When points update, also update the weekly progress display
                                tvWeeklyGoal.text = it.weeklyGoal.toString()
                                tvGoalProgress.text = it.getProgressText()
                                
                                // Show points change animation
                                showPointsUpdateAnimation()
                            } ?: UserData(
                                totalPoints = totalPoints,
                                recentPoints = 0,
                                weeklyGoal = 100,
                                weeklyProgress = 0
                            ).also {
                                // Set default values for new users
                                tvWeeklyGoal.text = it.weeklyGoal.toString()
                                tvGoalProgress.text = it.getProgressText()
                            }
                        }
                    } else {
                        Log.d(tag, "User document doesn't exist or was deleted")
                    }
                }
        }
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
        val uid = userId
        if (uid != null) {
            // Try to get user data from backend first
            loadUserDataFromBackend(uid)
        } else {
            // No user ID available - should not happen, but handle it anyway
            Log.e(tag, "No user ID available")
            navigateToLogin()
        }
    }
    
    private fun loadUserDataFromBackend(uid: String) {
        LoadingManager.showLoading(this, "Loading profile...")
        
        lifecycleScope.launch {
            try {
                // Get the user token from shared preferences
                val prefs = getSharedPreferences("TrashCashPrefs", MODE_PRIVATE)
                val token = prefs.getString("token", null)
                
                // Try to get user profile from backend
                if (token != null) {
                    val profileResponse = ApiClientUtil.getProfile(this@HomeActivity, uid, token)
                    
                    if (profileResponse != null) {
                        // Update UI with user's name from backend
                        tvUserName.text = profileResponse.name ?: userEmail?.split("@")?.get(0) ?: "User"
                        
                        // Now fetch stats through backend
                        fetchUserStats(uid)
                        
                        // Reset retry counter on success
                        backendRetryCount = 0
                    } else {
                        // No profile data, fall back to using email
                        tvUserName.text = userEmail?.split("@")?.get(0) ?: "User"
                        fetchUserStats(uid)
                    }
                } else {
                    // No token, fall back to using email
                    tvUserName.text = userEmail?.split("@")?.get(0) ?: "User"
                    fetchUserStats(uid)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading user profile from backend", e)
                
                // Increment retry counter
                backendRetryCount++
                
                if (backendRetryCount <= maxBackendRetries) {
                    // Retry after a short delay
                    Log.d(tag, "Retrying backend connection (${backendRetryCount}/${maxBackendRetries})")
                    delay(1000) // Wait 1 second before retry
                    loadUserDataFromBackend(uid)
                } else {
                    // Fall back to Firestore after max retries
                    Log.w(tag, "Max retries reached, falling back to Firestore")
                    fallbackToFirestore(uid)
                }
            } finally {
                LoadingManager.hideLoading(this@HomeActivity)
            }
        }
    }
    
    private fun fallbackToFirestore(uid: String) {
        Log.d(tag, "Using Firestore fallback for user: $uid")
        
        // Fall back to getting user details from Firestore
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    Log.d(tag, "Firestore document exists for user: $uid")
                    
                    // Dump all fields for debugging
                    val userData = document.data
                    userData?.forEach { (key, value) ->
                        Log.d(tag, "Firestore field: $key = $value (${value?.javaClass?.name})")
                    }
                    
                    // Update UI with user's name
                    val fullName = userData?.get("displayName") as? String 
                        ?: userData?.get("fullName") as? String
                        ?: userEmail?.split("@")?.get(0)
                        ?: "User"
                    
                    tvUserName.text = fullName
                    
                    // Try multiple ways to get totalPoints
                    var totalPoints = 0
                    
                    // First try: get data from map
                    val totalPointsValue = userData?.get("totalPoints")
                    if (totalPointsValue != null) {
                        Log.d(tag, "Found totalPoints in userData map: $totalPointsValue (${totalPointsValue.javaClass.name})")
                        totalPoints = when (totalPointsValue) {
                            is Long -> totalPointsValue.toInt()
                            is Int -> totalPointsValue
                            is Double -> totalPointsValue.toInt()
                            is String -> totalPointsValue.toIntOrNull() ?: 0
                            else -> 0
                        }
                    } else {
                        // Second try: getLong
                        val longValue = document.getLong("totalPoints")
                        if (longValue != null) {
                            Log.d(tag, "Found totalPoints via getLong(): $longValue")
                            totalPoints = longValue.toInt()
                        } else {
                            // Third try: getDouble (just in case)
                            val doubleValue = document.getDouble("totalPoints")
                            if (doubleValue != null) {
                                Log.d(tag, "Found totalPoints via getDouble(): $doubleValue")
                                totalPoints = doubleValue.toInt()
                            } else {
                                Log.w(tag, "totalPoints field not found in any expected location")
                            }
                        }
                    }
                    
                    Log.d(tag, "Final resolved totalPoints from Firestore: $totalPoints")
                    
                    // Update the total points display immediately
                    tvTotalPoints.text = totalPoints.toString()
                    
                    // Show a toast explaining we're using local data
                    Toast.makeText(
                        this@HomeActivity,
                        "Using local data (backend unavailable)",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Log.d(tag, "No Firestore document exists for user: $uid")
                    tvUserName.text = userEmail?.split("@")?.get(0) ?: "User"
                    
                    // Set default points value if document doesn't exist
                    tvTotalPoints.text = "0"
                    
                    Log.d(tag, "Document not found, defaulting to 0 points")
                }
                
                // Still fetch stats but now it will use Firebase fallback
                fetchUserStats(uid)
            }
            .addOnFailureListener { exception ->
                Log.e(tag, "Error getting user data from Firestore", exception)
                tvUserName.text = userEmail?.split("@")?.get(0) ?: "User"
                
                // Still try to fetch stats
                fetchUserStats(uid)
                
                // Show error message
                Toast.makeText(this, "Failed to load user profile, using default points", Toast.LENGTH_SHORT).show()
                tvTotalPoints.text = "0"
            }
    }
    
    private fun fetchUserStats(userId: String) {
        Log.d(tag, "Fetching user stats for user: $userId")
        
        lifecycleScope.launch {
            try {
                // Try to fetch user data from API
                val userDataFromApi = ApiClient.getUserData(userId)
                
                // Update our class level user data
                userData = userDataFromApi
                
                // Log the points received
                Log.d(tag, "Points retrieved from API/Firebase: ${userDataFromApi.totalPoints}")
                
                // Update UI with fetched data
                updateUIWithUserData(userDataFromApi)
                
                // Hide loading indicator
                showLoading(false)
            } catch (e: Exception) {
                Log.e(tag, "Error fetching user stats", e)
                
                // Use default data with 0 points in case of error
                val defaultData = UserData(
                    totalPoints = 0,
                    recentPoints = 0,
                    weeklyGoal = 100,
                    weeklyProgress = 0
                )
                
                // Update UI with default data
                updateUIWithUserData(defaultData)
                
                // Hide loading indicator
                showLoading(false)
                
                // Show error message
                Toast.makeText(
                    this@HomeActivity,
                    "Could not connect to server. Using default points.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun updateUIWithUserData(userData: UserData) {
        // Update UI elements with user data
        tvTotalPoints.text = userData.totalPoints.toString()
        
        // Show daily points increment with "+" prefix
        val dailyPointsIncrement = "+${userData.recentPoints}"
        tvPointsIncrement.text = dailyPointsIncrement
        
        // Set the text color based on whether there are points or not
        if (userData.recentPoints > 0) {
            tvPointsIncrement.setTextColor(resources.getColor(R.color.success, null))
        } else {
            tvPointsIncrement.text = "+0"
            tvPointsIncrement.setTextColor(resources.getColor(R.color.text_secondary, null))
        }
        
        // Set weekly goals
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
            remove("token")
            apply()
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Refresh data when coming back to this screen
        userId?.let { fetchUserStats(it) }
    }

    /**
     * This method attempts to fix user points by directly ensuring the totalPoints field exists and is set
     */
    private fun fixUserPoints(uid: String) {
        Log.d(tag, "Attempting to fix user points for $uid")
        
        // Check if the user exists first
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    Log.d(tag, "User document exists, dumping all fields:")
                    val userData = document.data
                    userData?.forEach { (key, value) ->
                        Log.d(tag, "Field: $key = $value (${value?.javaClass?.name})")
                    }
                    
                    // Check if totalPoints exists
                    val hasPoints = userData?.containsKey("totalPoints") == true
                    val currentPoints = when (val pointsValue = userData?.get("totalPoints")) {
                        is Long -> pointsValue.toInt()
                        is Int -> pointsValue
                        is Double -> pointsValue.toInt()
                        is String -> pointsValue.toIntOrNull() ?: 0
                        else -> null
                    }
                    
                    if (!hasPoints || currentPoints == null) {
                        Log.d(tag, "totalPoints field missing or invalid, setting to 500")
                        
                        // Set the points to 500
                        val updates = HashMap<String, Any>()
                        updates["totalPoints"] = 500
                        
                        db.collection("users").document(uid)
                            .set(updates, SetOptions.merge())
                            .addOnSuccessListener {
                                Log.d(tag, "Successfully set totalPoints to 500")
                                
                                // Update the UI
                                tvTotalPoints.text = "500"
                                Toast.makeText(this, "Points updated to 500", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Log.e(tag, "Error updating points", e)
                            }
                    } else {
                        Log.d(tag, "User has valid totalPoints: $currentPoints")
                        
                        // Use the existing points value
                        tvTotalPoints.text = currentPoints.toString()
                    }
                } else {
                    Log.d(tag, "User document doesn't exist, creating it with points")
                    
                    // Create a new user document with points
                    val newUser = HashMap<String, Any>()
                    newUser["uid"] = uid
                    newUser["email"] = userEmail ?: ""
                    newUser["totalPoints"] = 500
                    newUser["createdAt"] = FieldValue.serverTimestamp()
                    
                    db.collection("users").document(uid)
                        .set(newUser)
                        .addOnSuccessListener {
                            Log.d(tag, "Created new user document with 500 points")
                            
                            // Update the UI
                            tvTotalPoints.text = "500"
                            Toast.makeText(this, "Created user profile with 500 points", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e(tag, "Error creating user document", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Error checking user document", e)
            }
    }

    private fun showPointsUpdateAnimation() {
        // Simple animation to highlight points update
        tvTotalPoints.alpha = 0.3f
        tvTotalPoints.animate()
            .alpha(1.0f)
            .setDuration(500)
            .start()
    }
}