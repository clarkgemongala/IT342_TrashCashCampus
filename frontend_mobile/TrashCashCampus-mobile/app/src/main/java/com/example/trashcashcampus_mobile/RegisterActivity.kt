package com.example.trashcashcampus_mobile

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityOptionsCompat
import com.example.trashcashcampus_mobile.utils.ApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RegisterActivity : AppCompatActivity() {
    private val TAG = "RegisterActivity" // For logging

    private var currentStep = 1
    private lateinit var auth: FirebaseAuth  // Add Firebase Auth instance

    // Store all user inputs for state preservation
    private var userEmail = ""
    private var userName = ""
    private var userBirthday = ""
    private var selectedGender = ""
    private var userPassword = ""
    private var userConfirmPassword = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // Initialize Firebase Auth
            auth = FirebaseAuth.getInstance()

            // Restore state if available
            savedInstanceState?.let {
                currentStep = it.getInt("currentStep", 1)
                userEmail = it.getString("userEmail", "")
                userName = it.getString("userName", "")
                userBirthday = it.getString("userBirthday", "")
                selectedGender = it.getString("selectedGender", "")
                userPassword = it.getString("userPassword", "")
                userConfirmPassword = it.getString("userConfirmPassword", "")
            }

            showRegistrationStep(currentStep)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show()
            finish() // Return to previous activity if there's a fatal error
        }
    }

    // Save state when activity might be destroyed
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentStep", currentStep)
        outState.putString("userEmail", userEmail)
        outState.putString("userName", userName)
        outState.putString("userBirthday", userBirthday)
        outState.putString("selectedGender", selectedGender)
        outState.putString("userPassword", userPassword)
        outState.putString("userConfirmPassword", userConfirmPassword)
    }

    private fun showRegistrationStep(step: Int) {
        try {
            Log.d(TAG, "Showing registration step $step")
            
            // Special handling for steps 2, 3, 4 and 5
            if (step == 2) {
                handleStep2Directly()
                return
            } else if (step == 3) {
                handleStep3Directly()
                return
            } else if (step == 4) {
                handleStep4Directly()
                return
            } else if (step == 5) {
                handleStep5Directly()
                return
            }
            
            // First set the content view based on the step
            when (step) {
                1 -> setContentView(R.layout.activity_register1)
                2 -> setContentView(R.layout.activity_register2) // This will be handled separately
                3 -> setContentView(R.layout.activity_register3) // This will be handled separately
                4 -> setContentView(R.layout.activity_register4) // This will be handled separately
                5 -> setContentView(R.layout.activity_register5) // This will be handled separately
                else -> {
                    Log.e(TAG, "Invalid step number: $step")
                    setContentView(R.layout.activity_register1)
                    currentStep = 1
                }
            }
            
            // Use post to ensure view hierarchy is completely built
            findViewById<View>(android.R.id.content).post {
                try {
                    // Now set up the UI for the current step
                    setupStepContent(step)
                    
                    // Apply enter animations for the current step's main content
                    applyEnterAnimation()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in post-layout setup for step $step: ${e.message}", e)
                    Toast.makeText(this, "Error setting up the form. Please restart the app.", Toast.LENGTH_SHORT).show()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing registration step $step: ${e.message}", e)
            Toast.makeText(this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show()
            
            // If there's an error, try falling back to step 1
            if (step != 1) {
                Log.w(TAG, "Falling back to step 1 due to error")
                currentStep = 1
                setContentView(R.layout.activity_register1)
                findViewById<View>(android.R.id.content).post {
                    setupStepContent(1)
                }
            }
        }
    }
    
    private fun setupStepContent(step: Int) {
        // Skip steps 2, 3, 4 and 5 as they're handled specially
        if (step == 2 || step == 3 || step == 4 || step == 5) return
        
        when (step) {
            1 -> setupStep1Content()
        }
    }
    
    private fun setupStep1Content() {
        // Find the "Log in" text and set click listener to go back to login
        val tvLogIn = findViewById<TextView>(R.id.tvLogIn)
        if (tvLogIn == null) {
            Log.e(TAG, "tvLogIn is null")
            return
        }
        tvLogIn.setOnClickListener {
            // Go back to login screen
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        // Find email input - safely
        val etEmail = findViewById<EditText>(R.id.etEmail)
        if (etEmail == null) {
            Log.e(TAG, "etEmail is null")
            return
        }

        // Restore email if it exists
        if (userEmail.isNotEmpty()) {
            etEmail.setText(userEmail)
        }

        // Apply a subtle animation to the email field when focused
        etEmail.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                val scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1f, 1.05f, 1f)
                val scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1f, 1.05f, 1f)
                
                AnimatorSet().apply {
                    playTogether(scaleX, scaleY)
                    duration = 300
                    start()
                }
            }
        }

        // Save email as user types
        etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                userEmail = s.toString().trim()
            }
        })

        // Set continue button to move to next step
        val btnContinue = findViewById<AppCompatButton>(R.id.btnContinue)
        if (btnContinue == null) {
            Log.e(TAG, "btnContinue is null")
            return
        }
        
        // Add a press animation for the button
        btnContinue.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }
            }
            false
        }
        
        btnContinue.setOnClickListener {
            // Save email again in case text watcher missed it
            userEmail = etEmail.text.toString().trim()

            // Validate email
            if (userEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                showErrorAnimation(etEmail)
                Toast.makeText(this@RegisterActivity, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if the email ends with @cit.edu
            if (!userEmail.endsWith("@cit.edu")) {
                showErrorAnimation(etEmail)
                Toast.makeText(this@RegisterActivity, "Please use your CIT email (@cit.edu)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                // Show loading animation
                showLoadingAnimation()
                
                // BYPASS FIREBASE AUTH CHECK due to Google API security exception
                Log.w(TAG, "Bypassing Firebase Auth check due to potential security exception")
                
                // Directly proceed to step 2 or check Firestore
                checkEmailInFirestore(userEmail)
                
                /*
                // Original implementation - commented out
                // SOLUTION 1: First check if email exists in Firebase Auth
                auth.fetchSignInMethodsForEmail(userEmail)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            val signInMethods = authTask.result?.signInMethods
                            if (signInMethods.isNullOrEmpty()) {
                                // Email not registered with Firebase Auth, now check Firestore
                                checkEmailInFirestore(userEmail)
                            } else {
                                // Email already registered with Firebase Auth
                                Toast.makeText(this@RegisterActivity,
                                    "Email already registered with Firebase. Please use a different email or try again later.",
                                    Toast.LENGTH_LONG).show()
                            }
                        } else {
                            // Error checking email with Firebase Auth
                            Log.e(TAG, "Error checking email with Firebase Auth: ${authTask.exception?.message}", authTask.exception)
                            
                            // If there's an error, still proceed to next step
                            Log.w(TAG, "Proceeding to step 2 despite Firebase Auth error")
                            currentStep = 2
                            showRegistrationStep(currentStep)
                        }
                    }
                */
            } catch (e: Exception) {
                Log.e(TAG, "Error checking email: ${e.message}", e)
                
                // If there's an exception, still proceed to next step
                Log.w(TAG, "Proceeding to step 2 despite exception")
                currentStep = 2
                showRegistrationStep(currentStep)
            }
        }
    }

    private fun setupStep4Content() {
        // Set back button to return to previous step
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        if (btnBack == null) {
            Log.e(TAG, "btnBack is null in step 4")
            return
        }
        btnBack.setOnClickListener {
            Log.d(TAG, "Moving back from step 4 to step 3")
            currentStep = 3
            showRegistrationStep(currentStep)
        }

        // Find the "Log in" text and set click listener to go back to login
        val tvLogIn = findViewById<TextView>(R.id.tvLogIn)
        if (tvLogIn == null) {
            Log.e(TAG, "tvLogIn is null in step 4")
            return
        }
        tvLogIn.setOnClickListener {
            // Go back to login screen
            finish()
        }

        // Setup gender spinner with options - safely
        try {
            setupGenderSpinner()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up gender spinner: ${e.message}", e)
            Toast.makeText(this@RegisterActivity, "Error loading gender options", Toast.LENGTH_SHORT).show()
        }

        // Set continue button for next step
        val btnContinue = findViewById<AppCompatButton>(R.id.btnContinue)
        if (btnContinue == null) {
            Log.e(TAG, "btnContinue is null in step 4")
            return
        }
        btnContinue.setOnClickListener {
            // Validate gender selection
            if (selectedGender.isEmpty() || selectedGender == "Select your gender") {
                Toast.makeText(this@RegisterActivity, "Please select your gender", Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "Moving from step 4 to step 5")
                currentStep = 5
                showRegistrationStep(currentStep)
            }
        }
    }
    
    private fun setupStep5Content() {
        // Set back button to return to previous step
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        if (btnBack == null) {
            Log.e(TAG, "btnBack is null in step 5")
            return
        }
        btnBack.setOnClickListener {
            Log.d(TAG, "Moving back from step 5 to step 4")
            currentStep = 4
            showRegistrationStep(currentStep)
        }

        // Find the "Log in" text and set click listener to go back to login
        val tvLogIn = findViewById<TextView>(R.id.tvLogIn)
        if (tvLogIn == null) {
            Log.e(TAG, "tvLogIn is null in step 5")
            return
        }
        tvLogIn.setOnClickListener {
            // Go back to login screen
            finish()
        }

        // Find password fields
        val etPassword = findViewById<EditText>(R.id.etPassword)
        if (etPassword == null) {
            Log.e(TAG, "etPassword is null")
            return
        }
        
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        if (etConfirmPassword == null) {
            Log.e(TAG, "etConfirmPassword is null")
            return
        }

        val btnTogglePasswordVisibility = findViewById<ImageButton>(R.id.btnTogglePasswordVisibility)
        if (btnTogglePasswordVisibility == null) {
            Log.e(TAG, "btnTogglePasswordVisibility is null")
            return
        }
        
        val btnToggleConfirmPasswordVisibility = findViewById<ImageButton>(R.id.btnToggleConfirmPasswordVisibility)
        if (btnToggleConfirmPasswordVisibility == null) {
            Log.e(TAG, "btnToggleConfirmPasswordVisibility is null")
            return
        }

        // Set up visibility toggle for password field
        btnTogglePasswordVisibility.setOnClickListener {
            togglePasswordVisibility(etPassword, btnTogglePasswordVisibility)
        }

        // Set up visibility toggle for confirm password field
        btnToggleConfirmPasswordVisibility.setOnClickListener {
            togglePasswordVisibility(etConfirmPassword, btnToggleConfirmPasswordVisibility)
        }

        // Restore passwords if they exist
        if (userPassword.isNotEmpty()) {
            etPassword.setText(userPassword)
        }
        if (userConfirmPassword.isNotEmpty()) {
            etConfirmPassword.setText(userConfirmPassword)
        }

        // Save passwords as user types
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                userPassword = s.toString().trim()
            }
        })

        etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                userConfirmPassword = s.toString().trim()
            }
        })

        // Set finish button to complete registration
        val btnFinish = findViewById<AppCompatButton>(R.id.btnFinish)
        if (btnFinish == null) {
            Log.e(TAG, "btnFinish is null")
            return
        }
        btnFinish.setOnClickListener {
            // Save passwords again in case text watchers missed them
            userPassword = etPassword.text.toString().trim()
            userConfirmPassword = etConfirmPassword.text.toString().trim()

            val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$"
            if (!userPassword.matches(passwordPattern.toRegex())) {
                Toast.makeText(this@RegisterActivity, "Password must be at least 8 characters with numbers, upper and lower case letters, and special characters", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (userPassword.isEmpty()) {
                Toast.makeText(this@RegisterActivity, "Please enter a password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userPassword != userConfirmPassword) {
                Toast.makeText(this@RegisterActivity, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d(TAG, "Finishing registration process")
            // Create the user account with Firebase Auth
            createAccount()
        }
    }

    private fun togglePasswordVisibility(editText: EditText, toggleButton: ImageButton) {
        if (editText.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            // Show password
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            toggleButton.setImageResource(R.drawable.ic_visibility)
        } else {
            // Hide password
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleButton.setImageResource(R.drawable.ic_visibility_off)
        }
        // Maintain cursor position
        editText.setSelection(editText.text.length)
    }

    // New method to check email in Firestore
    private fun checkEmailInFirestore(email: String) {
        try {
            // Show loading indicator
            Toast.makeText(this, "Checking email availability...", Toast.LENGTH_SHORT).show()
            
            val db = Firebase.firestore
            db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        // Email not found in Firestore, proceed with registration
                        Log.d(TAG, "Email not found in Firestore, proceeding with registration")
                        
                        // Animate the progress indicator before moving to step 2
                        animateProgressIndicator(1, 2)
                        
                        // Add a short delay to let the animation play before moving to next step
                        Handler(Looper.getMainLooper()).postDelayed({
                            // Proceed to step 2
                            Log.d(TAG, "Moving from step 1 to step 2 (direct implementation)")
                            currentStep = 2
                            showRegistrationStep(currentStep)
                        }, 300)
                    } else {
                        // Email already exists in Firestore
                        Log.d(TAG, "Email already registered in our database")
                        Toast.makeText(this, "This email is already registered. Please use a different email or log in.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error checking email in Firestore: ${e.message}", e)
                    Toast.makeText(this, "Network error. Please try again.", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in checkEmailInFirestore: ${e.message}", e)
            Toast.makeText(this, "Error checking email. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    // Add this method to show the DatePickerDialog
    private fun showDatePickerDialog() {
        try {
            val calendar = Calendar.getInstance()

            // Get current date values as default
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            // Create DatePickerDialog
            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    // Format the date as DD/MM/YYYY
                    val formattedDate = String.format(
                        Locale.getDefault(),
                        "%02d/%02d/%04d",
                        selectedDay,
                        selectedMonth + 1,
                        selectedYear
                    )

                    // Update the TextView with selected date
                    val tvSelectedDate = findViewById<TextView>(R.id.tvSelectedDate)
                    tvSelectedDate?.text = formattedDate

                    // Save the selected date
                    userBirthday = formattedDate
                },
                year, month, day
            )

            // Set maximum date to today (so users can't select future dates)
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

            // Set a reasonable minimum date (e.g., 100 years ago)
            val minCalendar = Calendar.getInstance()
            minCalendar.add(Calendar.YEAR, -100)
            datePickerDialog.datePicker.minDate = minCalendar.timeInMillis

            // Show the dialog
            datePickerDialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing date picker: ${e.message}", e)
            Toast.makeText(this, "Could not open date picker", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createAccount() {
        try {
            Log.d(TAG, "Creating account for $userEmail")
            
            // Get a reference to the progress overlay
            val progressOverlay = findViewById<RelativeLayout>(R.id.progressOverlay)
            
            // Show progress overlay
            progressOverlay?.visibility = View.VISIBLE
            
            // Use Firebase Authentication directly in addition to our API
            registerWithFirebaseAndAPI(userEmail, userPassword, userName)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating account: ${e.message}", e)
            
            // If there's an exception, hide progress and show error
            findViewById<RelativeLayout>(R.id.progressOverlay)?.visibility = View.GONE
            
            Toast.makeText(
                this@RegisterActivity,
                "Error creating account: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun registerWithFirebaseAndAPI(email: String, password: String, username: String) {
        // Show loading state
        findViewById<RelativeLayout>(R.id.progressOverlay)?.visibility = View.VISIBLE

        // Launch a coroutine to register with Firebase first, then with our API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // First, create the user in Firebase Authentication
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                
                if (firebaseUser != null) {
                    // Send verification email through Firebase
                    try {
                        // Firebase Auth's built-in verification email
                        firebaseUser.sendEmailVerification().await()
                        Log.d(TAG, "Firebase Auth verification email sent successfully")
                    } catch (e: Exception) {
                        // If built-in verification fails, the user is still created
                        // Our Firebase function will take care of sending the verification email
                        Log.w(TAG, "Firebase Auth verification email failed, relying on cloud function", e)
                    }
                    
                    // Now register with our API to create the Firestore record
                    val response = ApiClient.register(this@RegisterActivity, email, password, username)
                    
                    if (response == null) {
                        // Backend API failed, create Firestore record directly as fallback
                        Log.w(TAG, "Backend API registration failed, using Firestore directly as fallback")
                        createFirestoreUserDirectly(firebaseUser.uid, email, password, username)
                    }
                    
                    withContext(Dispatchers.Main) {
                        // Hide loading state
                        findViewById<RelativeLayout>(R.id.progressOverlay)?.visibility = View.GONE
                        
                        if (response != null) {
                            // Registration successful
                            val userId = response["userId"] as? String ?: firebaseUser.uid
                            val message = response["message"] as? String ?: "Registration successful"
                            
                            // Navigate to verification waiting activity
                            val intent = Intent(this@RegisterActivity, VerificationWaitingActivity::class.java)
                            intent.putExtra("userId", userId)
                            intent.putExtra("email", email)
                            startActivity(intent)
                            finish()
                        } else {
                            // Backend registration failed, but Firebase auth succeeded
                            // Still redirect to verification screen
                            val intent = Intent(this@RegisterActivity, VerificationWaitingActivity::class.java)
                            intent.putExtra("userId", firebaseUser.uid)
                            intent.putExtra("email", email)
                            startActivity(intent)
                            finish()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        // Hide loading state
                        findViewById<RelativeLayout>(R.id.progressOverlay)?.visibility = View.GONE
                        
                        Toast.makeText(
                            this@RegisterActivity,
                            "Failed to create account with Firebase Authentication",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Hide loading state
                    findViewById<RelativeLayout>(R.id.progressOverlay)?.visibility = View.GONE
                    
                    Log.e(TAG, "Error during registration: ${e.message}", e)
                    Toast.makeText(
                        this@RegisterActivity,
                        "Registration failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Fallback method to create user in Firestore directly if backend API fails
    private fun createFirestoreUserDirectly(userId: String, email: String, password: String, username: String) {
        try {
            val db = Firebase.firestore
            val userRef = db.collection("users").document(userId)
            
            // Create user profile with exactly the same fields as the backend would
            val userData = hashMapOf(
                "email" to email,
                "displayName" to username,
                "password" to password,  // Note: In production, you should not store plain text passwords
                "isEmailVerified" to false,
                "totalPoints" to 0,
                "points" to 0,
                "recycledMetal" to 0,
                "recycledPlastic" to 0,
                "totalRecycled" to 0,
                "role" to "student",
                "photoURL" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "lastUpdated" to FieldValue.serverTimestamp(),
                "uid" to userId // Adding uid field for consistency
            )
            
            // Set the document with the user data
            userRef.set(userData)
                .addOnSuccessListener { 
                    Log.d(TAG, "Firestore user document created successfully with ID: $userId") 
                }
                .addOnFailureListener { e -> 
                    Log.e(TAG, "Error creating Firestore user document: ${e.message}", e) 
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating Firestore user document: ${e.message}", e)
        }
    }

    private fun setupGenderSpinner() {
        val spinner = findViewById<Spinner>(R.id.spinnerGender) ?:
        throw NullPointerException("Gender spinner not found in layout")

        // Create gender options
        val genderOptions = arrayOf("Select your gender", "Male", "Female", "Prefer Not to Say")

        // Create adapter with custom layouts
        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            genderOptions
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)

        // Apply adapter to spinner
        spinner.adapter = adapter

        // Set selection based on stored value
        if (selectedGender.isNotEmpty()) {
            val position = genderOptions.indexOf(selectedGender)
            if (position > 0) {
                spinner.setSelection(position)
            }
        } else {
            // Default selection hint
            spinner.setSelection(0)
        }

        // Set listener for selection
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    selectedGender = genderOptions[position]
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    // A direct and simplified approach to handling step 2
    private fun handleStep2Directly() {
        // Set the content view
        setContentView(R.layout.activity_register2)
        
        Log.d(TAG, "Direct handling of step 2")
        
        // Wait for a moment to ensure view inflation is complete
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                // Find and set up the back button
                val btnBack = findViewById<ImageButton>(R.id.btnBack)
                btnBack?.setOnClickListener {
                    currentStep = 1
                    showRegistrationStep(currentStep)
                }
                
                // Find and set up the full name input
                val etFullName = findViewById<EditText>(R.id.etFullName)
                if (etFullName != null) {
                    // Restore name if it exists
                    if (userName.isNotEmpty()) {
                        etFullName.setText(userName)
                    }
                    
                    // Save name as user types
                    etFullName.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        override fun afterTextChanged(s: Editable?) {
                            userName = s.toString().trim()
                        }
                    })
                }
                
                // Find and set up the login text view
                val tvLogIn = findViewById<TextView>(R.id.tvLogIn)
                tvLogIn?.setOnClickListener {
                    finish()
                }
                
                // Find and set up the continue button
                val btnContinue = findViewById<AppCompatButton>(R.id.btnContinue)
                btnContinue?.setOnClickListener {
                    // Get the full name from the input field
                    val currentName = etFullName?.text?.toString()?.trim() ?: ""
                    
                    // Update saved name
                    userName = currentName
                    
                    if (userName.isEmpty()) {
                        Toast.makeText(this@RegisterActivity, "Please enter your name", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    
                    // Check if the name is already taken in Firestore
                    checkNameInFirestore(userName)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in direct handling of step 2: ${e.message}", e)
                Toast.makeText(this, "Error setting up the form. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }, 300)  // Longer delay to ensure layout is fully inflated
    }

    // Method to check if the name is already taken in Firestore
    private fun checkNameInFirestore(name: String) {
        try {
            // Show loading indicator
            Toast.makeText(this, "Checking name availability...", Toast.LENGTH_SHORT).show()
            
            val db = Firebase.firestore
            db.collection("users")
                .whereEqualTo("fullName", name)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        // Name not found in Firestore, proceed with registration
                        Log.d(TAG, "Name not found in Firestore, proceeding with registration")
                        
                        // Animate the progress indicator before moving to step 3
                        animateProgressIndicator(2, 3)
                        
                        // Add a short delay to let the animation play before moving to next step
                        Handler(Looper.getMainLooper()).postDelayed({
                            // Proceed to step 3
                            Log.d(TAG, "Moving from step 2 to step 3 (direct implementation)")
                            currentStep = 3
                            showRegistrationStep(currentStep)
                        }, 300)
                    } else {
                        // Name already exists in Firestore
                        Log.d(TAG, "Name already registered in our database")
                        Toast.makeText(this, "This name is already registered. Please use a different name.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error checking name in Firestore: ${e.message}", e)
                    Toast.makeText(this, "Network error. Please try again.", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in checkNameInFirestore: ${e.message}", e)
            Toast.makeText(this, "Error checking name. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    // Direct implementation for Step 3
    private fun handleStep3Directly() {
        // Set the content view
        setContentView(R.layout.activity_register3)
        
        Log.d(TAG, "Direct handling of step 3")
        
        // Wait for a moment to ensure view inflation is complete
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                // Find and set up the back button
                val btnBack = findViewById<ImageButton>(R.id.btnBack)
                btnBack?.setOnClickListener {
                    currentStep = 2
                    showRegistrationStep(currentStep)
                }
                
                // Find the "Log in" text and set click listener to go back to login
                val tvLogIn = findViewById<TextView>(R.id.tvLogIn)
                tvLogIn?.setOnClickListener {
                    // Go back to login screen
                    finish()
                }
                
                // Find the TextView that will display the selected date
                val tvSelectedDate = findViewById<TextView>(R.id.tvSelectedDate)
                
                // Restore birthday if it exists
                if (userBirthday.isNotEmpty() && tvSelectedDate != null) {
                    tvSelectedDate.text = userBirthday
                }
                
                // Find the date picker container
                val datePickerContainer = findViewById<RelativeLayout>(R.id.datePickerContainer)
                
                // Set up click listener for the date field to open date picker
                datePickerContainer?.setOnClickListener {
                    showDatePickerDialog()
                }
                
                // Also set the calendar icon to open the date picker
                val ivCalendar = findViewById<ImageView>(R.id.ivCalendar)
                ivCalendar?.setOnClickListener {
                    showDatePickerDialog()
                }
                
                // Set continue button for next step
                val btnContinue = findViewById<AppCompatButton>(R.id.btnContinue)
                btnContinue?.setOnClickListener {
                    // Get the date from the TextView
                    val currentDate = tvSelectedDate?.text?.toString()?.trim() ?: ""
                    userBirthday = currentDate
                    
                    // Validate date
                    if (userBirthday.isEmpty() || userBirthday == "DD/MM/YYYY") {
                        Toast.makeText(this@RegisterActivity, "Please select your birthday", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    
                    // Animate the progress indicator before moving to step 4
                    animateProgressIndicator(3, 4)
                    
                    // Add a short delay to let the animation play before moving to next step
                    Handler(Looper.getMainLooper()).postDelayed({
                        // Proceed to step 4
                        Log.d(TAG, "Moving from step 3 to step 4 (direct implementation)")
                        currentStep = 4
                        showRegistrationStep(currentStep)
                    }, 300)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in direct handling of step 3: ${e.message}", e)
                Toast.makeText(this, "Error setting up the date selection form. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }, 300)  // Longer delay to ensure layout is fully inflated
    }

    // Direct implementation for Step 4
    private fun handleStep4Directly() {
        // Set the content view
        setContentView(R.layout.activity_register4)
        
        Log.d(TAG, "Direct handling of step 4")
        
        // Wait for a moment to ensure view inflation is complete
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                // Find and set up the back button
                val btnBack = findViewById<ImageButton>(R.id.btnBack)
                btnBack?.setOnClickListener {
                    currentStep = 3
                    showRegistrationStep(currentStep)
                }
                
                // Find the "Log in" text and set click listener to go back to login
                val tvLogIn = findViewById<TextView>(R.id.tvLogIn)
                tvLogIn?.setOnClickListener {
                    // Go back to login screen
                    finish()
                }
                
                // Setup gender spinner with options
                try {
                    setupGenderSpinner()
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting up gender spinner: ${e.message}", e)
                    Toast.makeText(this@RegisterActivity, "Error loading gender options", Toast.LENGTH_SHORT).show()
                }
                
                // Set continue button for next step
                val btnContinue = findViewById<AppCompatButton>(R.id.btnContinue)
                btnContinue?.setOnClickListener {
                    // Validate gender selection
                    if (selectedGender.isEmpty() || selectedGender == "Select your gender") {
                        Toast.makeText(this@RegisterActivity, "Please select your gender", Toast.LENGTH_SHORT).show()
                    } else {
                        // Animate the progress indicator before moving to step 5
                        animateProgressIndicator(4, 5)
                        
                        // Add a short delay to let the animation play before moving to next step
                        Handler(Looper.getMainLooper()).postDelayed({
                            Log.d(TAG, "Moving from step 4 to step 5")
                            currentStep = 5
                            showRegistrationStep(currentStep)
                        }, 300)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in direct handling of step 4: ${e.message}", e)
                Toast.makeText(this, "Error setting up the gender selection form. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }, 300)  // Longer delay to ensure layout is fully inflated
    }
    
    // Direct implementation for Step 5
    private fun handleStep5Directly() {
        // Set the content view
        setContentView(R.layout.activity_register5)
        
        Log.d(TAG, "Direct handling of step 5")
        
        // Wait for a moment to ensure view inflation is complete
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                // Find and set up the back button
                val btnBack = findViewById<ImageButton>(R.id.btnBack)
                btnBack?.setOnClickListener {
                    currentStep = 4
                    showRegistrationStep(currentStep)
                }
                
                // Find the "Log in" text and set click listener to go back to login
                val tvLogIn = findViewById<TextView>(R.id.tvLogIn)
                tvLogIn?.setOnClickListener {
                    // Go back to login screen
                    finish()
                }
                
                // Find password fields
                val etPassword = findViewById<EditText>(R.id.etPassword)
                val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
                val btnTogglePasswordVisibility = findViewById<ImageButton>(R.id.btnTogglePasswordVisibility)
                val btnToggleConfirmPasswordVisibility = findViewById<ImageButton>(R.id.btnToggleConfirmPasswordVisibility)
                
                // Set up visibility toggle for password field
                btnTogglePasswordVisibility?.setOnClickListener {
                    togglePasswordVisibility(etPassword, btnTogglePasswordVisibility)
                }
                
                // Set up visibility toggle for confirm password field
                btnToggleConfirmPasswordVisibility?.setOnClickListener {
                    togglePasswordVisibility(etConfirmPassword, btnToggleConfirmPasswordVisibility)
                }
                
                // Restore passwords if they exist
                if (userPassword.isNotEmpty() && etPassword != null) {
                    etPassword.setText(userPassword)
                }
                if (userConfirmPassword.isNotEmpty() && etConfirmPassword != null) {
                    etConfirmPassword.setText(userConfirmPassword)
                }
                
                // Save passwords as user types
                etPassword?.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        userPassword = s.toString().trim()
                    }
                })
                
                etConfirmPassword?.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        userConfirmPassword = s.toString().trim()
                    }
                })
                
                // Set finish button to complete registration
                val btnFinish = findViewById<AppCompatButton>(R.id.btnFinish)
                btnFinish?.setOnClickListener {
                    // Save passwords again in case text watchers missed them
                    userPassword = etPassword?.text?.toString()?.trim() ?: ""
                    userConfirmPassword = etConfirmPassword?.text?.toString()?.trim() ?: ""
                    
                    val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$"
                    if (!userPassword.matches(passwordPattern.toRegex())) {
                        Toast.makeText(this@RegisterActivity, "Password must be at least 8 characters with numbers, upper and lower case letters, and special characters", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                    
                    if (userPassword.isEmpty()) {
                        Toast.makeText(this@RegisterActivity, "Please enter a password", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    
                    if (userPassword != userConfirmPassword) {
                        Toast.makeText(this@RegisterActivity, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    
                    Log.d(TAG, "Finishing registration process")
                    // Create the user account with Firebase Auth
                    createAccount()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in direct handling of step 5: ${e.message}", e)
                Toast.makeText(this, "Error setting up the password form. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }, 300)  // Longer delay to ensure layout is fully inflated
    }

    // Helper method to offer email change
    private fun offerEmailChange() {
        // This could be implemented as a dialog or by returning to step 1
        // For simplicity, we'll just return to step 1
        currentStep = 1
        showRegistrationStep(currentStep)
    }

    // New method to apply enter animations
    private fun applyEnterAnimation() {
        try {
            // Find main container views to animate
            val mainContent = findViewById<View>(android.R.id.content)?.findViewById<View>(R.id.mainContent)
            val btnContinue = findViewById<AppCompatButton>(R.id.btnContinue) ?: findViewById<AppCompatButton>(R.id.btnFinish)
            
            // Animate main content with fade in and slight slide up
            mainContent?.let {
                it.alpha = 0f
                it.translationY = 50f
                it.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .start()
            }
            
            // Animate button with bounce effect
            btnContinue?.let {
                val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.bounce)
                it.startAnimation(bounceAnim)
            }
            
            // Animate fields one by one with a delay
            animateInputFields()
        } catch (e: Exception) {
            Log.e(TAG, "Error applying animations: ${e.message}")
            // Continue without animations if there's an error
        }
    }
    
    // Animate input fields with a staggered effect
    private fun animateInputFields() {
        try {
            val delay = 150L
            
            // Get all EditText fields
            val rootView = findViewById<View>(android.R.id.content)
            val editTexts = findAllEditTextsInView(rootView)
            
            // Animate each field with a delay
            editTexts.forEachIndexed { index, editText ->
                editText.alpha = 0f
                editText.translationX = 50f
                
                editText.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setStartDelay(delay * index)
                    .setDuration(400)
                    .start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error animating input fields: ${e.message}")
        }
    }
    
    // Helper method to find all EditText views in a view hierarchy
    private fun findAllEditTextsInView(view: View?): List<EditText> {
        val editTexts = mutableListOf<EditText>()
        
        if (view == null) return editTexts
        
        if (view is EditText) {
            editTexts.add(view)
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                editTexts.addAll(findAllEditTextsInView(view.getChildAt(i)))
            }
        }
        
        return editTexts
    }
    
    // Updated method to navigate to next step with animation
    private fun navigateToNextStep(nextStep: Int) {
        try {
            // Apply exit animation before changing the view
            val mainContent = findViewById<View>(android.R.id.content)?.findViewById<View>(R.id.mainContent)
            
            // If we can find the main content, animate it out
            if (mainContent != null) {
                mainContent.animate()
                    .alpha(0f)
                    .translationX(-100f)
                    .setDuration(300)
                    .withEndAction {
                        // Update step and show new view after animation
                        currentStep = nextStep
                        showRegistrationStep(currentStep)
                        
                        // Apply custom transition animation
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    }
                    .start()
            } else {
                // If we can't find the main content, just navigate without animation
                currentStep = nextStep
                showRegistrationStep(currentStep)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to next step: ${e.message}")
            // Fall back to non-animated transition
            currentStep = nextStep
            showRegistrationStep(currentStep)
        }
    }
    
    // Updated method to navigate to previous step with animation
    private fun navigateToPreviousStep(previousStep: Int) {
        try {
            // Apply exit animation before changing the view
            val mainContent = findViewById<View>(android.R.id.content)?.findViewById<View>(R.id.mainContent)
            
            // If we can find the main content, animate it out
            if (mainContent != null) {
                mainContent.animate()
                    .alpha(0f)
                    .translationX(100f)
                    .setDuration(300)
                    .withEndAction {
                        // Update step and show new view after animation
                        currentStep = previousStep
                        showRegistrationStep(currentStep)
                        
                        // Apply custom transition animation
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    }
                    .start()
            } else {
                // If we can't find the main content, just navigate without animation
                currentStep = previousStep
                showRegistrationStep(currentStep)
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to previous step: ${e.message}")
            // Fall back to non-animated transition
            currentStep = previousStep
            showRegistrationStep(currentStep)
        }
    }

    // New method to show loading animation
    private fun showLoadingAnimation() {
        try {
            val loadingOverlay = findViewById<View>(R.id.loadingOverlay) ?: return
            
            // Fade in the loading overlay
            loadingOverlay.visibility = View.VISIBLE
            loadingOverlay.alpha = 0f
            loadingOverlay.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
                
            // Hide loading overlay after a delay (in case the network call takes too long)
            Handler(Looper.getMainLooper()).postDelayed({
                loadingOverlay.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        loadingOverlay.visibility = View.GONE
                    }
                    .start()
            }, 3000)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing loading animation: ${e.message}")
        }
    }
    
    // New method to show error animation on input fields
    private fun showErrorAnimation(view: View) {
        try {
            // Store the original padding values
            val originalPaddingLeft = view.paddingLeft
            val originalPaddingTop = view.paddingTop
            val originalPaddingRight = view.paddingRight
            val originalPaddingBottom = view.paddingBottom
            
            // Create shake animation
            val animator = ObjectAnimator.ofFloat(view, "translationX", 0f, -15f, 15f, -15f, 15f, -15f, 15f, -15f, 15f, 0f)
            animator.duration = 500
            animator.start()
            
            // Highlight the field with red briefly
            view.setBackgroundResource(R.drawable.bg_input_field_error)
            
            // Apply the original padding to maintain dimensions
            view.setPadding(originalPaddingLeft, originalPaddingTop, originalPaddingRight, originalPaddingBottom)
            
            // Reset to normal style after a delay
            Handler(Looper.getMainLooper()).postDelayed({
                view.setBackgroundResource(R.drawable.bg_input_field_animated)
                // Maintain the original padding
                view.setPadding(originalPaddingLeft, originalPaddingTop, originalPaddingRight, originalPaddingBottom)
            }, 1500)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing error animation: ${e.message}")
        }
    }

    // Add this new method for animating progress indicators
    private fun animateProgressIndicator(completedStep: Int, nextStep: Int) {
        try {
            Log.d(TAG, "Animating progress indicators from step $completedStep to $nextStep")
            
            // Get the ImageView for the completed step that will change to a checkmark
            val completedCircleId = when (completedStep) {
                1 -> R.id.vCircleEmail
                2 -> R.id.vCircleName
                3 -> R.id.vCircleBirthday
                4 -> R.id.vCircleGender
                else -> null
            }
            
            // Find the view for the completed step
            val completedCircleView = if (completedCircleId != null) findViewById<View>(completedCircleId) else null
            
            // Only proceed if we found the view
            if (completedCircleView != null) {
                // Change the background to the selected state (orange circle)
                completedCircleView.setBackgroundResource(R.drawable.circle_selected)
                
                // If it's a View (not already an ImageView with a checkmark)
                if (completedCircleView is View && completedCircleView !is ImageView) {
                    // Create a new ImageView with the checkmark to replace it
                    val parentLayout = completedCircleView.parent as? ViewGroup
                    val indexInParent = parentLayout?.indexOfChild(completedCircleView) ?: -1
                    
                    if (parentLayout != null && indexInParent >= 0) {
                        // Create the checkmark ImageView with the same dimensions as the original view
                        val params = completedCircleView.layoutParams
                        
                        // Create the checkmark ImageView
                        val checkmarkView = ImageView(this).apply {
                            id = completedCircleView.id
                            layoutParams = params
                            setBackgroundResource(R.drawable.circle_selected)
                            setImageResource(R.drawable.ic_check)
                            // Use a fixed padding value in pixels instead of a dimension resource
                            setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
                            scaleX = 0f
                            scaleY = 0f
                            alpha = 0f
                        }
                        
                        // Remove the old view and add the new one
                        parentLayout.removeView(completedCircleView)
                        parentLayout.addView(checkmarkView, indexInParent)
                        
                        // Load and start the pop-in animation
                        val animation = AnimationUtils.loadAnimation(this, R.anim.circle_pop_in)
                        checkmarkView.startAnimation(animation)
                    }
                } else if (completedCircleView is ImageView) {
                    // If it's already an ImageView (in case of going back and forth between steps)
                    // Just play the animation on the existing view
                    val animation = AnimationUtils.loadAnimation(this, R.anim.circle_pop_in)
                    completedCircleView.startAnimation(animation)
                }
                
                // Update text color for completed step
                val stepTextId = when (completedStep) {
                    1 -> R.id.tvEmailStep
                    2 -> R.id.tvNameStep
                    3 -> R.id.tvBirthdayStep
                    4 -> R.id.tvGenderStep
                    else -> null
                }
                
                findViewById<TextView>(stepTextId ?: 0)?.setTextColor(getColor(R.color.orange_progress))
                
                // Update the next step's text to active state
                val nextStepTextId = when (nextStep) {
                    2 -> R.id.tvNameStep
                    3 -> R.id.tvBirthdayStep
                    4 -> R.id.tvGenderStep
                    else -> null
                }
                
                // Only try to update the text color if we can find the view
                val nextStepTextView = if (nextStepTextId != null) findViewById<TextView>(nextStepTextId) else null
                nextStepTextView?.setTextColor(getColor(R.color.dark_text))
                
                // Animate the connecting line between completed and next step
                if (completedStep < 4) {
                    // Find the connector line view - both the background and progress lines
                    val connectorBgId = R.id.vConnectorLineBackground
                    val connectorProgressId = when (completedStep) {
                        1 -> R.id.vConnectorLineProgress  // Default ID expected in layouts
                        2 -> R.id.vConnectorLineProgress2 // May need to add these IDs to layouts
                        3 -> R.id.vConnectorLineProgress3
                        else -> null
                    }
                    
                    // If progress line not found, try a different approach
                    val connectorLine = findViewById<View>(connectorProgressId ?: 0)
                    if (connectorLine != null) {
                        // Use the animation we created
                        val animation = AnimationUtils.loadAnimation(this, R.anim.line_expand)
                        connectorLine.startAnimation(animation)
                    } else {
                        // If we couldn't find a specific progress line, try to find the background line
                        // and dynamically create a progress line over it
                        val bgLine = findViewById<View>(connectorBgId)
                        if (bgLine != null && bgLine.parent is ViewGroup) {
                            // Create a new orange progress line with same dimensions as background
                            val parentLayout = bgLine.parent as ViewGroup
                            val indexInParent = parentLayout.indexOfChild(bgLine)
                            
                            val progressLine = View(this).apply {
                                id = View.generateViewId()  // Generate a new ID
                                // Create new layout params instead of cloning
                                val originalParams = bgLine.layoutParams
                                layoutParams = ViewGroup.LayoutParams(originalParams.width, originalParams.height)
                                setBackgroundColor(getColor(R.color.orange_progress))
                                // Start with 0 width for animation
                                scaleX = 0f
                                pivotX = 0f  // Important: set pivot to start of line for proper scale animation
                            }
                            
                            // Add the progress line right after the background line
                            parentLayout.addView(progressLine, indexInParent + 1)
                            
                            // Use animation
                            val animation = AnimationUtils.loadAnimation(this, R.anim.line_expand)
                            progressLine.startAnimation(animation)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error animating progress indicators: ${e.message}", e)
            // Continue without animation if there's an error
        }
    }
    
    // Helper extension function to convert DP to pixels
    private fun Int.dpToPx(): Int {
        val scale = resources.displayMetrics.density
        return (this * scale + 0.5f).toInt()
    }
}