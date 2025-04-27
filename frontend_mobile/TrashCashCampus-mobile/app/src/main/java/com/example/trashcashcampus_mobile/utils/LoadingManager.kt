package com.example.trashcashcampus_mobile.utils

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.trashcashcampus_mobile.R

/**
 * Utility for managing loading states across the app
 */
object LoadingManager {
    private var loadingDialog: Dialog? = null
    
    /**
     * Shows a loading dialog with an optional message
     */
    fun showLoading(activity: Activity, message: String = "Loading...") {
        // If there's already a dialog showing, dismiss it first
        hideLoading(activity)
        
        try {
            // Create a new dialog
            loadingDialog = Dialog(activity)
            loadingDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
            loadingDialog?.setCancelable(false)
            
            // Create a linear layout for dialog content
            val layout = LinearLayout(activity)
            layout.orientation = LinearLayout.VERTICAL
            layout.gravity = Gravity.CENTER
            layout.setPadding(30, 30, 30, 30)
            
            // Add a progress bar
            val progressBar = ProgressBar(activity)
            val progressBarParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            progressBarParams.gravity = Gravity.CENTER
            layout.addView(progressBar, progressBarParams)
            
            // Add a message text
            val textView = TextView(activity)
            textView.text = message
            textView.setPadding(0, 20, 0, 0)
            textView.gravity = Gravity.CENTER
            textView.setTextColor(activity.resources.getColor(R.color.text_primary, null))
            layout.addView(textView)
            
            // Set dialog content
            loadingDialog?.setContentView(layout)
            
            // Set transparent background
            loadingDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            
            // Show the dialog
            loadingDialog?.show()
        } catch (e: Exception) {
            // Log any errors but don't crash
            e.printStackTrace()
        }
    }
    
    /**
     * Hides the currently showing loading dialog
     */
    fun hideLoading(activity: Activity) {
        try {
            if (loadingDialog != null && loadingDialog?.isShowing == true) {
                // Only dismiss if the activity is not finishing/destroyed
                if (!activity.isFinishing && !activity.isDestroyed) {
                    loadingDialog?.dismiss()
                }
            }
            loadingDialog = null
        } catch (e: Exception) {
            // Log any errors but don't crash
            e.printStackTrace()
        }
    }
} 