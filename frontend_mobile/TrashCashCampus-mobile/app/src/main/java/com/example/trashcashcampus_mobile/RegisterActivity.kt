package com.example.trashcashcampus_mobile

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
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
import com.example.trashcashcampus_mobile.utils.ApiClient
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RegisterActivity : AppCompatActivity() {
    private val TAG = "RegisterActivity" // For logging

    private var currentStep = 1

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
            // Initialize Firebase Auth is no longer needed
            // auth = FirebaseAuth.getInstance()

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
        btnContinue.setOnClickListener {
            // Save email again in case text watcher missed it
            userEmail = etEmail.text.toString().trim()

            // Validate email
            if (userEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                Toast.makeText(this@RegisterActivity, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if the email ends with @cit.edu
            if (!userEmail.endsWith("@cit.edu")) {
                Toast.makeText(this@RegisterActivity, "Please use your CIT email (@cit.edu)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
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
            Toast.makeText(this, "Checking email...", Toast.LENGTH_SHORT).show()
            
            val db = Firebase.firestore
            db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        // Email not found in Firestore, proceed with registration
                        Log.d(TAG, "Email not found in Firestore, proceeding with registration")
                        currentStep = 2
                        showRegistrationStep(currentStep)
                    } else {
                        // Email already exists in Firestore
                        Log.d(TAG, "Email already registered in our database")
                        Toast.makeText(this, "Email already registered in our database", Toast.LENGTH_SHORT).show()
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
            
            // Instead of creating a Firebase Auth user, use our API
            registerWithAPI(userEmail, userPassword, userName)
            
            // The rest of the logic is now handled inside the registerWithAPI method
            
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

    private fun registerWithAPI(email: String, password: String, username: String) {
        // Show loading state
        findViewById<RelativeLayout>(R.id.progressOverlay)?.visibility = View.VISIBLE

        // Launch a coroutine to make the API call
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.register(this@RegisterActivity, email, password, username)
                
                withContext(Dispatchers.Main) {
                    // Hide loading state
                    findViewById<RelativeLayout>(R.id.progressOverlay)?.visibility = View.GONE
                    
                    if (response != null) {
                        // Registration successful
                        val userId = response["userId"] as? String ?: ""
                        val message = response["message"] as? String ?: "Registration successful"
                        
                        // Navigate to verification waiting activity
                        val intent = Intent(this@RegisterActivity, VerificationWaitingActivity::class.java)
                        intent.putExtra("userId", userId)
                        intent.putExtra("email", email)
                        startActivity(intent)
                        finish()
                    } else {
                        // Registration failed
                        Toast.makeText(
                            this@RegisterActivity,
                            "Registration failed. Please try again.",
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
                        // Proceed to step 3
                        Log.d(TAG, "Moving from step 2 to step 3 (direct implementation)")
                        currentStep = 3
                        showRegistrationStep(currentStep)
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
                    
                    // Proceed to step 4
                    Log.d(TAG, "Moving from step 3 to step 4 (direct implementation)")
                    currentStep = 4
                    showRegistrationStep(currentStep)
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
                    val spinner = findViewById<Spinner>(R.id.spinnerGender)
                    
                    if (spinner != null) {
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
                    } else {
                        Log.e(TAG, "Gender spinner not found in layout")
                    }
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
                        Log.d(TAG, "Moving from step 4 to step 5 (direct implementation)")
                        currentStep = 5
                        showRegistrationStep(currentStep)
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
}