package com.example.trashcashcampus_mobile.utils

import android.app.Activity
import android.view.View
import android.widget.TextView
import com.example.trashcashcampus_mobile.R

/**
 * Utility class to manage loading overlay visibility across the app.
 * This makes it easy to show/hide loading indicators when fetching data.
 */
class LoadingManager {
    companion object {
        /**
         * Shows the loading overlay with a custom message
         *
         * @param activity The activity where the loading overlay is shown
         * @param message Optional custom message to display (defaults to "Loading data...")
         */
        fun showLoading(activity: Activity, message: String = "Loading data...") {
            val loadingOverlay = activity.findViewById<View>(R.id.loadingOverlay)
            val tvLoadingMessage = activity.findViewById<TextView>(R.id.tvLoadingMessage)
            
            // Update the loading message if needed
            tvLoadingMessage?.text = message
            
            // Show the loading overlay
            loadingOverlay?.visibility = View.VISIBLE
        }
        
        /**
         * Hides the loading overlay
         *
         * @param activity The activity where the loading overlay should be hidden
         */
        fun hideLoading(activity: Activity) {
            val loadingOverlay = activity.findViewById<View>(R.id.loadingOverlay)
            loadingOverlay?.visibility = View.GONE
        }
    }
} 