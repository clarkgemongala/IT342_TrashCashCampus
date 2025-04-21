package com.example.trashcashcampus_mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.example.trashcashcampus_mobile.models.ApiClient
import com.example.trashcashcampus_mobile.models.ScanResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class QRScannerActivity : AppCompatActivity() {
    private val tag = "QRScannerActivity"
    private lateinit var codeScanner: CodeScanner
    private lateinit var scannerView: CodeScannerView
    private lateinit var tvScannerPrompt: TextView
    private lateinit var btnCancel: Button
    private lateinit var progressBar: ProgressBar
    
    // Camera permission request code
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    
    // Firebase Auth
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        
        // Initialize UI elements
        initializeUI()
        
        // Check camera permission
        if (hasCameraPermission()) {
            setupScanner()
        } else {
            requestCameraPermission()
        }
        
        // Set up button listeners
        setupListeners()
    }
    
    private fun initializeUI() {
        scannerView = findViewById(R.id.scanner_view)
        tvScannerPrompt = findViewById(R.id.tvScannerPrompt)
        btnCancel = findViewById(R.id.btnCancel)
        progressBar = findViewById(R.id.progressBar)
        
        // Initially hide progress bar
        progressBar.visibility = View.GONE
    }
    
    private fun setupScanner() {
        codeScanner = CodeScanner(this, scannerView)
        
        // Configure scanner options
        codeScanner.apply {
            camera = CodeScanner.CAMERA_BACK
            formats = CodeScanner.ALL_FORMATS
            autoFocusMode = AutoFocusMode.SAFE
            scanMode = ScanMode.SINGLE
            isAutoFocusEnabled = true
            isFlashEnabled = false
            
            // Handle successful scan
            decodeCallback = DecodeCallback { result ->
                runOnUiThread {
                    // Show processing UI
                    showProcessingUI()
                    
                    // Process the QR code
                    processQRCode(result.text)
                }
            }
            
            // Handle scan errors
            errorCallback = ErrorCallback { error ->
                runOnUiThread {
                    Log.e(tag, "Scanner error: ${error.message}")
                    Toast.makeText(this@QRScannerActivity, 
                        "Scanner error: ${error.message}", 
                        Toast.LENGTH_LONG).show()
                }
            }
        }
        
        // Handle user tapping on the scanner view to focus
        scannerView.setOnClickListener {
            codeScanner.startPreview()
        }
    }
    
    private fun showProcessingUI() {
        tvScannerPrompt.text = "Processing QR code..."
        progressBar.visibility = View.VISIBLE
        btnCancel.isEnabled = false
    }
    
    private fun processQRCode(qrCode: String) {
        Log.d(tag, "Scanned QR code: $qrCode")
        
        // Get the current user ID
        val userId = auth.currentUser?.uid
        
        if (userId != null) {
            // Determine waste type (in a real app, you might ask the user or use image recognition)
            val wasteType = "plastic" // Default for demo purposes
            
            // Send to backend via API
            lifecycleScope.launch {
                try {
                    val result = ApiClient.scanBin(userId, qrCode, wasteType)
                    showScanResultDialog(result)
                } catch (e: Exception) {
                    Log.e(tag, "Error processing scan", e)
                    showErrorDialog("Failed to process scan. Please try again.")
                }
            }
        } else {
            // User not logged in
            showErrorDialog("User not authenticated. Please log in again.")
        }
    }
    
    private fun showScanResultDialog(result: ScanResult) {
        // Hide progress bar
        progressBar.visibility = View.GONE
        btnCancel.isEnabled = true
        
        // Build and show result dialog
        val builder = AlertDialog.Builder(this)
        
        if (result.success) {
            builder.setTitle("Scan Successful!")
                .setMessage("${result.message}\n\n" +
                        "Points earned: +${result.pointsEarned}\n" +
                        "Total points: ${result.totalPoints}\n\n" +
                        "Did you know? ${result.fact}")
                .setPositiveButton("Great!") { _, _ ->
                    finish() // Return to previous screen
                }
                .setCancelable(false)
        } else {
            builder.setTitle("Scan Failed")
                .setMessage(result.message)
                .setPositiveButton("Try Again") { _, _ ->
                    // Reset scanner and UI
                    tvScannerPrompt.text = "Scan a TrashCash QR code"
                    codeScanner.startPreview()
                }
                .setNegativeButton("Cancel") { _, _ ->
                    finish() // Return to previous screen
                }
                .setCancelable(false)
        }
        
        builder.create().show()
    }
    
    private fun showErrorDialog(message: String) {
        // Hide progress bar
        progressBar.visibility = View.GONE
        btnCancel.isEnabled = true
        
        // Show error dialog
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("Try Again") { _, _ ->
                // Reset scanner and UI
                tvScannerPrompt.text = "Scan a TrashCash QR code"
                codeScanner.startPreview()
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish() // Return to previous screen
            }
            .setCancelable(false)
            .create()
            .show()
    }
    
    private fun setupListeners() {
        btnCancel.setOnClickListener {
            finish() // Return to previous screen
        }
    }
    
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, set up scanner
                setupScanner()
            } else {
                // Permission denied
                Toast.makeText(
                    this,
                    "Camera permission is required to scan QR codes",
                    Toast.LENGTH_LONG
                ).show()
                finish() // Return to previous screen
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (::codeScanner.isInitialized) {
            codeScanner.startPreview()
        }
    }
    
    override fun onPause() {
        super.onPause()
        if (::codeScanner.isInitialized) {
            codeScanner.releaseResources()
        }
    }
} 