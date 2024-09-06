package com.mtdevelopment.lafromagerie.di

import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

class UserModule {
//    module {
//        single { UserRepository() }
//        viewModel { UserViewModel(get()) }
//    }
}

val module = module {
    viewModel { com.mtdevelopment.cart.presentation.viewmodel.CartViewModel() }
}