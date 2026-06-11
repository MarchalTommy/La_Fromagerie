package com.mtdevelopment.admin.presentation.di

import com.mtdevelopment.admin.presentation.viewmodel.AdminViewModel
import org.koin.dsl.module

fun adminPresentationModule() = listOf(adminPresentationModule)

val adminPresentationModule = module {
    // Must NOT be a single: instances are stored in each screen's ViewModelStore.
    // A singleton would be cleared (viewModelScope cancelled) when the first screen
    // is popped, then reused dead on the next navigation (infinite loaders).
    factory {
        AdminViewModel(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
}