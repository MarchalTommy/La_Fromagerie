package com.mtdevelopment.lafromagerie

import android.app.Application
import android.util.Log
import com.cloudinary.android.MediaManager
import com.google.firebase.Firebase
import com.google.firebase.initialize
import com.mtdevelopment.lafromagerie.di.appModule
import com.mtdevelopment.lafromagerie.di.flavorModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Main application class for LaFromagerie.
 * Handles the global initialization of critical services:
 * 1. Firebase: Used for data storage (Firestore) and analytics/crashlytics.
 * 2. Cloudinary: Used for product image hosting and upload.
 * 3. Koin: Dependency injection framework used across all modules.
 */
// TODO: On delivery for Clients variant, when delivery and billing addresses are different, autocomplete on billing changes the delivery.
// TODO: On delivery for Clients variant, when keyboard is up, content padding seems too big and a white bar appears on top of the keyboard.
// TODO: On delivery for Clients variant, even when the delivery address is in a city which is part of a delivery path, I feel like we always show the "too far" message.
class CheeseApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Firebase
        Firebase.initialize(this)

        // 2. Initialize Cloudinary MediaManager
        try {
            val config = mapOf("cloud_name" to "dzgaywpmz")
            MediaManager.init(this, config)
            Log.i("YourApplication", "Cloudinary initialized successfully.")
        } catch (e: Exception) {
            Log.e("YourApplication", "Failed to initialize Cloudinary", e)
        }

        // 3. Initialize Koin Dependency Injection
        startKoin {
            androidLogger(level = Level.DEBUG)

            androidContext(this@CheeseApplication)

            // Loads common appModule and flavor-specific modules (Admin/Client)
            modules(
                appModule() + flavorModules()
            )
        }
    }
}
