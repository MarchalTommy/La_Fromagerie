package com.mtdevelopment.lafromagerie.di

import com.mtdevelopment.admin.data.di.adminDataModule
import com.mtdevelopment.admin.domain.di.adminDomainModule
import com.mtdevelopment.admin.presentation.di.adminPresentationModule
import com.mtdevelopment.lafromagerie.DeliveryTrackingService
import org.koin.dsl.module

fun flavorModules() =
    adminDataModule() + adminDomainModule() + adminPresentationModule() + mainModule

val mainModule = module {
    single<DeliveryTrackingService> {
        DeliveryTrackingService()
    }
}