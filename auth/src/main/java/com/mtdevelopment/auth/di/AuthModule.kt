package com.mtdevelopment.auth.di

import com.google.firebase.auth.FirebaseAuth
import com.mtdevelopment.auth.data.repository.AuthRepositoryImpl
import com.mtdevelopment.auth.domain.repository.AuthRepository
import com.mtdevelopment.auth.domain.usecase.ObserveAuthStateUseCase
import com.mtdevelopment.auth.domain.usecase.SignInWithPinUseCase
import com.mtdevelopment.auth.presentation.viewmodel.AuthViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin definitions for the admin gate. Wired into the ADMIN flavor only (see the app's
 * admin `flavorModules()`), so the client flavor never resolves these.
 */
fun authModule() = module {
    single<FirebaseAuth> { FirebaseAuth.getInstance() }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    factory { SignInWithPinUseCase(get()) }
    factory { ObserveAuthStateUseCase(get()) }
    viewModelOf(::AuthViewModel)
}
