package com.example.trashcashcampus_mobile

import android.app.Application
import android.util.Log
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp

class TrashCashApplication : Application() {
    private val TAG = "TrashCashApplication"
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase: ${e.message}", e)
        }
        
        // Check Google Play Services availability
        try {
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
            if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
                Log.w(TAG, "Google Play Services not available (code $resultCode)")
                if (googleApiAvailability.isUserResolvableError(resultCode)) {
                    Log.i(TAG, "User resolvable Google Play Services error")
                }
            } else {
                Log.d(TAG, "Google Play Services available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Google Play Services: ${e.message}", e)
        }
    }
}