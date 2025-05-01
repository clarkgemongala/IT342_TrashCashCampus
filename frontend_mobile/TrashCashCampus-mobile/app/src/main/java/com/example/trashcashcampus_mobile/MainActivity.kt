package com.example.trashcashcampus_mobile

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.example.trashcashcampus_mobile.fragments.DashboardFragment
import com.example.trashcashcampus_mobile.fragments.MapFragment
import com.example.trashcashcampus_mobile.fragments.RewardsFragment
import com.example.trashcashcampus_mobile.utils.ApiClient
import com.example.trashcashcampus_mobile.utils.LoadingManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    
    // UI elements
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnTogglePasswordVisibility: ImageButton
    private lateinit var btnLogin: AppCompatButton
    private lateinit var tvRegister: TextView
    private lateinit var loginLayout: ConstraintLayout
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var ivLoginIllustration: ImageView
    private lateinit var tvWelcome: TextView
    
    // Firebase Auth
    private lateinit var auth: FirebaseAuth
    
    // Activity Result Launcher for LoginActivity
    private lateinit var loginActivityResultLauncher: ActivityResultLauncher<Intent>
    
    // Current active fragment
    private var activeFragment: Fragment? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        
        // Get authentication status from shared preferences
        val prefs = getSharedPreferences("TrashCashPrefs", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("isLoggedIn", false)
        
        // Decide which layout to use based on authentication state
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified && isLoggedIn) {
            // User is signed in, verified, and has completely logged in - show home with navigation
            setContentView(R.layout.activity_main_with_navigation)
            initializeNavigation()
        } else {
            // Not signed in or not verified, show login form
            setContentView(R.layout.activity_main)
            initializeLoginForm()
        }
    }
    
    private fun initializeNavigation() {
        // Initialize navigation
        fragmentContainer = findViewById(R.id.fragment_container)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        
        // Set up navigation listener
        bottomNavigation.setOnItemSelectedListener { item ->
            handleNavigationItemSelected(item)
        }
        
        // Set initial fragment if needed
        if (activeFragment == null) {
            // Load dashboard fragment by default
            loadFragment(DashboardFragment.newInstance())
            bottomNavigation.selectedItemId = R.id.navigation_dashboard
        }
    }
    
    private fun initializeLoginForm() {
        try {
            // Set up the Activity Result Launcher
            setupActivityResultLauncher()
            
            // Initialize UI elements
            loginLayout = findViewById(R.id.loginLayout)
            etEmail = findViewById(R.id.etEmail)
            etPassword = findViewById(R.id.etPassword)
            btnTogglePasswordVisibility = findViewById(R.id.btnTogglePasswordVisibility)
            btnLogin = findViewById(R.id.btnLogin)
            tvRegister = findViewById(R.id.tvRegister)
            ivLoginIllustration = findViewById(R.id.ivLoginIllustration)
            tvWelcome = findViewById(R.id.tvWelcome)
            
            // Set up listeners
            setupListeners()
            
            // Check backend connectivity
            checkBackendConnection()
            
            // Make sure views are visible but transparent for animation
            ivLoginIllustration.visibility = View.VISIBLE
            tvWelcome.visibility = View.VISIBLE
            loginLayout.visibility = View.VISIBLE
            
            // Animate the login form elements
            animateLoginElements()
        } catch (e: Exception) {
            Log.e(TAG, "Error in initializeLoginForm: ${e.message}", e)
            Toast.makeText(this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun animateLoginElements() {
        // Set initial state - move elements off screen or make them invisible
        ivLoginIllustration.translationY = -100f
        ivLoginIllustration.alpha = 0f
        
        tvWelcome.translationY = 50f
        tvWelcome.alpha = 0f
        
        loginLayout.translationY = 100f
        loginLayout.alpha = 0f
        
        // Animate the illustration first
        val illustrationAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(ivLoginIllustration, "translationY", -100f, 0f),
                ObjectAnimator.ofFloat(ivLoginIllustration, "alpha", 0f, 1f)
            )
            duration = 800
            interpolator = DecelerateInterpolator()
        }
        
        // Animate the welcome text
        val welcomeAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(tvWelcome, "translationY", 50f, 0f),
                ObjectAnimator.ofFloat(tvWelcome, "alpha", 0f, 1f)
            )
            duration = 600
            interpolator = DecelerateInterpolator()
        }
        
        // Animate the login form
        val loginFormAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(loginLayout, "translationY", 100f, 0f),
                ObjectAnimator.ofFloat(loginLayout, "alpha", 0f, 1f)
            )
            duration = 600
            interpolator = DecelerateInterpolator()
        }
        
        // Chain all animations with proper timing
        AnimatorSet().apply {
            play(illustrationAnim)
            play(welcomeAnim).after(200).after(illustrationAnim)
            play(loginFormAnim).after(150).after(welcomeAnim)
            start()
        }
    }
    
    private fun handleNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.navigation_dashboard -> {
                loadFragment(DashboardFragment.newInstance())
                return true
            }
            R.id.navigation_rewards -> {
                loadFragment(RewardsFragment.newInstance())
                return true
            }
            R.id.navigation_map -> {
                loadFragment(MapFragment.newInstance())
                return true
            }
        }
        return false
    }
    
    private fun loadFragment(fragment: Fragment) {
        val previousFragment = activeFragment
        activeFragment = fragment
        
        // Determine the animation direction
        val transitionAnimator = when {
            previousFragment == null -> {
                // First load - fade in
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            }
            previousFragment is DashboardFragment && fragment is RewardsFragment -> {
                // Dashboard to Rewards (move right)
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
            }
            previousFragment is RewardsFragment && fragment is DashboardFragment -> {
                // Rewards to Dashboard (move left)
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
            }
            previousFragment is RewardsFragment && fragment is MapFragment -> {
                // Rewards to Map (move right)
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
            }
            previousFragment is MapFragment && fragment is RewardsFragment -> {
                // Map to Rewards (move left)
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
            }
            previousFragment is DashboardFragment && fragment is MapFragment -> {
                // Dashboard to Map (move right twice)
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
            }
            previousFragment is MapFragment && fragment is DashboardFragment -> {
                // Map to Dashboard (move left twice)
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
            }
            else -> {
                // Default fade animation
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            }
        }
        
        transitionAnimator
            .replace(R.id.fragment_container, fragment)
            .commit()
        
        // Animate the bottom navigation with a small bounce
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val animator = ObjectAnimator.ofFloat(bottomNavigation, "translationY", 20f, 0f)
        animator.duration = 300
        animator.interpolator = OvershootInterpolator(2f)
        animator.start()
    }
    
    private fun checkBackendConnection() {
        // Show backend URL in log for debugging
        Log.d(TAG, "Using backend URL: ${ApiClient.getBackendUrl()}")
        
        // Test connection to backend
        CoroutineScope(Dispatchers.IO).launch {
            val isConnected = ApiClient.checkBackendConnection(this@MainActivity)
            
            withContext(Dispatchers.Main) {
                if (isConnected) {
                    Log.d(TAG, "Backend connection successful")
                } else {
                    Log.e(TAG, "Backend connection failed")
                    Toast.makeText(
                        this@MainActivity,
                        "Warning: Backend server not available. Login and registration will not work.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun setupActivityResultLauncher() {
        loginActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val message = data?.getStringExtra("message") ?: "Unknown error"
                val success = data?.getBooleanExtra("success", false) ?: false
                
                if (success) {
                    // Login was successful
                    val userId = data?.getStringExtra("userId")
                    val email = data?.getStringExtra("email")
                    
                    // Navigate to Dashboard or Home screen with navigation
                    navigateToDashboard(userId, email)
                } else {
                    // Login failed, show error message
                    Log.d(TAG, "Login failed: $message")
                    
                    // Show specific feedback based on the error message
                    if (message == "EMAIL_VERIFICATION_REQUIRED") {
                        Toast.makeText(this, "Please verify your email before logging in.", Toast.LENGTH_LONG).show()
                    } else if (message.contains("password")) {
                        // Password-related errors
                        Toast.makeText(this, "Invalid password. Please try again.", Toast.LENGTH_LONG).show()
                        etPassword.requestFocus()
                        etPassword.error = "Invalid password"
                    } else if (message.contains("account") || message.contains("user")) {
                        // Account/user-related errors
                        Toast.makeText(this, "Account not found. Please check your email.", Toast.LENGTH_LONG).show()
                        etEmail.requestFocus()
                        etEmail.error = "Account not found"
                    } else {
                        // General error
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                    
                    showLoginForm()
                }
            } else {
                // User canceled or something went wrong
                Toast.makeText(this, "Login canceled or failed", Toast.LENGTH_SHORT).show()
                showLoginForm()
            }
        }
    }
    
    private fun setupListeners() {
        // Set up password visibility toggle
        btnTogglePasswordVisibility.setOnClickListener {
            togglePasswordVisibility()
        }
        
        // Set up login button with animation
        btnLogin.setOnClickListener {
            // Add a small scale animation when button is clicked
            val scaleDown = ObjectAnimator.ofFloat(btnLogin, "scaleX", 1f, 0.95f).apply {
                duration = 100
            }
            val scaleUp = ObjectAnimator.ofFloat(btnLogin, "scaleX", 0.95f, 1f).apply {
                duration = 100
            }
            
            AnimatorSet().apply {
                playSequentially(scaleDown, scaleUp)
                start()
            }
            
            // Proceed with login attempt
            attemptLogin()
        }
        
        // Set up register text view
        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            // Add transition animation
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
        
        // Add text watchers for error clearing
        etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                etEmail.error = null
            }
        })
        
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                etPassword.error = null
            }
        })
    }
    
    private fun showLoginForm() {
        // Hide the loading overlay
        LoadingManager.hideLoading(this)
        
        // Show login form and enable button
        loginLayout.visibility = View.VISIBLE
        btnLogin.isEnabled = true
        loginLayout.alpha = 1.0f
    }
    
    private fun togglePasswordVisibility() {
        if (etPassword.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            // Show password
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            btnTogglePasswordVisibility.setImageResource(R.drawable.ic_visibility)
        } else {
            // Hide password
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            btnTogglePasswordVisibility.setImageResource(R.drawable.ic_visibility_off)
        }
        // Maintain cursor position
        etPassword.setSelection(etPassword.text.length)
    }
    
    private fun attemptLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        
        // Validate inputs
        if (email.isEmpty()) {
            etEmail.error = "Please enter your email"
            etEmail.requestFocus()
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Please enter a valid email"
            etEmail.requestFocus()
            return
        }
        
        if (password.isEmpty()) {
            etPassword.error = "Please enter your password"
            etPassword.requestFocus()
            return
        }
        
        // First check if backend is available
        CoroutineScope(Dispatchers.IO).launch {
            // Show loading state
            withContext(Dispatchers.Main) {
                showLoadingState()
            }
            
            // Check if backend is responding
            val isConnected = ApiClient.checkBackendConnection(this@MainActivity)
            
            if (!isConnected) {
                withContext(Dispatchers.Main) {
                    // Hide loading overlay
                    LoadingManager.hideLoading(this@MainActivity)
                    
                    // Show error
                    Toast.makeText(
                        this@MainActivity,
                        "Cannot connect to the server. Make sure your backend is running.",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Re-enable login button
                    btnLogin.isEnabled = true
                    loginLayout.alpha = 1.0f
                }
                return@launch
            }
            
            // Backend is available, proceed with login activity
            withContext(Dispatchers.Main) {
                // Launch LoginActivity to verify credentials
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                intent.putExtra("email", email)
                intent.putExtra("password", password)
                loginActivityResultLauncher.launch(intent)
            }
        }
    }
    
    private fun showLoadingState() {
        LoadingManager.showLoading(this)
        btnLogin.isEnabled = false
        loginLayout.alpha = 0.5f
    }
    
    private fun navigateToDashboard(userId: String?, email: String?) {
        // Save user info to shared preferences for later use
        val prefs = getSharedPreferences("TrashCashPrefs", MODE_PRIVATE)
        with(prefs.edit()) {
            putString("userId", userId)
            putString("email", email)
            putBoolean("isLoggedIn", true)
            apply()
        }
        
        // Switch to the navigation layout
        setContentView(R.layout.activity_main_with_navigation)
        initializeNavigation()
    }
}