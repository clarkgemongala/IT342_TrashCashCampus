package com.example.trashcashcampus_mobile
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Handle sign up button click
        findViewById<TextView>(R.id.tvSignUp).setOnClickListener {
            try {
                val intent = Intent(this, RegisterActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                // Log the exception to see what's happening
                Log.e("MainActivity", "Error starting RegisterActivity", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Handle login button click
        findViewById<AppCompatButton>(R.id.btnLogin).setOnClickListener {
            val email = findViewById<EditText>(R.id.etEmail).text.toString().trim()
            val password = findViewById<EditText>(R.id.etPassword).text.toString().trim()

            // Very basic validation
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            } else {
                // In a real app, you would validate credentials against a database or API
                // For this example, we'll just navigate to the Home screen
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)

                // Optional: finish this activity so pressing back won't return to login
                // finish()
            }
        }
    }
}