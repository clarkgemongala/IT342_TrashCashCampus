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

class SplashActivity : AppCompatActivity() {
    private lateinit var tvWelcome: TextView
    private lateinit var ivLogo: ImageView
    private lateinit var splashContainer: ConstraintLayout
    private lateinit var tvTagline: TextView
    private var isAnimationComplete = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Initialize views
        tvWelcome = findViewById(R.id.tvWelcome)
        ivLogo = findViewById(R.id.ivLogo)
        splashContainer = findViewById(R.id.splashContainer)
        tvTagline = findViewById(R.id.tvTagline)
        
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
        
        // Use a longer delay to ensure animations complete
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) {
                navigateToMainActivity()
            }
        }, 5000) // Increased to 5 seconds to ensure all animations complete
    }
    
    private fun startAnimations() {
        // Create welcome text animation
        val welcomeFadeIn = ObjectAnimator.ofFloat(tvWelcome, "alpha", 0f, 1f).apply {
            duration = 1000
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        val welcomeSlideUp = ObjectAnimator.ofFloat(tvWelcome, "translationY", 50f, 0f).apply {
            duration = 1000
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // Create logo animations
        val logoFadeIn = ObjectAnimator.ofFloat(ivLogo, "alpha", 0f, 1f).apply {
            duration = 800
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        val logoScale = ObjectAnimator.ofFloat(ivLogo, "scaleX", 0.5f, 1f).apply {
            duration = 1000
            interpolator = OvershootInterpolator(1.5f)
        }
        
        val logoScaleY = ObjectAnimator.ofFloat(ivLogo, "scaleY", 0.5f, 1f).apply {
            duration = 1000
            interpolator = OvershootInterpolator(1.5f)
        }
        
        // Create tagline animations - reduced start delay
        val taglineFadeIn = ObjectAnimator.ofFloat(tvTagline, "alpha", 0f, 1f).apply {
            duration = 800
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        val taglineSlideUp = ObjectAnimator.ofFloat(tvTagline, "translationY", 30f, 0f).apply {
            duration = 800
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // Create the background color animation
        val backgroundColorAnimation = ObjectAnimator.ofArgb(
            splashContainer,
            "backgroundColor", 
            resources.getColor(R.color.splash_start_color, theme),
            resources.getColor(R.color.splash_end_color, theme)
        ).apply {
            duration = 2000
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // First AnimatorSet - Welcome and background
        val welcomeSet = AnimatorSet().apply {
            playTogether(welcomeFadeIn, welcomeSlideUp, backgroundColorAnimation)
            duration = 1200
        }
        
        // Second AnimatorSet - Logo animations
        val logoSet = AnimatorSet().apply {
            playTogether(logoFadeIn, logoScale, logoScaleY)
            duration = 1200
            startDelay = 300
        }
        
        // Third AnimatorSet - Tagline animations
        val taglineSet = AnimatorSet().apply {
            playTogether(taglineFadeIn, taglineSlideUp)
            duration = 1000
            startDelay = 300
        }
        
        // Main AnimatorSet to chain all animations
        val mainSet = AnimatorSet()
        mainSet.playSequentially(welcomeSet, logoSet, taglineSet)
        
        // Add a listener to log when animations complete
        mainSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                isAnimationComplete = true
                Log.d("SplashActivity", "All animations completed")
                Log.d("SplashActivity", "Tagline final state - visibility: ${tvTagline.visibility}, alpha: ${tvTagline.alpha}")
            }
        })
        
        mainSet.start()
    }
    
    private fun navigateToMainActivity() {
        // Log transition
        Log.d("SplashActivity", "Navigating to MainActivity")
        Log.d("SplashActivity", "Tagline final visibility: ${tvTagline.visibility}, alpha: ${tvTagline.alpha}")
        
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        
        // Add a custom transition animation
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        
        // Close this activity so it's removed from the back stack
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d("SplashActivity", "onDestroy called")
    }
} 