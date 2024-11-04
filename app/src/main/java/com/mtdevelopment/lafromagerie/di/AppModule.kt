package com.mtdevelopment.lafromagerie.di

import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.checkout.data.local.CheckoutDatastorePreferenceImpl
import com.mtdevelopment.checkout.data.remote.model.Constants
import com.mtdevelopment.checkout.data.remote.source.SumUpDataSource
import com.mtdevelopment.checkout.data.repository.PaymentRepositoryImpl
import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference
import com.mtdevelopment.checkout.domain.repository.PaymentRepository
import com.mtdevelopment.checkout.domain.usecase.CreateNewCheckoutUseCase
import com.mtdevelopment.checkout.domain.usecase.CreatePaymentsClientUseCase
import com.mtdevelopment.checkout.domain.usecase.FetchAllowedPaymentMethods
import com.mtdevelopment.checkout.domain.usecase.GetCanUseGooglePayUseCase
import com.mtdevelopment.checkout.domain.usecase.GetCheckoutDataUseCase
import com.mtdevelopment.checkout.domain.usecase.GetIsReadyToPayUseCase
import com.mtdevelopment.checkout.domain.usecase.GetPaymentDataRequestUseCase
import com.mtdevelopment.checkout.domain.usecase.ProcessSumUpCheckoutUseCase
import com.mtdevelopment.checkout.domain.usecase.SaveCheckoutReferenceUseCase
import com.mtdevelopment.checkout.presentation.viewmodel.CheckoutViewModel
import com.mtdevelopment.checkout.presentation.viewmodel.DeliveryViewModel
import com.mtdevelopment.core.local.SharedDatastoreImpl
import com.mtdevelopment.core.repository.NetworkRepository
import com.mtdevelopment.core.repository.NetworkRepositoryImpl
import com.mtdevelopment.core.repository.SharedDatastore
import com.mtdevelopment.core.usecase.ClearDatastoreUseCase
import com.mtdevelopment.core.usecase.ClearOrderUseCase
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import com.mtdevelopment.core.usecase.SaveToDatastoreUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

fun appModule() = listOf(
    mainAppModule, provideDatastore, provideHttpClientModule
)

val mainAppModule = module {
    single { SumUpDataSource(get()) }
    single<NetworkRepository> { NetworkRepositoryImpl(get()) }
    single<PaymentRepository> { PaymentRepositoryImpl(get(), get()) }

    factory { GetCheckoutDataUseCase(get()) }
    factory { SaveToDatastoreUseCase(get()) }
    factory { ClearDatastoreUseCase(get()) }
    factory { ClearOrderUseCase(get()) }

    factory { GetIsReadyToPayUseCase(get()) }
    factory { GetCanUseGooglePayUseCase(get()) }
    factory { FetchAllowedPaymentMethods(get()) }
    factory { CreatePaymentsClientUseCase(get()) }
    factory { GetIsNetworkConnectedUseCase(get()) }
    factory { GetPaymentDataRequestUseCase(get()) }

    factory { CreateNewCheckoutUseCase(get()) }
    factory { ProcessSumUpCheckoutUseCase(get()) }
    factory { SaveCheckoutReferenceUseCase(get()) }

    viewModelOf(::CartViewModel)
    viewModelOf(::DeliveryViewModel)
    viewModelOf(::CheckoutViewModel)
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
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }
        }
    }
}

val provideDatastore = module {
    single<CheckoutDatastorePreference> { CheckoutDatastorePreferenceImpl(get()) }
    single<SharedDatastore> { SharedDatastoreImpl(get()) }
}