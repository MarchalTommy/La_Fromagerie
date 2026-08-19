package com.mtdevelopment.lafromagerie.di

import com.mtdevelopment.checkout.domain.repository.OrderReminderScheduler
import com.mtdevelopment.lafromagerie.notifications.NotificationLocalStore
import com.mtdevelopment.lafromagerie.notifications.NotificationViewModel
import com.mtdevelopment.lafromagerie.notifications.WorkManagerOrderReminderScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

fun flavorModules() = listOf(flavorModules)

val flavorModules = module {
    // single: two DataStore instances on the same file crash at runtime, and the store is
    // shared between ClientMessagingService, OrderReminderWorker and NotificationViewModel.
    single { NotificationLocalStore(androidContext(), get()) }
    viewModelOf(::NotificationViewModel)

    // Local order reminders. The admin flavor binds a no-op counterpart — both flavors
    // must provide one, since Koin only discovers a missing definition at runtime.
    single<OrderReminderScheduler> { WorkManagerOrderReminderScheduler(androidContext()) }
}
