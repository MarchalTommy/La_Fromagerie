package com.mtdevelopment.lafromagerie.di

import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.checkout.data.local.CheckoutDatastorePreferenceImpl
import com.mtdevelopment.checkout.data.remote.model.Constants
import com.mtdevelopment.checkout.data.remote.source.SumUpDataSource
import com.mtdevelopment.checkout.data.repository.PaymentRepositoryImpl
import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference
import com.mtdevelopment.checkout.domain.repository.PaymentRepository
import com.mtdevelopment.checkout.domain.usecase.CreatePaymentClientUseCase
import com.mtdevelopment.checkout.domain.usecase.FetchAllowedPaymentMethods
import com.mtdevelopment.checkout.domain.usecase.FetchCanUseGooglePayUseCase
import com.mtdevelopment.checkout.domain.usecase.GetLoadPaymentDataTaskUseCase
import com.mtdevelopment.checkout.presentation.viewmodel.CheckoutViewModel
import com.mtdevelopment.checkout.presentation.viewmodel.DeliveryViewModel
import com.mtdevelopment.core.repository.NetworkRepository
import com.mtdevelopment.core.repository.NetworkRepositoryImpl
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

fun appModule() = listOf(
    mainAppModule, provideDatastore, provideHttpClientModule
)

val mainAppModule = module {
    single { SumUpDataSource(get()) }
    single<NetworkRepository> { NetworkRepositoryImpl(get()) }
    single<PaymentRepository> { PaymentRepositoryImpl(get(), get()) }

    factory { GetIsNetworkConnectedUseCase(get()) }
    factory { CreatePaymentClientUseCase(get()) }
    factory { FetchAllowedPaymentMethods(get()) }
    factory { FetchCanUseGooglePayUseCase(get()) }
    factory { GetLoadPaymentDataTaskUseCase(get()) }

    viewModel { CartViewModel(get()) }
    viewModel { DeliveryViewModel(get()) }
    viewModel {
        CheckoutViewModel(
            savedStateHandle = get(),
            createPaymentClientUseCase = get(),
            fetchCanUseGooglePayUseCase = get(),
            getLoadPaymentDataTaskUseCase = get(),
            fetchAllowedPaymentMethods = get(),
            getIsConnectedUseCase = get()
        )
    }
}

val provideHttpClientModule = module {
    single {
        // TODO: DETERMINE IF MANUAL HEADER IS NEEDED
        HttpClient(CIO) {
            install(Logging) {
                logger = Logger.ANDROID
                level = LogLevel.ALL
                sanitizeHeader { header -> header == HttpHeaders.Authorization }
            }
            install(DefaultRequest) {
                url {
                    protocol = URLProtocol.HTTPS
                    host = Constants.BASE_URL_WITHOUT_HTTPS
                }
            }
        }
    }
}

val provideDatastore = module {
    single<CheckoutDatastorePreference> { CheckoutDatastorePreferenceImpl(get()) }
}