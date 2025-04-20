package com.example.trashcashcampus_mobile

import android.app.DatePickerDialog
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
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RegisterActivity : AppCompatActivity() {
    private val TAG = "RegisterActivity" // For logging

    private var currentStep = 1
    private var selectedGender = ""

    // User data variables
    private var userEmail = ""
    private var userName = ""
    private var userBirthday = ""
    private var userPassword = ""

    // Firebase Authentication
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // Initialize Firebase Auth
            auth = FirebaseAuth.getInstance()

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

                        // Check if the email ends with @cit.edu
                        if (!email.endsWith("@cit.edu")) {
                            Toast.makeText(this, "Please use your CIT email (@cit.edu)", Toast.LENGTH_SHORT).show()
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
                            Log.e(TAG, "Error accessing Firestore: ${e.message}", e)
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

                    // Find the "Log in" text and set click listener to go back to login
                    val tvLogIn = findViewById<TextView>(R.id.tvLogIn) ?: return
                    tvLogIn.setOnClickListener {
                        // Go back to login screen
                        finish()
                    }

                    // Find the TextView that will display the selected date
                    val tvSelectedDate = findViewById<TextView>(R.id.tvSelectedDate) ?: return

                    // Find the date picker container
                    val datePickerContainer = findViewById<RelativeLayout>(R.id.datePickerContainer) ?: return

                    // Set up click listener for the date field to open date picker
                    datePickerContainer.setOnClickListener {
                        showDatePickerDialog()
                    }

                    // Also set the calendar icon to open the date picker
                    val ivCalendar = findViewById<ImageView>(R.id.ivCalendar) ?: return
                    ivCalendar.setOnClickListener {
                        showDatePickerDialog()
                    }

                    // Set continue button for next step
                    val btnContinue = findViewById<AppCompatButton>(R.id.btnContinue) ?: return
                    btnContinue.setOnClickListener {
                        // Get the date from the TextView
                        val birthday = tvSelectedDate.text.toString().trim()

                        // Validate date
                        if (birthday.isEmpty() || birthday == "DD/MM/YYYY") {
                            Toast.makeText(this, "Please select your birthday", Toast.LENGTH_SHORT).show()
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

                        val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$"
                        if (!password.matches(passwordPattern.toRegex())) {
                            Toast.makeText(this, "Password must be at least 8 characters with numbers, upper and lower case letters, and special characters", Toast.LENGTH_LONG).show()
                            return@setOnClickListener
                        }

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

                        // Create the user account with Firebase Auth
                        createUserWithFirebaseAuth()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing registration step $step: ${e.message}", e)
            Toast.makeText(this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show()
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

    private fun createUserWithFirebaseAuth() {
        val auth = FirebaseAuth.getInstance()

        // Show progress or loading indicator
        // (You would need to implement this)

        // Create user with email and password
        auth.createUserWithEmailAndPassword(userEmail, userPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // User account created successfully
                    val user = auth.currentUser

                    // Send verification email
                    sendVerificationEmail(user)
                } else {
                    // Account creation failed
                    Log.e(TAG, "Error creating user: ${task.exception?.message}", task.exception)
                    Toast.makeText(this,
                        "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun sendVerificationEmail(user: FirebaseUser?) {
        user?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Email sent successfully
                    Toast.makeText(this,
                        "Verification email sent to $userEmail",
                        Toast.LENGTH_LONG).show()

                    // Store the additional user data in Firestore
                    saveUserDataToFirestore(user.uid)

                    // Navigate to a verification waiting screen or login screen
                    navigateToVerificationWaitingScreen()
                } else {
                    // Failed to send email
                    Log.e(TAG, "Error sending verification email: ${task.exception?.message}", task.exception)
                    Toast.makeText(this,
                        "Failed to send verification email: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserDataToFirestore(userId: String) {
        try {
            val db = Firebase.firestore

            // Create a user map with the collected data
            val userData = hashMapOf(
                "userId" to userId,
                "email" to userEmail,
                "fullName" to userName,
                "birthday" to userBirthday,
                "gender" to selectedGender,
                "isEmailVerified" to false,  // Will update this when verified
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            // Add the user to Firestore
            db.collection("users")
                .document(userId)  // Using Firebase Auth UID as document ID
                .set(userData)
                .addOnSuccessListener {
                    Log.d(TAG, "User data successfully saved to Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving user data to Firestore: ${e.message}", e)
                    Toast.makeText(this,
                        "Your account was created but we couldn't save all your details. Please update your profile later.",
                        Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing Firestore: ${e.message}", e)
        }
    }

    private fun navigateToVerificationWaitingScreen() {
        val intent = Intent(this, VerificationWaitingActivity::class.java)
        intent.putExtra("USER_EMAIL", userEmail)
        startActivity(intent)
        finish() // Close the registration activity
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
}