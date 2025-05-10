package com.example.trashcashcampus_mobile

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.FirebaseApp

class SplashActivity : AppCompatActivity() {
    private lateinit var tvWelcome: TextView
    private lateinit var ivLogo: ImageView
    private lateinit var splashContainer: ConstraintLayout
    private lateinit var tvTagline: TextView
    private var isAnimationComplete = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_splash)
            
            // Initialize Firebase (and other components) safely
            try {
                FirebaseApp.initializeApp(this)
            } catch (e: Exception) {
                // Log but don't crash if Firebase initialization fails
                Log.e("SplashActivity", "Firebase initialization error: ${e.message}", e)
            }
            
            // Initialize views
            tvWelcome = findViewById(R.id.tvWelcome)
            ivLogo = findViewById(R.id.ivLogo)
            splashContainer = findViewById(R.id.splashContainer)
            tvTagline = findViewById(R.id.tvTagline)

            // Load image with downsampling to prevent OOM errors
            try {
                // Get the ImageView dimensions
                ivLogo.post {
                    try {
                        val width = ivLogo.width
                        val height = ivLogo.height
                        
                        if (width > 0 && height > 0) {
                            Log.d("SplashActivity", "Loading optimized logo image at size ${width}x${height}")
                            
                            // Use the application's utility function
                            val bitmap = TrashCashApplication.decodeSampledBitmapFromResource(
                                this@SplashActivity,
                                R.drawable.trashcash_logo,
                                width,
                                height
                            )
                            
                            if (bitmap != null) {
                                ivLogo.setImageBitmap(bitmap)
                            } else {
                                // Fallback to the vectorized icon
                                ivLogo.setImageResource(R.drawable.ic_launcher_foreground)
                            }
                        } else {
                            Log.e("SplashActivity", "ImageView size is invalid: ${width}x${height}")
                            // Fallback to the vectorized icon
                            ivLogo.setImageResource(R.drawable.ic_launcher_foreground)
                        }
                    } catch (e: Exception) {
                        Log.e("SplashActivity", "Error in image loading: ${e.message}", e)
                        // Fallback to the vectorized icon
                        ivLogo.setImageResource(R.drawable.ic_launcher_foreground)
                    }
                }
            } catch (e: Exception) {
                Log.e("SplashActivity", "Error setting up image loading: ${e.message}", e)
                // If image loading fails, continue with default
                ivLogo.setImageResource(R.drawable.ic_launcher_foreground)
            }

            // Set initial visibility - ensure all are VISIBLE with alpha 0
            tvWelcome.alpha = 0f
            tvWelcome.visibility = View.VISIBLE

            ivLogo.alpha = 0f
            ivLogo.visibility = View.VISIBLE

            tvTagline.alpha = 0f
            tvTagline.visibility = View.VISIBLE

            // Log view states for debugging
            Log.d("SplashActivity", "tvWelcome visibility: ${tvWelcome.visibility}, alpha: ${tvWelcome.alpha}")
            Log.d("SplashActivity", "ivLogo visibility: ${ivLogo.visibility}, alpha: ${ivLogo.alpha}")
            Log.d("SplashActivity", "tvTagline visibility: ${tvTagline.visibility}, alpha: ${tvTagline.alpha}")

            // Start splash animation sequence with callback when complete
            startAnimations()

            // Navigate to main activity after delay - increase from 2000ms to 3000ms (3 seconds)
            // Also make sure we only navigate when animations are complete
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    // Only navigate if our animations have completed or 3 seconds have passed
                    if (isAnimationComplete) {
                        Log.d("SplashActivity", "Animations complete, navigating to MainActivity")
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // Animations aren't complete yet, wait a bit more
                        Log.d("SplashActivity", "Animations not complete yet, waiting additional time")
                        Handler(Looper.getMainLooper()).postDelayed({
                            Log.d("SplashActivity", "Additional delay complete, navigating to MainActivity")
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }, 1000) // Wait one more second
                    }
                } catch (e: Exception) {
                    Log.e("SplashActivity", "Navigation error: ${e.message}", e)
                    // If navigation fails, try to finish this activity at least
                    try {
                        finish()
                    } catch (ex: Exception) {
                        // Last resort - nothing more we can do
                    }
                }
            }, 3000) // 3 seconds delay
        } catch (e: Exception) {
            // Catch any unexpected errors in the entire onCreate
            Log.e("SplashActivity", "Critical error in onCreate: ${e.message}", e)
            try {
                // Try to navigate to MainActivity anyway
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } catch (ex: Exception) {
                // If that fails too, just try to finish
                try {
                    finish()
                } catch (finalEx: Exception) {
                    // Nothing more we can do
                }
            }
        }
    }

    private fun startAnimations() {
        try {
            Log.d("SplashActivity", "Starting animations")
            // Create welcome text animation
            val welcomeFadeIn = ObjectAnimator.ofFloat(tvWelcome, "alpha", 0f, 1f).apply {
                duration = 800
                interpolator = AccelerateDecelerateInterpolator()
            }

            val welcomeSlideUp = ObjectAnimator.ofFloat(tvWelcome, "translationY", 50f, 0f).apply {
                duration = 800
                interpolator = AccelerateDecelerateInterpolator()
            }

            // Create logo animations
            val logoFadeIn = ObjectAnimator.ofFloat(ivLogo, "alpha", 0f, 1f).apply {
                duration = 800
                interpolator = AccelerateDecelerateInterpolator()
            }

            val logoScale = ObjectAnimator.ofFloat(ivLogo, "scaleX", 0.5f, 1f).apply {
                duration = 800
                interpolator = OvershootInterpolator(1.5f)
            }

            val logoScaleY = ObjectAnimator.ofFloat(ivLogo, "scaleY", 0.5f, 1f).apply {
                duration = 800
                interpolator = OvershootInterpolator(1.5f)
            }

            // Create tagline animations - increased visibility and duration
            val taglineFadeIn = ObjectAnimator.ofFloat(tvTagline, "alpha", 0f, 1f).apply {
                duration = 1000
                interpolator = AccelerateDecelerateInterpolator()
            }

            val taglineSlideUp = ObjectAnimator.ofFloat(tvTagline, "translationY", 30f, 0f).apply {
                duration = 1000
                interpolator = AccelerateDecelerateInterpolator()
            }

            // Create the background color animation
            val backgroundColorAnimation = ObjectAnimator.ofArgb(
                splashContainer,
                "backgroundColor",
                resources.getColor(R.color.splash_start_color, theme),
                resources.getColor(R.color.splash_end_color, theme)
            ).apply {
                duration = 1500
                interpolator = AccelerateDecelerateInterpolator()
            }

            // First AnimatorSet - Welcome and background
            val welcomeSet = AnimatorSet().apply {
                playTogether(welcomeFadeIn, welcomeSlideUp, backgroundColorAnimation)
                duration = 1000
            }

            // Second AnimatorSet - Logo animations
            val logoSet = AnimatorSet().apply {
                playTogether(logoFadeIn, logoScale, logoScaleY)
                duration = 1000
                startDelay = 200
            }

            // Third AnimatorSet - Tagline animations
            val taglineSet = AnimatorSet().apply {
                playTogether(taglineFadeIn, taglineSlideUp)
                duration = 1000
                startDelay = 200
            }

            // Main AnimatorSet to chain all animations
            val mainSet = AnimatorSet()
            mainSet.playSequentially(welcomeSet, logoSet, taglineSet)

            // Add a listener to log when animations complete
            mainSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    try {
                        super.onAnimationEnd(animation)
                        isAnimationComplete = true
                        Log.d("SplashActivity", "All animations completed")
                        Log.d("SplashActivity", "Tagline final state - visibility: ${tvTagline.visibility}, alpha: ${tvTagline.alpha}")
                    } catch (e: Exception) {
                        Log.e("SplashActivity", "Error in animation end listener: ${e.message}", e)
                    }
                }
            })

            // Start animation in try-catch for safety
            try {
                mainSet.start()
                Log.d("SplashActivity", "Animation started successfully")
            } catch (e: Exception) {
                Log.e("SplashActivity", "Error starting animation: ${e.message}", e)
            }
        } catch (e: Exception) {
            Log.e("SplashActivity", "Error setting up animations: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SplashActivity", "onDestroy called")
    }
} 