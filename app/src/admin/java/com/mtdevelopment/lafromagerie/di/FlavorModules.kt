package com.mtdevelopment.lafromagerie.di

import com.mtdevelopment.admin.data.di.adminDataModule
import com.mtdevelopment.admin.domain.di.adminDomainModule
import com.mtdevelopment.admin.presentation.di.adminPresentationModule
import com.mtdevelopment.auth.di.authModule
import com.mtdevelopment.checkout.domain.repository.OrderReminderScheduler
import com.mtdevelopment.lafromagerie.DeliveryTrackingService
import com.mtdevelopment.lafromagerie.NoOpOrderReminderScheduler
import org.koin.dsl.module

fun flavorModules() =
    adminDataModule() + adminDomainModule() + adminPresentationModule() + authModule() + mainModule

val mainModule = module {
    single<DeliveryTrackingService> {
        DeliveryTrackingService()
    }

    // The admin flavor also reaches checkout, so this definition must exist here too:
    // Koin would otherwise crash at runtime rather than fail the build.
    single<OrderReminderScheduler> { NoOpOrderReminderScheduler() }
}