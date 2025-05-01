package com.example.trashcashcampus_mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
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
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QRScannerActivity : AppCompatActivity() {
    private val tag = "QRScannerActivity"
    private lateinit var codeScanner: CodeScanner
    private lateinit var scannerView: CodeScannerView
    private lateinit var tvScannerPrompt: TextView
    private lateinit var btnCancel: Button
    private lateinit var progressBar: ProgressBar
    
    // Photo verification UI elements
    private lateinit var photoVerificationLayout: ConstraintLayout
    private lateinit var photoImageView: ImageView
    private lateinit var btnTakePhoto: Button
    private lateinit var btnSubmit: Button
    private lateinit var radioGroupItemSize: RadioGroup
    private lateinit var radioBtnSmall: RadioButton
    private lateinit var radioBtnBig: RadioButton
    
    // Camera permission request codes
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private val REQUEST_IMAGE_CAPTURE = 101
    
    // Firebase Auth
    private lateinit var auth: FirebaseAuth
    
    // Captured data
    private var scannedQRCode: String = ""
    private var scannedBinType: String = ""
    private var capturedPhotoBase64: String = ""
    private var selectedItemSize: String = "small" // Default size
    
    // Location data from map
    private var locationName: String? = null
    
    // Authentication variables
    private var userId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        
        // Check for authentication state
        checkAuthentication()
        
        // Get the location name from the intent if available
        locationName = intent.getStringExtra("LOCATION_NAME")
        Log.d(tag, "LOCATION: Starting scanner with location: $locationName")
        
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
        
        // Update the prompt if launched from a map marker
        if (locationName != null) {
            tvScannerPrompt.text = "Scanning at $locationName\nPlease scan the QR code"
        }
    }
    
    private fun checkAuthentication() {
        // Get the current Firebase user
        val currentUser = auth.currentUser
        
        // Check if user is authenticated with Firebase
        if (currentUser != null) {
            Log.d(tag, "User authenticated: ${currentUser.uid}")
            userId = currentUser.uid
            
            // Also check shared preferences for backup auth
            val prefs = getSharedPreferences("TrashCashPrefs", MODE_PRIVATE)
            if (prefs.getString("userId", null) == null) {
                // Save user ID to preferences if not already saved
                prefs.edit().putString("userId", currentUser.uid).apply()
                Log.d(tag, "Saved user ID to preferences")
            }
        } else {
            // Try to get user ID from shared preferences as backup
            val prefs = getSharedPreferences("TrashCashPrefs", MODE_PRIVATE)
            userId = prefs.getString("userId", null)
            
            if (userId != null) {
                Log.d(tag, "User ID found in preferences: $userId")
            } else {
                // No authentication found anywhere
                Log.e(tag, "User not authenticated in Firebase or preferences")
                showAuthError()
                return
            }
        }
    }
    
    private fun showAuthError() {
        AlertDialog.Builder(this)
            .setTitle("Authentication Error")
            .setMessage("You need to be signed in to use this feature. Please sign in again.")
            .setPositiveButton("OK") { _, _ ->
                // Return to login screen
                navigateToLogin()
            }
            .setCancelable(false)
            .create()
            .show()
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun initializeUI() {
        scannerView = findViewById(R.id.scanner_view)
        tvScannerPrompt = findViewById(R.id.tvScannerPrompt)
        btnCancel = findViewById(R.id.btnCancel)
        progressBar = findViewById(R.id.progressBar)
        
        // Initialize photo verification UI
        photoVerificationLayout = findViewById(R.id.photoVerificationLayout)
        photoImageView = findViewById(R.id.photoImageView)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnSubmit = findViewById(R.id.btnSubmit)
        radioGroupItemSize = findViewById(R.id.radioGroupItemSize)
        radioBtnSmall = findViewById(R.id.radioBtnSmall)
        radioBtnBig = findViewById(R.id.radioBtnBig)
        
        // Configure the image view for proper display
        photoImageView.setImageResource(android.R.drawable.ic_menu_camera)
        photoImageView.scaleType = ImageView.ScaleType.CENTER
        
        // Initially hide progress bar and photo verification UI
        progressBar.visibility = View.GONE
        photoVerificationLayout.visibility = View.GONE
        
        // Make sure the scanner container is visible
        findViewById<View>(R.id.scannerContainer).visibility = View.VISIBLE
        scannerView.visibility = View.VISIBLE
        
        // Make sure cancel button is enabled
        btnCancel.isEnabled = true
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
                    
                    // Try to parse the QR code as JSON
                    try {
                        val json = JSONObject(result.text)
                        scannedQRCode = result.text
                        
                        // Check if this is a bin QR code
                        if (json.has("binId") && json.has("binName")) {
                            scannedBinType = json.getString("binId")
                            val binName = json.getString("binName")
                            
                            // Add location info to the scanned QR code if we have it
                            if (locationName != null) {
                                // Create an enhanced QR code with location info
                                val enhancedQrData = JSONObject(result.text)
                                enhancedQrData.put("locationName", locationName)
                                
                                // IMPORTANT: Log current location and QR data
                                Log.d(tag, "LOCATION: Scanning at $locationName")
                                Log.d(tag, "LOCATION: Original QR data: $result.text")
                                Log.d(tag, "LOCATION: Enhanced QR data: $enhancedQrData")
                                
                                scannedQRCode = enhancedQrData.toString()
                                Log.d(tag, "Enhanced QR data with location: $scannedQRCode")
                            } else {
                                // Without location name, check if we can get it from the intent extras
                                val building = intent.getStringExtra("BUILDING_NAME")
                                if (building != null) {
                                    // Use the building name from intent extras
                                    val enhancedQrData = JSONObject(result.text)
                                    enhancedQrData.put("locationName", building)
                                    scannedQRCode = enhancedQrData.toString()
                                    Log.d(tag, "Enhanced QR data with building from intent: $scannedQRCode")
                                } else if (!json.has("locationName")) {
                                    // Only use default if no location info exists in QR code or from intent
                                    val enhancedQrData = JSONObject(result.text)
                                    enhancedQrData.put("locationName", "NGE Building")
                                    scannedQRCode = enhancedQrData.toString()
                                    Log.d(tag, "Enhanced QR data with default building: $scannedQRCode")
                                }
                            }
                            
                            // Show photo verification UI
                            showPhotoVerificationUI(binName)
                        } else {
                            // Not a valid bin QR code
                            showErrorDialog("Invalid QR code. Please scan a valid TrashCash bin QR code.")
                        }
                    } catch (e: Exception) {
                        // Not a JSON QR code, try to process as plain text
                        Log.e(tag, "Error parsing QR code JSON", e)
                        scannedQRCode = result.text
                        
                        // For backwards compatibility, assume it's a bin ID
                        scannedBinType = "recyclable" // Default type
                        
                        // Create a JSON object with the bin ID and location
                        if (locationName != null) {
                            try {
                                val jsonData = JSONObject()
                                jsonData.put("binId", scannedQRCode)
                                jsonData.put("binName", "Recyclable Bin")
                                jsonData.put("locationName", locationName)
                                
                                // IMPORTANT: Log for debugging
                                Log.d(tag, "LOCATION: Creating JSON with location=$locationName")
                                
                                scannedQRCode = jsonData.toString()
                                Log.d(tag, "Created JSON QR data with location: $scannedQRCode")
                            } catch (e2: Exception) {
                                Log.e(tag, "Error creating JSON data", e2)
                            }
                        } else {
                            // If no location name from intent, check for BUILDING_NAME
                            val building = intent.getStringExtra("BUILDING_NAME")
                            try {
                                val jsonData = JSONObject()
                                jsonData.put("binId", scannedQRCode)
                                jsonData.put("binName", "Recyclable Bin")
                                // Use building from intent if available, otherwise default
                                jsonData.put("locationName", building ?: "NGE Building") 
                                scannedQRCode = jsonData.toString()
                                Log.d(tag, building?.let { "Created JSON QR data with building: $scannedQRCode" } 
                                    ?: "Created JSON QR data with default building: $scannedQRCode")
                            } catch (e2: Exception) {
                                Log.e(tag, "Error creating JSON data", e2)
                            }
                        }
                        
                        showPhotoVerificationUI("Recyclable Bin")
                    }
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
        btnCancel.isEnabled = true  // Ensure cancel button is enabled
    }
    
    private fun showPhotoVerificationUI(binName: String) {
        try {
            // Update prompt
            tvScannerPrompt.text = "Take a photo of your waste in the $binName"
            
            // Hide scanner view container
            findViewById<View>(R.id.scannerContainer).visibility = View.GONE
            
            // Hide progress
            progressBar.visibility = View.GONE
            
            // Show photo verification UI
            photoVerificationLayout.visibility = View.VISIBLE
            
            // Make sure image view is visible with default camera icon
            photoImageView.setImageResource(android.R.drawable.ic_menu_camera)
            photoImageView.scaleType = ImageView.ScaleType.CENTER
            
            // Enable the take photo button
            btnTakePhoto.isEnabled = true
            
            // Enable the submit button only if photo has been taken
            btnSubmit.isEnabled = false
            
            // Ensure cancel button is enabled
            btnCancel.isEnabled = true
        } catch(e: Exception) {
            Log.e(tag, "Error showing photo verification UI", e)
            Toast.makeText(this, "Error displaying camera interface", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun takePicture() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } else {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                val imageBitmap = data?.extras?.get("data") as Bitmap
                
                // Set the captured image to the ImageView with proper scaling
                photoImageView.setImageBitmap(imageBitmap)
                photoImageView.scaleType = ImageView.ScaleType.CENTER_CROP
                
                // Convert the bitmap to Base64 string
                capturedPhotoBase64 = bitmapToBase64(imageBitmap)
                
                // Enable submit button
                btnSubmit.isEnabled = true
            } catch (e: Exception) {
                Log.e(tag, "Error processing camera result", e)
                Toast.makeText(this, "Error processing photo, please try again", Toast.LENGTH_SHORT).show()
                
                // Reset photo view
                photoImageView.setImageResource(android.R.drawable.ic_menu_camera)
                photoImageView.scaleType = ImageView.ScaleType.CENTER
                
                // Don't enable submit
                btnSubmit.isEnabled = false
            }
        }
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    
    private fun processVerifiedWaste() {
        // Double-check authentication
        checkAuthentication()
        
        // Show progress while submitting
        progressBar.visibility = View.VISIBLE
        btnSubmit.isEnabled = false
        btnTakePhoto.isEnabled = false
        btnCancel.isEnabled = true  // Always keep cancel enabled
        
        // Get the current user ID
        val uid = userId
        
        if (uid.isNullOrEmpty()) {
            // User not logged in or ID not available
            showErrorDialog("User not authenticated. Please log in again.")
            return
        }
        
        // Determine waste type based on which bin was scanned
        val wasteType = when (scannedBinType) {
            "recyclable" -> "plastic" // Default for recyclable bin
            "biodegradable" -> "organic"
            "non-biodegradable" -> "metal" // Default for non-biodegradable
            else -> "plastic" // Default
        }
        
        // Get the selected item size
        selectedItemSize = if (radioBtnBig.isChecked) "big" else "small"
        
        // Get current user name/email
        val userName = auth.currentUser?.displayName ?: auth.currentUser?.email ?: "Unknown User"
        
        // Get current date/time
        val timestamp = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = dateFormat.format(Date(timestamp))
        
        // Extract location data from intent or scanned QR code
        var submissionLocation = locationName
        
        // If locationName is null, check if it's in the QR code
        if (submissionLocation == null) {
            try {
                val qrData = JSONObject(scannedQRCode)
                if (qrData.has("locationName")) {
                    submissionLocation = qrData.getString("locationName")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error parsing QR data for location", e)
            }
        }
        
        // Last resort - check for BUILDING_NAME in intent
        if (submissionLocation == null) {
            submissionLocation = intent.getStringExtra("BUILDING_NAME")
        }
        
        // Debug logs for location tracking - very important
        Log.d(tag, "LOCATION: Processing waste with location: $submissionLocation")
        Log.d(tag, "LOCATION: Original QR data: $scannedQRCode")
        
        // Make sure the final QR code has location data
        try {
            val finalQrData = JSONObject(scannedQRCode)
            
            // If the QR code doesn't have location, but we have it from somewhere else, add it
            if (!finalQrData.has("locationName") && !submissionLocation.isNullOrEmpty()) {
                finalQrData.put("locationName", submissionLocation)
                scannedQRCode = finalQrData.toString()
                Log.d(tag, "LOCATION: Added missing location to QR data: $scannedQRCode")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error updating QR data with location", e)
        }
        
        // Send to backend via API with photo
        lifecycleScope.launch {
            try {
                Log.d(tag, "Submitting waste for admin approval: userId=$uid, binType=$scannedBinType, size=$selectedItemSize, location=$submissionLocation")
                
                // Submit with pending approval status
                val result = ApiClient.submitRecyclingForApproval(
                    userId = uid,
                    userName = userName,
                    qrCode = scannedQRCode,
                    binType = scannedBinType,
                    wasteType = wasteType,
                    photoBase64 = capturedPhotoBase64,
                    itemSize = selectedItemSize,
                    timestamp = timestamp,
                    dateTime = dateStr
                )
                
                showSubmissionResultDialog(result)
            } catch (e: Exception) {
                Log.e(tag, "Error processing scan", e)
                
                // Create a fallback result for when submissions fail completely
                val fallbackResult = ScanResult(
                    success = true,
                    status = "pending",
                    message = "Your recycling has been processed in offline mode due to connectivity issues. Points will be awarded after admin approval.",
                    pointsEarned = calculatePointsForWasteType(scannedBinType) + (if (selectedItemSize == "big") 5 else 0),
                    totalPoints = 0, // Don't add to total yet
                    fact = "Going offline doesn't stop your recycling effort! Thank you for helping the environment."
                )
                
                showSubmissionResultDialog(fallbackResult)
            }
        }
    }
    
    private fun showSubmissionResultDialog(result: ScanResult) {
        // Hide progress bar
        progressBar.visibility = View.GONE
        
        // Build and show result dialog
        val builder = AlertDialog.Builder(this)
        
        if (result.success) {
            val message = if (result.message.contains("Offline Mode")) {
                // Add notice for offline mode submissions
                "NOTICE: The app is currently in offline mode due to connectivity issues.\n\n" +
                "Your recycling has been recorded locally and will be synced when connectivity is restored.\n\n" +
                "Upon admin approval, you will receive:\n" +
                "• ${calculatePointsForWasteType(scannedBinType)} points for this waste type\n" +
                "${if (selectedItemSize == "big") "• 5 bonus points for large item size\n" else ""}\n" +
                "Thank you for recycling!"
            } else {
                "Your recycling submission has been received and is pending admin approval.\n\n" +
                "Once approved, you will receive:\n" +
                "• ${calculatePointsForWasteType(scannedBinType)} points for this waste type\n" +
                "${if (selectedItemSize == "big") "• 5 bonus points for large item size\n" else ""}\n" +
                "Thank you for recycling!"
            }
            
            builder.setTitle("Submission Successful!")
                .setMessage(message)
                .setPositiveButton("Great!") { _, _ ->
                    // Set result to indicate points should be refreshed
                    setResult(RESULT_OK)
                    finish() // Return to previous screen
                }
                .setCancelable(false)
        } else {
            builder.setTitle("Submission Failed")
                .setMessage(result.message)
                .setPositiveButton("Try Again") { _, _ ->
                    // Reset and go back to scanner
                    resetToScanMode()
                }
                .setNegativeButton("Cancel") { _, _ ->
                    // No changes to points, just finish
                    setResult(RESULT_CANCELED)
                    finish() // Return to previous screen
                }
                .setCancelable(false)
        }
        
        builder.create().show()
    }
    
    private fun calculatePointsForWasteType(binType: String): Int {
        return when(binType) {
            "recyclable" -> 15
            "biodegradable" -> 5
            "non-biodegradable" -> 25
            else -> 10
        }
    }
    
    private fun resetToScanMode() {
        try {
            // Clear captured data
            capturedPhotoBase64 = ""
            scannedQRCode = ""
            scannedBinType = ""
            
            // Reset UI
            photoImageView.setImageResource(android.R.drawable.ic_menu_camera)
            photoImageView.scaleType = ImageView.ScaleType.CENTER
            
            // Hide photo verification layout
            photoVerificationLayout.visibility = View.GONE
            
            // Show scanner container
            findViewById<View>(R.id.scannerContainer).visibility = View.VISIBLE
            
            // Make scanner visible again
            scannerView.visibility = View.VISIBLE
            
            // Hide progress
            progressBar.visibility = View.GONE
            
            // Reset prompt
            tvScannerPrompt.text = "Scan a TrashCash QR code"
            
            // Make sure buttons are properly enabled
            btnCancel.isEnabled = true
            btnTakePhoto.isEnabled = true
            
            // Restart scanner
            codeScanner.startPreview()
        } catch(e: Exception) {
            Log.e(tag, "Error resetting to scan mode", e)
            Toast.makeText(this, "Error resetting scanner", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showErrorDialog(message: String) {
        // Hide progress bar
        progressBar.visibility = View.GONE
        
        // Re-enable buttons
        btnCancel.isEnabled = true
        btnTakePhoto.isEnabled = true
        
        // Show error dialog
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("Try Again") { _, _ ->
                // Reset and go back to scanner
                resetToScanMode()
            }
            .setNegativeButton("Cancel") { _, _ ->
                // No changes to points, just finish
                setResult(RESULT_CANCELED)
                finish() // Return to previous screen
            }
            .setCancelable(false)
            .create()
            .show()
    }
    
    private fun setupListeners() {
        // Make sure the cancel button is properly set up
        btnCancel.setOnClickListener {
            Log.d(tag, "Cancel button clicked")
            // No changes to points, just finish
            setResult(RESULT_CANCELED)
            finish() // Return to previous screen
        }
        
        btnTakePhoto.setOnClickListener {
            takePicture()
        }
        
        btnSubmit.setOnClickListener {
            processVerifiedWaste()
        }
        
        radioGroupItemSize.setOnCheckedChangeListener { _, checkedId ->
            selectedItemSize = if (checkedId == R.id.radioBtnBig) "big" else "small"
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
        
        // Re-check authentication when resuming
        checkAuthentication()
    }
    
    override fun onPause() {
        super.onPause()
        if (::codeScanner.isInitialized) {
            codeScanner.releaseResources()
        }
    }

    private fun submitScanResults(wasteType: String) {
        if (capturedPhotoBase64.isEmpty()) {
            Toast.makeText(this, "No photo captured. Please take a photo first.", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading state during submission
        progressBar.visibility = View.VISIBLE
        btnSubmit.isEnabled = false

        // Send scan data to the server (including location)
        lifecycleScope.launch {
            try {
                val response = ApiClient.submitScan(
                    qrCode = scannedQRCode,
                    wasteType = wasteType,
                    imageBase64 = capturedPhotoBase64,
                    locationName = locationName // Pass the location name to the API
                )

                // Handle response
                if (response.success) {
                    // Show success message
                    val message = "${response.message}\nYou earned ${response.pointsEarned} points!"
                    showSuccessDialog(message, response.fact, response.pointsEarned)
                } else {
                    // Show error
                    Toast.makeText(this@QRScannerActivity, 
                        "Error: ${response.message}", 
                        Toast.LENGTH_LONG).show()
                    
                    // Return to scanner
                    resetToScanner()
                }
            } catch (e: Exception) {
                // Handle error
                Log.e(tag, "Error submitting scan", e)
                Toast.makeText(this@QRScannerActivity, 
                    "Error submitting: ${e.message}", 
                    Toast.LENGTH_LONG).show()
                
                // Return to scanner
                resetToScanner()
            }
        }
    }
    
    // Add the missing methods
    private fun showSuccessDialog(message: String, fact: String, pointsEarned: Int) {
        // Hide progress first
        progressBar.visibility = View.GONE
        
        // Build and show success dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Recycling Successful!")
            .setMessage("$message\n\nDid you know?\n$fact")
            .setPositiveButton("Great!") { _, _ ->
                // Set result to indicate points should be refreshed
                val resultIntent = Intent()
                resultIntent.putExtra("POINTS_EARNED", pointsEarned)
                setResult(RESULT_OK, resultIntent)
                finish() // Return to previous screen
            }
            .setCancelable(false)
            .create()
            .show()
    }
    
    private fun resetToScanner() {
        // Hide progress
        progressBar.visibility = View.GONE
        
        // Reset UI and data (reusing existing resetToScanMode method)
        resetToScanMode()
    }
} 