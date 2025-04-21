package com.example.trashcashcampus_mobile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    
    // UI elements
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnTogglePasswordVisibility: ImageButton
    private lateinit var btnLogin: AppCompatButton
    private lateinit var tvRegister: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var loginLayout: ConstraintLayout
    
    // Firebase Auth
    private lateinit var auth: FirebaseAuth
    
    // Activity Result Launcher for LoginActivity
    private lateinit var loginActivityResultLauncher: ActivityResultLauncher<Intent>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        try {
            // Initialize Firebase Auth
            auth = FirebaseAuth.getInstance()
            
            // Set up the Activity Result Launcher
            setupActivityResultLauncher()
            
            // Initialize UI elements
            initializeUI()
            
            // Set up listeners
            setupListeners()
            
            // Check if user is already logged in
            checkCurrentUser()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show()
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
                    
                    // Navigate to Dashboard or Home screen
                    navigateToDashboard(userId, email)
                } else {
                    // Login failed, show error message
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    showLoginForm()
                }
            } else {
                // User canceled or something went wrong
                Toast.makeText(this, "Login canceled or failed", Toast.LENGTH_SHORT).show()
                showLoginForm()
            }
        }
    }
    
    private fun initializeUI() {
        loginLayout = findViewById(R.id.loginLayout)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnTogglePasswordVisibility = findViewById(R.id.btnTogglePasswordVisibility)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        progressBar = findViewById(R.id.progressBar)
    }
    
    private fun setupListeners() {
        // Set up password visibility toggle
        btnTogglePasswordVisibility.setOnClickListener {
            togglePasswordVisibility()
        }
        
        // Set up login button
        btnLogin.setOnClickListener {
            attemptLogin()
        }
        
        // Set up register text view
        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
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
    
    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            // User is already logged in and verified
            navigateToDashboard(currentUser.uid, currentUser.email)
        } else {
            // No user logged in or not verified, show login form
            showLoginForm()
        }
    }
    
    private fun showLoginForm() {
        progressBar.visibility = View.GONE
        loginLayout.visibility = View.VISIBLE
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
        
        // Show loading state
        showLoadingState()
        
        // Launch LoginActivity to verify credentials
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("email", email)
        intent.putExtra("password", password)
        loginActivityResultLauncher.launch(intent)
    }
    
    private fun showLoadingState() {
        btnLogin.isEnabled = false
        progressBar.visibility = View.VISIBLE
        loginLayout.alpha = 0.5f
    }
    
    private fun navigateToDashboard(userId: String?, email: String?) {
        // Store current user info if needed
        val sharedPref = getSharedPreferences("TrashCashPrefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("userId", userId)
            putString("email", email)
            apply()
        }
        
        // Navigate to HomeActivity
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Close the login activity
    }
}