package com.mtdevelopment.admin.presentation.di

import com.mtdevelopment.admin.presentation.viewmodel.AdminViewModel
import org.koin.dsl.module

fun adminPresentationModule() = listOf(adminPresentationModule)

val adminPresentationModule = module {
    single { AdminViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
}