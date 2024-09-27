package com.mtdevelopment.lafromagerie.di

import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.checkout.presentation.viewmodel.CheckoutViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val module = module {
    viewModel { CartViewModel() }
    viewModel { CheckoutViewModel() }
}