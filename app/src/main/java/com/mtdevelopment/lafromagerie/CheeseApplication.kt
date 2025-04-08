package com.mtdevelopment.lafromagerie

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.initialize
import com.mtdevelopment.lafromagerie.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class CheeseApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Firebase.initialize(this)

        startKoin {
            androidLogger(level = Level.DEBUG)

            androidContext(this@CheeseApplication)

            // Place Koin Modules here !
            modules(
                appModule()
            )
        }
    }
}

// TODO: List clients commands on admin dashboard to allow preparation with dates (a list of cheese with the quantity and a way to know like "5 for this command, 3 for this one" etc... filtered by dates sticky header)
// TODO: A way for the admin to "limit" cheese stock