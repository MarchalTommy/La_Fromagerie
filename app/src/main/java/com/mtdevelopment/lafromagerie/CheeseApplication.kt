package com.mtdevelopment.lafromagerie

import android.app.Application
import android.util.Log
import com.cloudinary.android.MediaManager
import com.google.firebase.Firebase
import com.google.firebase.initialize
import com.mtdevelopment.checkout.domain.usecase.ResumePendingPaymentFinalizationUseCase
import com.mtdevelopment.lafromagerie.di.appModule
import com.mtdevelopment.lafromagerie.di.flavorModules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
class CheeseApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        val koinApp = startKoin {
            androidLogger(level = Level.DEBUG)

            androidContext(this@CheeseApplication)

            // Loads common appModule and flavor-specific modules (Admin/Client)
            modules(
                appModule() + flavorModules()
            )
        }

        // 4. If a payment was submitted but never finalized (app killed mid-processing),
        // re-enqueue the background work that reconciles the order status.
        applicationScope.launch {
            try {
                koinApp.koin.get<ResumePendingPaymentFinalizationUseCase>().invoke()
            } catch (e: Exception) {
                Log.e("CheeseApplication", "Failed to resume pending payment finalization", e)
            }
        }
    }
}
