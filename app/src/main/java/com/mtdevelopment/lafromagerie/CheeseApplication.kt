package com.mtdevelopment.lafromagerie

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class CheeseApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(level = Level.DEBUG)

            androidContext(this@CheeseApplication)

            // Place Koin Modules here !
            modules(

            )
        }
    }
}