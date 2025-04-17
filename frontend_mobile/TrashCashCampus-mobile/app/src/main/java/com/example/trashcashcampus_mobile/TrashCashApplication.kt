package com.example.trashcashcampus_mobile

import android.app.Application
import com.google.firebase.FirebaseApp

class TrashCashApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(applicationContext)
    }
}