package com.example.trashcashcampus_mobile.fragments

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.trashcashcampus_mobile.LoginActivity
import com.example.trashcashcampus_mobile.MainActivity
import com.example.trashcashcampus_mobile.QRScannerActivity
import com.example.trashcashcampus_mobile.R
import com.example.trashcashcampus_mobile.models.UserData
import com.example.trashcashcampus_mobile.utils.ApiClient
import com.example.trashcashcampus_mobile.utils.LoadingManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardFragment : Fragment() {
    private val tag = "DashboardFragment"
    
    // UI elements
    private lateinit var tvUserName: TextView
    private lateinit var tvTotalPoints: TextView
    private lateinit var tvPointsIncrement: TextView
    private lateinit var tvWeeklyGoal: TextView
    private lateinit var tvGoalProgress: TextView
    private lateinit var btnViewRewards: Button
    private lateinit var progressWeekly: ProgressBar
    private lateinit var progressBarLoading: ProgressBar
    private lateinit var btnLogout: ImageButton
    
    // Firestore
    private val db = Firebase.firestore
    
    // User data
    private var userData: UserData? = null
    private var userId: String? = null
    private var userEmail: String? = null
    
    // Firebase listener
    private var userPointsListener: ListenerRegistration? = null
    
    // Backend retry parameters
    private val maxBackendRetries = 3
    private var backendRetryCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get user ID from SharedPreferences (just like HomeActivity does)
        val prefs = requireActivity().getSharedPreferences("TrashCashPrefs", Context.MODE_PRIVATE)
        userId = prefs.getString("userId", null)
        userEmail = prefs.getString("email", null)
        
        Log.d(tag, "Initial user ID from SharedPreferences: $userId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize UI elements
        initializeUI(view)
        
        // If we don't have a userId from SharedPreferences, try Firebase Auth
        if (userId == null) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            userId = currentUser?.uid
            userEmail = currentUser?.email
            
            // If we got a userId from Firebase Auth, save it to SharedPreferences for next time
            if (userId != null) {
                val prefs = requireActivity().getSharedPreferences("TrashCashPrefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("userId", userId)
                    .putString("email", userEmail)
                    .apply()
            }
        }
        
        if (userId == null) {
            Log.e(tag, "User ID is null - cannot load user data")
            handleAuthError()
            return
        }
        
        Log.d(tag, "Using userId: $userId (email: $userEmail)")
        
        // Just like HomeActivity, directly try Firestore first to ensure we have data
        fixUserPoints(userId!!)
        
        // Load user data
        loadUserData()
        
        // Set up button click listeners
        setupListeners()
        
        // Animate dashboard elements
        animateDashboardEntrance()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        userPointsListener?.remove()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check if we have a valid user ID
        if (userId == null) {
            // Try to re-authenticate
            refreshUserIdFromSources()
        }
        
        // Make sure our real-time listener is active if we have a valid user ID
        if (userId != null && userPointsListener == null) {
            setupUserPointsListener()
        }
        
        // Force refresh data when returning to this fragment
        if (userId != null) {
            // Force a refresh of user data when returning to the dashboard
            Log.d(tag, "Forcing user data refresh on resume")
            forceRefreshUserData()
        }
    }
    
    private fun initializeUI(view: View) {
        // User info
        tvUserName = view.findViewById(R.id.tv_user_name)
        
        // Points
        tvTotalPoints = view.findViewById(R.id.tv_total_points)
        tvPointsIncrement = view.findViewById(R.id.tv_points_increment)
        
        // Weekly goal
        tvWeeklyGoal = view.findViewById(R.id.tv_weekly_goal)
        tvGoalProgress = view.findViewById(R.id.tv_goal_progress)
        progressWeekly = view.findViewById(R.id.progress_weekly)
        
        // Loading indicator
        progressBarLoading = view.findViewById(R.id.progress_weekly) // Temporarily use weekly progress bar
        
        // Buttons
        btnViewRewards = view.findViewById(R.id.btn_view_rewards)
        btnLogout = view.findViewById(R.id.btn_logout)
    }
    
    private fun setupListeners() {
        // Setup rewards button
        btnViewRewards.setOnClickListener {
            // Navigate to rewards tab if using bottom navigation
            val bottomNav = activity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
            bottomNav?.selectedItemId = R.id.navigation_rewards
        }
        
        // Setup logout button
        btnLogout.setOnClickListener {
            logout()
        }
        
        // NOTE: The global QR code scan button has been removed from the Map fragment
        // QR scanning is now only available through location markers on the map
        // This enhances the user experience by tying scanning to specific locations
    }
    
    private fun loadUserData() {
        userId?.let { uid ->
            Log.d(tag, "Loading user data for user ID: $uid")
            
            // First check if Firestore has a valid user document with displayName and totalPoints
            checkFirestoreUser(uid)
            
            // Then try to get data from the backend/API
            lifecycleScope.launch {
                loadUserDataFromBackend(uid)
            }
            
            // Also set up real-time listener for updates
            setupUserPointsListener()
        } ?: run {
            Log.e(tag, "Cannot load user data - userId is null")
            Toast.makeText(requireContext(), "Error: User ID is missing", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun checkFirestoreUser(uid: String) {
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    Log.d(tag, "Firestore document exists for user: $uid")
                    
                    // Get user name from document
                    val userData = document.data
                    
                    // Log all fields for debugging
                    userData?.forEach { (key, value) ->
                        Log.d(tag, "User data field: $key = $value")
                    }
                    
                    // Get user email to check if it's a CIT email
                    val email = userEmail ?: ""
                    
                    // Try to get display name with priority: displayName > fullName > name > email username
                    val displayName = userData?.get("displayName") as? String 
                        ?: userData?.get("fullName") as? String
                        ?: userData?.get("name") as? String
                    
                    // Update UI with the display name
                    updateDisplayName(displayName)
                    
                    if (email.endsWith("@cit.edu")) {
                        // For CIT emails, fetch from separate collection for better name
                        Log.d(tag, "CIT email detected: $email, fetching from students collection")
                        fetchStudentName(email)
                    } else if (displayName == null || displayName.isBlank() || displayName == email.split("@")[0]) {
                        // If no name or just using email username, try to update with a better name
                        updateUserDisplayName(uid)
                    }
                    
                    // Get totalPoints from document
                    var totalPoints = 0
                    val totalPointsValue = userData?.get("totalPoints")
                    if (totalPointsValue != null) {
                        totalPoints = when (totalPointsValue) {
                            is Long -> totalPointsValue.toInt()
                            is Int -> totalPointsValue
                            is Double -> totalPointsValue.toInt()
                            is String -> totalPointsValue.toIntOrNull() ?: 0
                            else -> 0
                        }
                        
                        // Update UI immediately with points
                        tvTotalPoints.text = totalPoints.toString()
                        Log.d(tag, "Set total points to: $totalPoints")
                    }
                } else {
                    Log.d(tag, "No Firestore document exists for user: $uid, creating one")
                    createUserDocument(uid)
                }
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Error checking Firestore document", e)
            }
    }
    
    private fun fetchStudentName(email: String) {
        Log.d(tag, "Fetching student name for email: $email")
        db.collection("students")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Get the first matching document
                    val document = documents.documents[0]
                    
                    // Try to get the fullName field
                    val fullName = document.getString("fullName")
                    
                    if (!fullName.isNullOrEmpty()) {
                        Log.d(tag, "Found student fullName: $fullName")
                        updateDisplayName(fullName)
                        
                        // Also update the user document with this name
                        userId?.let { uid ->
                            val updates = HashMap<String, Any>()
                            updates["displayName"] = fullName
                            
                            db.collection("users").document(uid)
                                .update(updates)
                                .addOnSuccessListener {
                                    Log.d(tag, "Updated user document with fullName: $fullName")
                                }
                                .addOnFailureListener { e ->
                                    Log.e(tag, "Error updating user document with fullName", e)
                                }
                        }
                    } else {
                        // Fall back to email username
                        val username = email.split("@")[0]
                        updateDisplayName(username)
                        Log.d(tag, "No fullName found in students collection, using: $username")
                    }
                } else {
                    // No student document found
                    val username = email.split("@")[0]
                    updateDisplayName(username)
                    Log.d(tag, "No student record found, using email username: $username")
                }
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Error fetching student name", e)
                // Fall back to email username
                val username = email.split("@")[0]
                updateDisplayName(username)
            }
    }
    
    private fun updateUserDisplayName(uid: String) {
        // Get Firebase Auth display name if available
        val authUser = FirebaseAuth.getInstance().currentUser
        val authDisplayName = authUser?.displayName
        
        if (!authDisplayName.isNullOrBlank()) {
            // We have a better name from Firebase Auth, update Firestore
            val updates = HashMap<String, Any>()
            updates["displayName"] = authDisplayName
            
            db.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener {
                    Log.d(tag, "Updated user displayName to: $authDisplayName")
                    tvUserName.text = authDisplayName
                }
                .addOnFailureListener { e ->
                    Log.e(tag, "Error updating user displayName", e)
                }
        }
    }
    
    private fun createUserDocument(uid: String) {
        // Get display name from email or use default
        val displayName = userEmail?.split("@")?.get(0) ?: "User"
        
        // Create document with basic fields
        val userData = HashMap<String, Any>()
        userData["uid"] = uid
        userData["email"] = userEmail ?: ""
        userData["displayName"] = displayName
        userData["totalPoints"] = 0
        userData["createdAt"] = FieldValue.serverTimestamp()
        
        // Save to Firestore
        db.collection("users").document(uid)
            .set(userData)
            .addOnSuccessListener {
                Log.d(tag, "Created new user document with displayName: $displayName")
                tvUserName.text = displayName
                tvTotalPoints.text = "0"
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Error creating user document", e)
            }
    }
    
    private suspend fun loadUserDataFromBackend(uid: String) {
        lifecycleScope.launch {
            try {
                LoadingManager.showLoading(requireActivity())
                Log.d(tag, "Attempting to load user data from backend for user: $uid")
                
                context?.let { ctx ->
                    // First check if we can connect to the backend
                    val isConnected = ApiClient.checkBackendConnection(ctx)
                    
                    if (isConnected) {
                        Log.d(tag, "Backend is reachable, fetching user data")
                        fetchUserStats(uid)
                    } else {
                        Log.w(tag, "Backend is not reachable, falling back to Firestore")
                        fallbackToFirestore(uid)
                    }
                } ?: run {
                    // Context is null, fall back to Firestore
                    Log.w(tag, "Context is null, falling back to Firestore")
                    fallbackToFirestore(uid)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading user data from backend", e)
                
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
                context?.let {
                    LoadingManager.hideLoading(requireActivity())
                }
            }
        }
    }
    
    private fun fallbackToFirestore(uid: String) {
        if (uid.isBlank()) {
            Log.e(tag, "Cannot fallback to Firestore - userId is blank")
            showDefaultUI()
            return
        }
        
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
                    val displayName = userData?.get("displayName") as? String 
                        ?: userData?.get("fullName") as? String
                        ?: userData?.get("name") as? String
                    
                    // Update the display name
                    updateDisplayName(displayName)
                    
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
                    
                    // Always update with the retrieved points (even if 0)
                    tvTotalPoints.text = totalPoints.toString()
                    
                    // Create user data with the retrieved points
                    this.userData = UserData(
                        totalPoints = totalPoints,
                        recentPoints = 0,
                        weeklyGoal = 100,
                        weeklyProgress = totalPoints.coerceAtMost(100) // Use points as progress up to 100
                    )
                    
                    // Update UI
                    updateUIWithUserData(this.userData!!)
                    
                } else {
                    Log.d(tag, "No Firestore document exists for user: $uid")
                    
                    // Use email username instead of default "User"
                    val username = userEmail?.split("@")?.get(0) ?: "User"
                    updateDisplayName(username)
                    
                    // Create a new user with default points
                    createUserWithPoints(uid, username, 0)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(tag, "Error getting user data from Firestore", exception)
                updateDisplayName(userEmail?.split("@")?.get(0))
                
                // Default to 0 points when there's an error
                tvTotalPoints.text = "0"
                
                // Show error message
                Toast.makeText(requireContext(), "Failed to load user profile", Toast.LENGTH_SHORT).show()
            }
    }
    
    // Create a new user document with points
    private fun createUserWithPoints(uid: String, username: String, points: Int) {
        Log.d(tag, "Creating new user document with $points points")
        
        val newUser = HashMap<String, Any>()
        newUser["uid"] = uid
        newUser["email"] = userEmail ?: ""
        newUser["displayName"] = username // Use this for consistent name field
        newUser["totalPoints"] = points
        newUser["createdAt"] = FieldValue.serverTimestamp()
        
        db.collection("users").document(uid)
            .set(newUser)
            .addOnSuccessListener {
                Log.d(tag, "Created new user document with $points points")
                
                // Update the UI
                tvTotalPoints.text = points.toString()
                tvUserName.text = username
                
                Toast.makeText(requireContext(), "Created user profile", Toast.LENGTH_SHORT).show()
                
                // Update our stored user data
                this.userData = UserData(
                    totalPoints = points,
                    recentPoints = 0,
                    weeklyGoal = 100,
                    weeklyProgress = 0 
                )
                
                // Update UI with the new data
                updateUIWithUserData(this.userData!!)
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Error creating user document", e)
                tvTotalPoints.text = "0"
            }
    }
    
    private fun updateUIWithUserData(userData: UserData) {
        // Always display the actual points, even if 0
        tvTotalPoints.text = userData.totalPoints.toString()
        Log.d(tag, "Displaying ${userData.totalPoints} points")
        
        // Show daily points increment with "+" prefix
        val dailyPointsIncrement = "+${userData.recentPoints}"
        tvPointsIncrement.text = dailyPointsIncrement
        
        // Set the text color based on whether there are points or not
        if (userData.recentPoints > 0) {
            tvPointsIncrement.visibility = View.VISIBLE
            tvPointsIncrement.setTextColor(resources.getColor(R.color.success, null))
        } else {
            tvPointsIncrement.text = "+0"
            tvPointsIncrement.visibility = View.INVISIBLE
        }
        
        // Set weekly goals
        tvWeeklyGoal.text = userData.weeklyGoal.toString()
        tvGoalProgress.text = userData.getProgressText()
        
        // Update progress bar
        val progressPercentage = userData.getProgressPercentage()
        progressWeekly.progress = progressPercentage
        
        // Log what we've displayed for debugging
        Log.d(tag, "Updated UI with: points=${userData.totalPoints}, progress=$progressPercentage%")
    }
    
    private fun showDefaultPoints() {
        // Display 0 points instead of "Unavailable"
        tvTotalPoints.text = "0"
        
        // Log what we're doing
        Log.d(tag, "Setting default points to 0")
        
        // Create a default UserData
        userData = UserData(
            totalPoints = 0,
            recentPoints = 0,
            weeklyGoal = 100,
            weeklyProgress = 0
        )
        
        // Update UI with this data
        updateUIWithUserData(userData!!)
        
        // Show error message
        view?.post {
            try {
                if (isAdded && context != null) {
                    Toast.makeText(
                        requireContext(),
                        "No points found. Starting with 0.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error showing toast", e)
            }
        }
    }
    
    private fun showPointsUpdateAnimation(pointsDiff: Int) {
        if (pointsDiff > 0) {
            // Show points increment animation
            tvPointsIncrement.text = "+$pointsDiff"
            tvPointsIncrement.visibility = View.VISIBLE
            tvPointsIncrement.alpha = 1.0f
            
            // Animate the points total
            tvTotalPoints.alpha = 0.3f
            tvTotalPoints.animate()
                .alpha(1.0f)
                .setDuration(500)
                .start()
            
            // Fade out the points increment after a delay
            tvPointsIncrement.animate()
                .alpha(0.0f)
                .setDuration(2000)
                .withEndAction {
                    tvPointsIncrement.visibility = View.INVISIBLE
                }
                .start()
        }
    }
    
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
                                Toast.makeText(requireContext(), "Points updated to 500", Toast.LENGTH_SHORT).show()
                                
                                // Update our stored user data
                                this.userData = UserData(
                                    totalPoints = 500,
                                    recentPoints = 0,
                                    weeklyGoal = 100,
                                    weeklyProgress = 50 // 50% of weekly goal
                                )
                                
                                // Update UI with the new data
                                updateUIWithUserData(this.userData!!)
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
                            Toast.makeText(requireContext(), "Created user profile with 500 points", Toast.LENGTH_SHORT).show()
                            
                            // Update our stored user data
                            this.userData = UserData(
                                totalPoints = 500,
                                recentPoints = 0,
                                weeklyGoal = 100,
                                weeklyProgress = 50 // 50% of weekly goal
                            )
                            
                            // Update UI with the new data
                            updateUIWithUserData(this.userData!!)
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
    
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBarLoading.visibility = View.VISIBLE
        } else {
            progressBarLoading.visibility = View.GONE
        }
    }
    
    private fun setupUserPointsListener() {
        if (userId == null) {
            Log.e(tag, "Cannot setup listener - userId is null")
            handleAuthError()
            return
        }
        
        try {
            Log.d(tag, "Setting up real-time listener for user: $userId")
            
            // Remove any existing listener
            userPointsListener?.remove()
            
            // Set up a listener for the user document
            userPointsListener = db.collection("users").document(userId!!)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(tag, "Error listening for user points changes", error)
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null && snapshot.exists()) {
                        Log.d(tag, "Received real-time update for user document")
                        
                        // Get the total points from the document
                        var totalPoints = 0
                        var weeklyGoal = 100
                        var weeklyProgress = 0
                        
                        // Try to get totalPoints
                        val totalPointsValue = snapshot.get("totalPoints")
                        if (totalPointsValue != null) {
                            totalPoints = when (totalPointsValue) {
                                is Long -> totalPointsValue.toInt()
                                is Int -> totalPointsValue
                                is Double -> totalPointsValue.toInt()
                                is String -> totalPointsValue.toIntOrNull() ?: 0
                                else -> 0
                            }
                            Log.d(tag, "Real-time listener found totalPoints = $totalPoints")
                        }
                        
                        // Try to get weeklyGoal
                        val weeklyGoalValue = snapshot.get("weeklyGoal")
                        if (weeklyGoalValue != null) {
                            weeklyGoal = when (weeklyGoalValue) {
                                is Long -> weeklyGoalValue.toInt()
                                is Int -> weeklyGoalValue
                                is Double -> weeklyGoalValue.toInt()
                                is String -> weeklyGoalValue.toIntOrNull() ?: 100
                                else -> 100
                            }
                        }
                        
                        // Try to get weeklyProgress
                        val weeklyProgressValue = snapshot.get("weeklyProgress")
                        if (weeklyProgressValue != null) {
                            weeklyProgress = when (weeklyProgressValue) {
                                is Long -> weeklyProgressValue.toInt()
                                is Int -> weeklyProgressValue
                                is Double -> weeklyProgressValue.toInt()
                                is String -> weeklyProgressValue.toIntOrNull() ?: 0
                                else -> 0
                            }
                        }
                        
                        // Try to get user name
                        val displayName = snapshot.getString("displayName") 
                            ?: snapshot.getString("fullName") 
                            ?: snapshot.getString("name")
                        
                        // Update user name
                        updateDisplayName(displayName)
                        
                        // Check if this is an actual update (not the initial load)
                        val currentPoints = userData?.totalPoints ?: 0
                        if (currentPoints != 0 && currentPoints != totalPoints) {
                            showPointsUpdateAnimation(totalPoints - currentPoints)
                        }
                        
                        // Always update the points display
                        tvTotalPoints.text = totalPoints.toString()
                        
                        // Update our user data object
                        userData = UserData(
                            totalPoints = totalPoints,
                            recentPoints = if (totalPoints > currentPoints) totalPoints - currentPoints else 0,
                            weeklyGoal = weeklyGoal,
                            weeklyProgress = weeklyProgress
                        )
                        
                        // Update weekly goal UI
                        updateUIWithUserData(userData!!)
                    } else {
                        Log.d(tag, "User document doesn't exist or was deleted")
                        // Show default points (0) when document doesn't exist
                        showDefaultPoints()
                    }
                }
        } catch (e: Exception) {
            Log.e(tag, "Error setting up Firestore listener", e)
            showDefaultPoints()
        }
    }
    
    private fun fetchUserStats(userId: String) {
        Log.d(tag, "Fetching user stats for user: $userId")
        
        if (userId.isBlank()) {
            Log.e(tag, "Cannot fetch user stats - userId is blank")
            showDefaultUI()
            return
        }
        
        lifecycleScope.launch {
            try {
                // Try to fetch user data from API
                context?.let { ctx ->
                    try {
                        val userDataFromApi = ApiClient.getUserData(ctx, userId)
                        
                        // Check if the API returned valid data
                        if (userDataFromApi.totalPoints > 0) {
                            // Update our class level user data
                            userData = userDataFromApi
                            
                            // Log the points received
                            Log.d(tag, "Points retrieved from API: ${userDataFromApi.totalPoints}")
                            
                            // Update UI with fetched data
                            updateUIWithUserData(userDataFromApi)
                            return@let
                        } else {
                            Log.d(tag, "API returned zero points, will check Firestore directly")
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error fetching from API, falling back to Firestore", e)
                    }
                    
                    // Directly try Firestore
                    val firestore = Firebase.firestore
                    firestore.collection("users").document(userId)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                // Log what we found
                                val userDataMap = document.data
                                userDataMap?.forEach { (key, value) ->
                                    Log.d(tag, "Found user data field: $key = $value")
                                }
                                
                                // Get user details
                                val fullName = userDataMap?.get("displayName") as? String 
                                    ?: userDataMap?.get("fullName") as? String
                                    ?: userDataMap?.get("name") as? String
                                    ?: userEmail?.split("@")?.get(0)
                                    ?: "User"
                                    
                                // Update UI with user name
                                activity?.runOnUiThread {
                                    tvUserName.text = fullName
                                    Log.d(tag, "Updated name display to: $fullName")
                                }
                                
                                // Get points
                                val totalPoints = when (val pointsValue = userDataMap?.get("totalPoints")) {
                                    is Long -> pointsValue.toInt()
                                    is Int -> pointsValue
                                    is Double -> pointsValue.toInt()
                                    is String -> pointsValue.toIntOrNull() ?: 0
                                    else -> 0
                                }
                                
                                // Create user data object
                                val retrievedData = UserData(
                                    totalPoints = totalPoints,
                                    recentPoints = 0,
                                    weeklyGoal = 100,
                                    weeklyProgress = totalPoints.coerceAtMost(100)
                                )
                                
                                // Update UI
                                activity?.runOnUiThread {
                                    this@DashboardFragment.userData = retrievedData
                                    updateUIWithUserData(retrievedData)
                                    Log.d(tag, "Successfully fetched user data directly from Firestore")
                                }
                            } else {
                                Log.d(tag, "Firestore document doesn't exist, showing default")
                                activity?.runOnUiThread {
                                    showDefaultPoints()
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(tag, "Error getting data directly from Firestore", e)
                            activity?.runOnUiThread {
                                showDefaultPoints()
                            }
                        }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error in fetchUserStats", e)
                activity?.runOnUiThread {
                    showDefaultPoints()
                }
            }
        }
    }

    // Helper method to refresh user ID from all possible sources
    private fun refreshUserIdFromSources() {
        // Try getting from Firebase Auth first
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            userId = currentUser.uid
            userEmail = currentUser.email
            Log.d(tag, "Refreshed userId from Firebase Auth: $userId")
            return
        }
        
        // Try getting from SharedPreferences
        val prefs = requireActivity().getSharedPreferences("TrashCashPrefs", Context.MODE_PRIVATE)
        userId = prefs.getString("userId", null)
        userEmail = prefs.getString("email", null)
        
        if (userId != null) {
            Log.d(tag, "Refreshed userId from SharedPreferences: $userId")
        } else {
            Log.e(tag, "Failed to refresh userId from any source")
            
            // Show a helpful message with retry option
            Toast.makeText(requireContext(), "Authentication error - please login again", Toast.LENGTH_LONG).show()
            
            // Set some default values for UI
            tvUserName.text = "Guest"
            tvTotalPoints.text = "0"
        }
    }

    // Show default UI when no user data is available
    private fun showDefaultUI() {
        tvUserName.text = "Guest"
        tvTotalPoints.text = "0"
        
        // Create default user data
        userData = UserData(
            totalPoints = 0,
            recentPoints = 0,
            weeklyGoal = 100,
            weeklyProgress = 0
        )
        
        updateUIWithUserData(userData!!)
        
        Toast.makeText(requireContext(), 
            "Not connected to account. Please restart the app.", 
            Toast.LENGTH_LONG).show()
    }

    // Navigate to login when authentication is invalid
    private fun handleAuthError() {
        Toast.makeText(requireContext(), "Authentication error - please log in", Toast.LENGTH_LONG).show()
        
        try {
            // Navigate to login activity
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(tag, "Error navigating to login activity", e)
        }
    }

    private fun logout() {
        try {
            // Clear user session data
            clearUserSession()
            
            // Sign out from Firebase Auth
            FirebaseAuth.getInstance().signOut()
            
            // Show success message
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            
            // Navigate to login screen (MainActivity)
            navigateToLogin()
        } catch (e: Exception) {
            Log.e(tag, "Error during logout", e)
            Toast.makeText(requireContext(), "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearUserSession() {
        // Clear SharedPreferences data
        val prefs = requireActivity().getSharedPreferences("TrashCashPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            remove("userId")
            remove("email")
            remove("token")
            putBoolean("isLoggedIn", false)
            apply()
        }
    }

    private fun navigateToLogin() {
        // Navigate back to MainActivity (login screen)
        val intent = Intent(requireContext(), MainActivity::class.java)
        // Clear back stack so user can't go back after logout
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish() // This is important to finish the HomeActivity
    }

    // Handle result from QR scanner
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == QR_SCANNER_REQUEST_CODE) {
            Log.d(tag, "Returned from QR scanner with result: $resultCode")
            
            if (resultCode == android.app.Activity.RESULT_OK) {
                // QR scan was successful, refresh points
                Log.d(tag, "QR scan successful, refreshing points")
                
                // Force a refresh of user data
                forceRefreshUserData()
            }
        }
    }
    
    private fun forceRefreshUserData() {
        // Show loading indicator
        showLoading(true)
        
        userId?.let { uid ->
            // First try to get fresh data from Firestore
            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Extract points from the document - check both field names for compatibility
                        val userData = document.data
                        
                        // Log all fields for debugging
                        userData?.forEach { (key, value) ->
                            Log.d(tag, "User data field: $key = $value")
                        }
                        
                        // Get points from totalPoints field
                        val totalPointsValue = when (val pointsValue = document.get("totalPoints")) {
                            is Long -> pointsValue.toInt()
                            is Int -> pointsValue
                            is Double -> pointsValue.toInt()
                            is String -> pointsValue.toIntOrNull() ?: 0
                            else -> 0
                        }
                        
                        // Also check the 'points' field
                        val pointsValue = when (val pointsValue = document.get("points")) {
                            is Long -> pointsValue.toInt()
                            is Int -> pointsValue
                            is Double -> pointsValue.toInt()
                            is String -> pointsValue.toIntOrNull() ?: 0
                            else -> 0
                        }
                        
                        // Use the higher value of the two fields
                        val finalPointsValue = Math.max(totalPointsValue, pointsValue)
                        
                        Log.d(tag, "Force refreshed points from Firestore: totalPoints=$totalPointsValue, points=$pointsValue, using max: $finalPointsValue")
                        
                        // Create user data with the retrieved points 
                        val retrievedData = UserData(
                            totalPoints = finalPointsValue,
                            recentPoints = 0,
                            weeklyGoal = 100,
                            weeklyProgress = finalPointsValue.coerceAtMost(100) // Use points as progress up to 100
                        )
                        
                        // Update our stored user data
                        userData = retrievedData
                        
                        // Update UI
                        updateUIWithUserData(retrievedData)
                        
                        // Show points animation if this is an actual update
                        val oldPoints = tvTotalPoints.text.toString().toIntOrNull() ?: 0
                        if (finalPointsValue > oldPoints) {
                            showPointsUpdateAnimation(finalPointsValue - oldPoints)
                        }
                        
                        // Hide loading indicator
                        showLoading(false)
                    } else {
                        // No document exists
                        Log.d(tag, "No user document found during force refresh")
                        showDefaultPoints()
                        showLoading(false)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(tag, "Error force refreshing points from Firestore", e)
                    // Fall back to regular API fetch
                    fetchUserStats(uid)
                    showLoading(false)
                }
        } ?: run {
            // No user ID available
            showLoading(false)
        }
    }

    private fun animateDashboardEntrance() {
        // Get references to view elements
        val appBar = view?.findViewById<View>(R.id.layout_app_bar)
        val userInfoCard = view?.findViewById<View>(R.id.card_user_info)
        val pointsCard = view?.findViewById<View>(R.id.card_points)
        val weeklyGoalCard = view?.findViewById<View>(R.id.card_weekly_goal)
        val rewardsCard = view?.findViewById<View>(R.id.card_rewards)
        
        // Define animation durations and delays
        val baseDelay = 100L
        val animDuration = 500L
        
        // Animate the app bar
        appBar?.apply {
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(animDuration)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .setStartDelay(baseDelay)
                .start()
        }
        
        // Animate the user info card
        userInfoCard?.apply {
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(animDuration)
                .setInterpolator(android.view.animation.OvershootInterpolator(0.8f))
                .setStartDelay(baseDelay + 100)
                .start()
        }
        
        // Animate the points card
        pointsCard?.apply {
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(animDuration)
                .setInterpolator(android.view.animation.OvershootInterpolator(0.8f))
                .setStartDelay(baseDelay + 200)
                .start()
        }
        
        // Animate the weekly goal card
        weeklyGoalCard?.apply {
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(animDuration)
                .setInterpolator(android.view.animation.OvershootInterpolator(0.8f))
                .setStartDelay(baseDelay + 300)
                .start()
        }
        
        // Animate the rewards card
        rewardsCard?.apply {
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(animDuration)
                .setInterpolator(android.view.animation.OvershootInterpolator(0.8f))
                .setStartDelay(baseDelay + 400)
                .withEndAction {
                    // Add a subtle pulse animation to the "View All Rewards" button to draw attention
                    val rewardsButton = view?.findViewById<Button>(R.id.btn_view_rewards)
                    rewardsButton?.apply {
                        val scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.05f, 1f)
                        val scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.05f, 1f)
                        
                        val animSet = AnimatorSet()
                        animSet.playTogether(scaleX, scaleY)
                        animSet.duration = 800
                        animSet.interpolator = AccelerateDecelerateInterpolator()
                        animSet.start()
                    }
                }
                .start()
        }
        
        // Add animation to display point increments when they come in
        observePointChanges()
    }
    
    private fun observePointChanges() {
        // When points change, show the increment with an animation
        var previousPoints = 0
        
        // Update the observer in the existing setupUserPointsListener function
        // We'll track point changes and animate when they increase
        userPointsListener?.remove()
        
        userPointsListener = db.collection("users").document(userId!!)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(tag, "Listen failed.", e)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    // Convert snapshot to HashMap instead of UserData class
                    val userData = snapshot.data
                    val currentPoints = when (val pointsValue = userData?.get("totalPoints")) {
                        is Long -> pointsValue.toInt()
                        is Int -> pointsValue
                        is Double -> pointsValue.toInt()
                        is String -> pointsValue.toIntOrNull() ?: 0
                        else -> 0
                    }
                    
                    // Get the display name
                    val displayName = userData?.get("displayName") as? String ?: "User"
                    
                    // Update UI with user data
                    activity?.runOnUiThread {
                        tvUserName.text = displayName
                        tvTotalPoints.text = currentPoints.toString()
                        
                        // Calculate points increment and animate if positive
                        if (previousPoints > 0 && currentPoints > previousPoints) {
                            val increment = currentPoints - previousPoints
                            animatePointIncrement(increment)
                        }
                        
                        // Update progress bar
                        val weeklyGoal = 100 // Default weekly goal
                        val weeklyProgress = currentPoints.coerceAtMost(weeklyGoal) // Simple progress calculation
                        tvWeeklyGoal.text = weeklyGoal.toString()
                        tvGoalProgress.text = "$weeklyProgress/$weeklyGoal"
                        progressWeekly.max = weeklyGoal
                        progressWeekly.progress = weeklyProgress
                    }
                    
                    // Store points for next comparison
                    previousPoints = currentPoints
                }
            }
    }
    
    private fun animatePointIncrement(increment: Int) {
        // Show the increment text
        tvPointsIncrement.text = "+$increment"
        tvPointsIncrement.visibility = View.VISIBLE
        tvPointsIncrement.alpha = 0f
        tvPointsIncrement.scaleX = 0.5f
        tvPointsIncrement.scaleY = 0.5f
        
        try {
            // Create a color transition animation for points
            val colorFrom = Color.WHITE
            val colorTo = Color.parseColor("#FFEB3B")
            val colorAnim = ObjectAnimator.ofInt(tvTotalPoints, "textColor", colorFrom, colorTo, colorFrom)
            colorAnim.duration = 1000
            colorAnim.setEvaluator(ArgbEvaluator())
            colorAnim.interpolator = AccelerateDecelerateInterpolator()
            colorAnim.start()
            
            // Animate the increment text
            tvPointsIncrement.animate()
                .alpha(1f)
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(300)
                .withEndAction {
                    tvPointsIncrement.animate()
                        .alpha(0f)
                        .translationY(-50f)
                        .setDuration(800)
                        .setStartDelay(500)
                        .withEndAction {
                            tvPointsIncrement.translationY = 0f
                            tvPointsIncrement.visibility = View.INVISIBLE
                        }
                        .start()
                }
                .start()
                
        } catch (e: Exception) {
            Log.e(tag, "Error animating point increment", e)
            // Fallback - just show the increment
            tvPointsIncrement.visibility = View.VISIBLE
        }
    }

    private fun updateDisplayName(displayName: String?) {
        val finalName = displayName?.takeIf { it.isNotBlank() }
            ?: userEmail?.split("@")?.get(0)
            ?: "User"
        
        try {
            tvUserName.text = finalName
            Log.d(tag, "Updated display name: $finalName")
        } catch (e: Exception) {
            Log.e(tag, "Error updating display name", e)
        }
    }

    companion object {
        const val QR_SCANNER_REQUEST_CODE = 100
        
        @JvmStatic
        fun newInstance() = DashboardFragment()
    }
} 