package com.example.trashcashcampus_mobile

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
    
    companion object {
        /**
         * Safely decode a bitmap resource with memory optimizations
         */
        fun decodeSampledBitmapFromResource(
            context: Context,
            resourceId: Int,
            reqWidth: Int,
            reqHeight: Int
        ): Bitmap? {
            return try {
                // First decode with inJustDecodeBounds=true to check dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeResource(context.resources, resourceId, options)
                
                // Calculate inSampleSize
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                
                // Use RGB_565 to reduce memory usage (half of ARGB_8888)
                options.inPreferredConfig = Bitmap.Config.RGB_565
                
                // Decode bitmap with inSampleSize set
                options.inJustDecodeBounds = false
                
                val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)
                Log.d("BitmapUtil", "Loaded bitmap: ${bitmap?.width}x${bitmap?.height}, bytes: ${bitmap?.byteCount ?: 0}")
                bitmap
            } catch (e: Exception) {
                Log.e("BitmapUtil", "Error decoding bitmap: ${e.message}", e)
                null
            }
        }
        
        /**
         * Calculate the optimal sample size for loading a bitmap
         */
        private fun calculateInSampleSize(
            options: BitmapFactory.Options,
            reqWidth: Int,
            reqHeight: Int
        ): Int {
            // Raw height and width of image
            val (height: Int, width: Int) = options.run { outHeight to outWidth }
            var inSampleSize = 1
            
            if (height > reqHeight || width > reqWidth) {
                val halfHeight: Int = height / 2
                val halfWidth: Int = width / 2
                
                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
                
                // For very large images, limit the max size
                val maxSize = 4096
                if (height / inSampleSize > maxSize || width / inSampleSize > maxSize) {
                    inSampleSize *= 2
                }
            }
            
            return inSampleSize
        }
    }
}