package com.mtdevelopment.lafromagerie.di

import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.checkout.presentation.viewmodel.CheckoutViewModel
import com.mtdevelopment.core.repository.NetworkRepository
import com.mtdevelopment.core.repository.NetworkRepositoryImpl
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<NetworkRepository> { NetworkRepositoryImpl(get()) }

    factory { GetIsNetworkConnectedUseCase(get()) }

    viewModel { CartViewModel(get()) }
    viewModel { CheckoutViewModel(get()) }
}