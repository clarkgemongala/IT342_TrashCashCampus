package com.example.trashcashcampus_mobile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {
    private val TAG = "RegisterActivity" // For logging

    private var currentStep = 1
    private var selectedGender = ""

    // User data variables
    private var userEmail = ""
    private var userName = ""
    private var userBirthday = ""
    private var userPassword = ""

    // List to store verification code EditText fields
    private lateinit var codeDigits: List<EditText>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // Initialize Firebase - make sure it's already initialized in your Application class
            // but we'll safely access it here
            showRegistrationStep(currentStep)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show()
            finish() // Return to previous activity if there's a fatal error
        }
    }

    private fun showRegistrationStep(step: Int) {
        try {
            when (step) {
                1 -> {
                    setContentView(R.layout.activity_register1)

                    // Find the "Log in" text and set click listener to go back to login
                    val tvLogIn = findViewById<TextView>(R.id.tvLogIn) ?: return
                    tvLogIn.setOnClickListener {
                        // Go back to login screen
                        finish()
                    }

                    // Find email input - safely
                    val etEmail = findViewById<EditText>(R.id.etEmail) ?: return

                    // Set continue button to move to next step
                    val btnContinue = findViewById<AppCompatButton>(R.id.btnContinue) ?: return
                    btnContinue.setOnClickListener {
                        // Validate email
                        val email = etEmail.text.toString().trim()
                        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        try {
                            // Check if email already exists in Firestore
                            val db = Firebase.firestore
                            db.collection("users")
                                .whereEqualTo("email", email)
                                .get()
                                .addOnSuccessListener { documents ->
                                    if (documents.isEmpty) {
                                        // Email not found, proceed with registration
                                        userEmail = email
                                        currentStep = 2
                                        showRegistrationStep(currentStep)
                                    } else {
                                        // Email already exists
                                        Toast.makeText(this, "Email already registered", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error checking email: ${e.message}", e)
                                    Toast.makeText(this, "Network error. Please try again.", Toast.LENGTH_SHORT).show()
                                }
                        } catch (e: Exception) {
                            Log.e(TAG, "Firestore error: ${e.message}", e)
                            Toast.makeText(this, "Error connecting to database. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                2 -> {
                    setContentView(R.layout.activity_register2)

                    // Set back button to return to previous step
                    val btnBack = findViewById<ImageButton>(R.id.btnBack) ?: return
                    btnBack.setOnClickListener {
                        currentStep = 1
                        showRegistrationStep(currentStep)
                    }

                    // Find name input - safely
                    val etFullName = findViewById<EditText>(R.id.etFullName) ?: return

                    // Find the "Log in" text and set click listener to go back to login
                    val tvLogIn = findViewById<TextView>(R.id.tvLogIn) ?: return
                    tvLogIn.setOnClickListener {
                        // Go back to login screen
                        finish()
                    }

                    // Set continue button for next step
                    val btnContinue = findViewById<AppCompatButton>(R.id.btnContinue) ?: return
                    btnContinue.setOnClickListener {
                        // Validate and save name
                        val name = etFullName.text.toString().trim()
                        if (name.isEmpty()) {
                            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        // Save name and proceed
                        userName = name
                        currentStep = 3
                        showRegistrationStep(currentStep)
                    }
                }
                3 -> {
                    setContentView(R.layout.activity_register3)

                    // Set back button to return to previous step
                    val btnBack = findViewById<ImageButton>(R.id.btnBack) ?: return
                    btnBack.setOnClickListener {
                        currentStep = 2
                        showRegistrationStep(currentStep)
                    }

                    // Find birthday input - safely
                    val etBirthday = findViewById<EditText>(R.id.etBirthday) ?: return

                    // Find the "Log in" text and set click listener to go back to login
                    val tvLogIn = findViewById<TextView>(R.id.tvLogIn) ?: return
                    tvLogIn.setOnClickListener {
                        // Go back to login screen
                        finish()
                    }

                    // Set continue button for next step
                    val btnContinue = findViewById<AppCompatButton>(R.id.btnContinue) ?: return
                    btnContinue.setOnClickListener {
                        // Validate and save birthday
                        val birthday = etBirthday.text.toString().trim()
                        if (birthday.isEmpty()) {
                            Toast.makeText(this, "Please enter your birthday", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        // Save birthday and proceed
                        userBirthday = birthday
                        currentStep = 4
                        showRegistrationStep(currentStep)
                    }
                }
                4 -> {
                    setContentView(R.layout.activity_register4)

                    // Set back button to return to previous step
                    val btnBack = findViewById<ImageButton>(R.id.btnBack) ?: return
                    btnBack.setOnClickListener {
                        currentStep = 3
                        showRegistrationStep(currentStep)
                    }

                    // Find the "Log in" text and set click listener to go back to login
                    val tvLogIn = findViewById<TextView>(R.id.tvLogIn) ?: return
                    tvLogIn.setOnClickListener {
                        // Go back to login screen
                        finish()
                    }

                    // Setup gender spinner with options - safely
                    try {
                        setupGenderSpinner()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting up gender spinner: ${e.message}", e)
                        Toast.makeText(this, "Error loading gender options", Toast.LENGTH_SHORT).show()
                    }

                    // Set continue button for next step
                    val btnContinue = findViewById<AppCompatButton>(R.id.btnContinue) ?: return
                    btnContinue.setOnClickListener {
                        // Validate gender selection
                        if (selectedGender.isEmpty() || selectedGender == "Select your gender") {
                            Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show()
                        } else {
                            currentStep = 5
                            showRegistrationStep(currentStep)
                        }
                    }
                }
                5 -> {
                    setContentView(R.layout.activity_register5)

                    // Set back button to return to previous step
                    val btnBack = findViewById<ImageButton>(R.id.btnBack) ?: return
                    btnBack.setOnClickListener {
                        currentStep = 4
                        showRegistrationStep(currentStep)
                    }

                    // Find the "Log in" text and set click listener to go back to login
                    val tvLogIn = findViewById<TextView>(R.id.tvLogIn) ?: return
                    tvLogIn.setOnClickListener {
                        // Go back to login screen
                        finish()
                    }

                    // Set finish button to complete registration
                    val btnFinish = findViewById<AppCompatButton>(R.id.btnFinish) ?: return
                    btnFinish.setOnClickListener {
                        // Validate passwords
                        val etPassword = findViewById<EditText>(R.id.etPassword) ?: return@setOnClickListener
                        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword) ?: return@setOnClickListener

                        val password = etPassword.text.toString().trim()
                        val confirmPassword = etConfirmPassword.text.toString().trim()

                        if (password.isEmpty()) {
                            Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        if (password != confirmPassword) {
                            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        // Passwords match, save password
                        userPassword = password

                        // Move to confirmation code screen
                        currentStep = 6
                        showRegistrationStep(currentStep)

                        // In a real app, you would send a verification code to the email address
                        // For demo purposes, we'll simulate sending the code
                        simulateSendingVerificationCode()
                    }
                }
                6 -> {
                    // Confirmation code screen
                    setContentView(R.layout.activity_register_confirm)

                    // Update email in instructions
                    val tvConfirmationInstructions = findViewById<TextView>(R.id.tvConfirmationInstructions) ?: return
                    tvConfirmationInstructions.text = "Enter the 6-digit code we sent to $userEmail"

                    // Set up the code digits for auto-focus
                    try {
                        setupVerificationCodeFields()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting up verification code fields: ${e.message}", e)
                    }

                    // Set up resend code functionality
                    val tvResendCode = findViewById<TextView>(R.id.tvResendCode) ?: return
                    tvResendCode.setOnClickListener {
                        simulateSendingVerificationCode()
                        Toast.makeText(this, "Code resent to $userEmail", Toast.LENGTH_SHORT).show()
                    }

                    // Set up confirm button
                    val btnConfirm = findViewById<AppCompatButton>(R.id.btnConfirm) ?: return
                    btnConfirm.setOnClickListener {
                        try {
                            // Check if all digits are entered
                            val codeComplete = codeDigits.all { it.text.isNotEmpty() }

                            if (codeComplete) {
                                // In a real app, you would validate the code against what was sent
                                // For now, we'll consider it verified and save the user data
                                saveUserToFirestore()
                            } else {
                                Toast.makeText(this, "Please enter the complete 6-digit code", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing verification code: ${e.message}", e)
                            Toast.makeText(this, "Error processing code. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing registration step $step: ${e.message}", e)
            Toast.makeText(this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun simulateSendingVerificationCode() {
        // In a real app, you would generate and send a verification code
        // This is just a placeholder for demonstration purposes
        Log.d(TAG, "Simulating sending verification code to $userEmail")
    }

    private fun saveUserToFirestore() {
        try {
            // Get Firestore instance safely
            val db = Firebase.firestore

            // Create a user map with all the collected data
            val user = hashMapOf(
                "email" to userEmail,
                "fullName" to userName,
                "birthday" to userBirthday,
                "gender" to selectedGender,
                "password" to userPassword,  // Note: In a production app, passwords should be handled securely
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            // Add the user to Firestore
            db.collection("users")
                .document(userEmail)  // Using email as document ID for easy lookup
                .set(user)
                .addOnSuccessListener {
                    Log.d(TAG, "User successfully added to Firestore")
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_LONG).show()

                    // Navigate to main app screen or login
                    // For this example, we'll just return to login
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error adding user to Firestore: ${e.message}", e)
                    Toast.makeText(this, "Error creating account: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing Firestore: ${e.message}", e)
            Toast.makeText(this, "Error connecting to database. Please try again.", Toast.LENGTH_SHORT).show()
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

        // Set selection hint
        spinner.setSelection(0)

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

    private fun setupVerificationCodeFields() {
        // Get all 6 digit fields - safely
        codeDigits = listOf(
            findViewById(R.id.etCodeDigit1) ?: throw NullPointerException("Code digit 1 not found"),
            findViewById(R.id.etCodeDigit2) ?: throw NullPointerException("Code digit 2 not found"),
            findViewById(R.id.etCodeDigit3) ?: throw NullPointerException("Code digit 3 not found"),
            findViewById(R.id.etCodeDigit4) ?: throw NullPointerException("Code digit 4 not found"),
            findViewById(R.id.etCodeDigit5) ?: throw NullPointerException("Code digit 5 not found"),
            findViewById(R.id.etCodeDigit6) ?: throw NullPointerException("Code digit 6 not found")
        )

        // Set focus to first digit
        codeDigits[0].requestFocus()

        // Add text change listeners to auto-advance focus
        for (i in codeDigits.indices) {
            val currentDigit = codeDigits[i]

            currentDigit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1) {
                        // Move to next digit
                        if (i < codeDigits.size - 1) {
                            codeDigits[i + 1].requestFocus()
                        }
                    } else if (s?.isEmpty() == true) {
                        // Move to previous digit when deleted
                        if (i > 0) {
                            codeDigits[i - 1].requestFocus()
                        }
                    }
                }
            })
        }
    }
}