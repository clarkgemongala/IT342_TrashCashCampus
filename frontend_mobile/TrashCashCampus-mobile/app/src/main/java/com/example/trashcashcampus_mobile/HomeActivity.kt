package com.example.trashcashcampus_mobile

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)

        // Set up user info
        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        val tvTotalPoints = findViewById<TextView>(R.id.tvTotalPoints)
        val tvPointsIncrement = findViewById<TextView>(R.id.tvPointsIncrement)
        val tvWeeklyGoal = findViewById<TextView>(R.id.tvWeeklyGoal)
        val tvGoalProgress = findViewById<TextView>(R.id.tvGoalProgress)

        // You would normally get this data from a database or API
        tvUserName.text = "John"
        tvTotalPoints.text = "250"
        tvPointsIncrement.text = "+35"
        tvWeeklyGoal.text = "1000"
        tvGoalProgress.text = "25% completed"

        // Set up button click listeners
        val btnViewAllRewards = findViewById<Button>(R.id.btnViewAllRewards)
        val btnScanQR = findViewById<Button>(R.id.btnScanQR)

        btnViewAllRewards.setOnClickListener {
            Toast.makeText(this, "View All Rewards clicked", Toast.LENGTH_SHORT).show()
            // Navigate to rewards page
        }

        btnScanQR.setOnClickListener {
            Toast.makeText(this, "Scan QR clicked", Toast.LENGTH_SHORT).show()
            // Open camera for QR scanning
        }
    }
}