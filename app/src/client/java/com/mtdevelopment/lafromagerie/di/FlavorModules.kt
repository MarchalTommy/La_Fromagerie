package com.mtdevelopment.lafromagerie.di

import com.mtdevelopment.lafromagerie.notifications.NotificationLocalStore
import com.mtdevelopment.lafromagerie.notifications.NotificationViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

fun flavorModules() = listOf(flavorModules)

val flavorModules = module {
    // single: two DataStore instances on the same file crash at runtime, and the store is
    // shared between ClientMessagingService and NotificationViewModel.
    single { NotificationLocalStore(androidContext(), get()) }
    viewModelOf(::NotificationViewModel)
}
