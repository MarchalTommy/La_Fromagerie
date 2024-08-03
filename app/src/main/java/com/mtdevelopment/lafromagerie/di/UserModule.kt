package com.mtdevelopment.lafromagerie.di

import com.mtdevelopment.home.presentation.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

class UserModule {
//    module {
//        single { UserRepository() }
//        viewModel { UserViewModel(get()) }
//    }
}

val module = module {
    viewModel { MainViewModel() }
}