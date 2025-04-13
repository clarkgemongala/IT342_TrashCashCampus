package com.example.trashcashcampus_mobile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

class RegisterActivity : AppCompatActivity() {

    private var currentStep = 1
    private var selectedGender = ""

    // Add user data variables
    private var userEmail = "john@doe.com" // Default email for testing
    private var userName = ""
    private var userBirthday = ""
    private var userPassword = ""

    // List to store verification code EditText fields
    private lateinit var codeDigits: List<EditText>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showRegistrationStep(currentStep)
    }

    private fun showRegistrationStep(step: Int) {
        when (step) {
            1 -> {
                setContentView(R.layout.activity_register1)

                // Find the "Log in" text and set click listener to go back to login
                val tvLogIn = findViewById<TextView>(R.id.tvLogIn)
                tvLogIn.setOnClickListener {
                    // Go back to login screen
                    finish()
                }

                // Set continue button to move to next step
                val btnContinue = findViewById<AppCompatButton>(R.id.btnContinue)
                btnContinue.setOnClickListener {
                    // In a real app, you would validate and save the email here
                    // For now, just proceed to next step
                    currentStep = 2
                    showRegistrationStep(currentStep)
                }
            }
            2 -> {
                setContentView(R.layout.activity_register2)

                // Set back button to return to previous step
                val btnBack = findViewById<ImageButton>(R.id.btnBack)
                btnBack.setOnClickListener {
                    currentStep = 1
                    showRegistrationStep(currentStep)
                }

                // Find the "Log in" text and set click listener to go back to login
                val tvLogIn = findViewById<TextView>(R.id.tvLogIn)
                tvLogIn.setOnClickListener {
                    // Go back to login screen
                    finish()
                }

                // Set continue button for next step
                val btnContinue = findViewById<AppCompatButton>(R.id.btnContinue)
                btnContinue.setOnClickListener {
                    // Validate and save name
                    // For now, just proceed to next step
                    currentStep = 3
                    showRegistrationStep(currentStep)
                }
            }
            3 -> {
                setContentView(R.layout.activity_register3)

                // Set back button to return to previous step
                val btnBack = findViewById<ImageButton>(R.id.btnBack)
                btnBack.setOnClickListener {
                    currentStep = 2
                    showRegistrationStep(currentStep)
                }

                // Find the "Log in" text and set click listener to go back to login
                val tvLogIn = findViewById<TextView>(R.id.tvLogIn)
                tvLogIn.setOnClickListener {
                    // Go back to login screen
                    finish()
                }

                // Set continue button for next step
                val btnContinue = findViewById<AppCompatButton>(R.id.btnContinue)
                btnContinue.setOnClickListener {
                    // Validate and save birthday
                    // For now, just proceed to next step
                    currentStep = 4
                    showRegistrationStep(currentStep)
                }
            }
            4 -> {
                setContentView(R.layout.activity_register4)

                // Set back button to return to previous step
                val btnBack = findViewById<ImageButton>(R.id.btnBack)
                btnBack.setOnClickListener {
                    currentStep = 3
                    showRegistrationStep(currentStep)
                }

                // Find the "Log in" text and set click listener to go back to login
                val tvLogIn = findViewById<TextView>(R.id.tvLogIn)
                tvLogIn.setOnClickListener {
                    // Go back to login screen
                    finish()
                }

                // Setup gender spinner with options
                setupGenderSpinner()

                // Set continue button for next step
                val btnContinue = findViewById<AppCompatButton>(R.id.btnContinue)
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
                val btnBack = findViewById<ImageButton>(R.id.btnBack)
                btnBack.setOnClickListener {
                    currentStep = 4
                    showRegistrationStep(currentStep)
                }

                // Find the "Log in" text and set click listener to go back to login
                val tvLogIn = findViewById<TextView>(R.id.tvLogIn)
                tvLogIn.setOnClickListener {
                    // Go back to login screen
                    finish()
                }

                // Set finish button to complete registration
                val btnFinish = findViewById<AppCompatButton>(R.id.btnFinish)
                btnFinish.setOnClickListener {
                    // Validate passwords
                    val etPassword = findViewById<EditText>(R.id.etPassword)
                    val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)

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
                }
            }
            6 -> {
                // Confirmation code screen
                setContentView(R.layout.activity_register_confirm)

                // Update email in instructions
                val tvConfirmationInstructions = findViewById<TextView>(R.id.tvConfirmationInstructions)
                tvConfirmationInstructions.text = "Enter the 6-digit code we sent to $userEmail"

                // Set up the code digits for auto-focus
                setupVerificationCodeFields()

                // Set up resend code functionality
                val tvResendCode = findViewById<TextView>(R.id.tvResendCode)
                tvResendCode.setOnClickListener {
                    Toast.makeText(this, "Code resent to $userEmail", Toast.LENGTH_SHORT).show()
                }

                // Set up confirm button
                val btnConfirm = findViewById<AppCompatButton>(R.id.btnConfirm)
                btnConfirm.setOnClickListener {
                    // Check if all digits are entered
                    val codeComplete = codeDigits.all { it.text.isNotEmpty() }

                    if (codeComplete) {
                        // In a real app, you would validate the code against what was sent
                        // For now, just show success and return to login
                        Toast.makeText(this, "Account verified successfully!", Toast.LENGTH_LONG).show()

                        // Return to login screen
                        finish()
                    } else {
                        Toast.makeText(this, "Please enter the complete 6-digit code", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupGenderSpinner() {
        val spinner = findViewById<Spinner>(R.id.spinnerGender)

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
        // Get all 6 digit fields
        codeDigits = listOf(
            findViewById(R.id.etCodeDigit1),
            findViewById(R.id.etCodeDigit2),
            findViewById(R.id.etCodeDigit3),
            findViewById(R.id.etCodeDigit4),
            findViewById(R.id.etCodeDigit5),
            findViewById(R.id.etCodeDigit6)
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