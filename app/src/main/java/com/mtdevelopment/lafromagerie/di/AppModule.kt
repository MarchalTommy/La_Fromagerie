package com.mtdevelopment.lafromagerie.di

import android.app.Application
import android.location.Geocoder
import androidx.room.Room
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.mtdevelopment.admin.data.repository.FirebaseAdminRepositoryImpl
import com.mtdevelopment.admin.data.source.FirestoreAdminDatasource
import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.admin.domain.usecase.AddNewProductUseCase
import com.mtdevelopment.admin.domain.usecase.DeleteProductUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateProductUseCase
import com.mtdevelopment.admin.presentation.viewmodel.AdminViewModel
import com.mtdevelopment.cart.presentation.viewmodel.CartViewModel
import com.mtdevelopment.checkout.data.BuildConfig
import com.mtdevelopment.checkout.data.local.CheckoutDatastorePreferenceImpl
import com.mtdevelopment.checkout.data.remote.model.Constants.ADDRESS_API_BASE_URL_WITHOUT_HTTPS
import com.mtdevelopment.checkout.data.remote.model.Constants.OPEN_ROUTE_BASE_URL_WITHOUT_HTTPS
import com.mtdevelopment.checkout.data.remote.source.AddressApiDataSource
import com.mtdevelopment.checkout.data.remote.source.FirestoreDataSource
import com.mtdevelopment.checkout.data.remote.source.OpenRouteDataSource
import com.mtdevelopment.checkout.data.remote.source.SumUpDataSource
import com.mtdevelopment.checkout.data.remote.source.local.DeliveryDatabase
import com.mtdevelopment.checkout.data.remote.source.local.dao.DeliveryDao
import com.mtdevelopment.checkout.data.repository.AddressApiRepositoryImpl
import com.mtdevelopment.checkout.data.repository.FirestorePathRepositoryImpl
import com.mtdevelopment.checkout.data.repository.PaymentRepositoryImpl
import com.mtdevelopment.checkout.data.repository.RoomDeliveryRepositoryImpl
import com.mtdevelopment.checkout.domain.repository.AddressApiRepository
import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference
import com.mtdevelopment.checkout.domain.repository.FirestorePathRepository
import com.mtdevelopment.checkout.domain.repository.PaymentRepository
import com.mtdevelopment.checkout.domain.repository.RoomDeliveryRepository
import com.mtdevelopment.checkout.domain.usecase.CreateNewCheckoutUseCase
import com.mtdevelopment.checkout.domain.usecase.CreatePaymentsClientUseCase
import com.mtdevelopment.checkout.domain.usecase.FetchAllowedPaymentMethods
import com.mtdevelopment.checkout.domain.usecase.GetAllDeliveryPathsUseCase
import com.mtdevelopment.checkout.domain.usecase.GetCanUseGooglePayUseCase
import com.mtdevelopment.checkout.domain.usecase.GetCheckoutDataUseCase
import com.mtdevelopment.checkout.domain.usecase.GetDeliveryPathUseCase
import com.mtdevelopment.checkout.domain.usecase.GetIsReadyToPayUseCase
import com.mtdevelopment.checkout.domain.usecase.GetPaymentDataRequestUseCase
import com.mtdevelopment.checkout.domain.usecase.GetUserInfoFromDatastoreUseCase
import com.mtdevelopment.checkout.domain.usecase.ProcessSumUpCheckoutUseCase
import com.mtdevelopment.checkout.domain.usecase.SaveCheckoutReferenceUseCase
import com.mtdevelopment.checkout.presentation.viewmodel.CheckoutViewModel
import com.mtdevelopment.checkout.presentation.viewmodel.DeliveryViewModel
import com.mtdevelopment.core.local.SharedDatastoreImpl
import com.mtdevelopment.core.presentation.MainViewModel
import com.mtdevelopment.core.repository.NetworkRepository
import com.mtdevelopment.core.repository.NetworkRepositoryImpl
import com.mtdevelopment.core.repository.SharedDatastore
import com.mtdevelopment.core.usecase.ClearDatastoreUseCase
import com.mtdevelopment.core.usecase.ClearOrderUseCase
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import com.mtdevelopment.core.usecase.SaveToDatastoreUseCase
import com.mtdevelopment.home.data.repository.FirebaseHomeRepositoryImpl
import com.mtdevelopment.home.data.repository.RoomHomeRepositoryImpl
import com.mtdevelopment.home.data.source.local.HomeDatabase
import com.mtdevelopment.home.data.source.local.dao.HomeDao
import com.mtdevelopment.home.data.source.remote.FirestoreDatabase
import com.mtdevelopment.home.domain.repository.FirebaseHomeRepository
import com.mtdevelopment.home.domain.repository.RoomHomeRepository
import com.mtdevelopment.home.domain.usecase.GetAllCheesesUseCase
import com.mtdevelopment.home.domain.usecase.GetAllProductsUseCase
import com.mtdevelopment.home.domain.usecase.GetLastFirestoreDatabaseUpdateUseCase
import com.mtdevelopment.home.presentation.viewmodel.HomeViewModel
import com.mtdevelopment.lafromagerie.FromagerieDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
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
    mainAppModule,
    provideJson,
    provideDatastore,
    provideHttpClientModule,
    provideFirebaseDatabase,
    provideRoomFromagerieDatabase,
    provideGeocoder,
    provideOpenRouteDatasource,
    provideAddressApiDataSource
)

val mainAppModule = module {
    single { SumUpDataSource(get()) }
    single<NetworkRepository> { NetworkRepositoryImpl(get()) }
    single<PaymentRepository> { PaymentRepositoryImpl(get(), get()) }
    single<AddressApiRepository> { AddressApiRepositoryImpl(get()) }

    single<FirebaseHomeRepository> { FirebaseHomeRepositoryImpl(get()) }
    single<RoomHomeRepository> { RoomHomeRepositoryImpl(get()) }
    single<RoomDeliveryRepository> { RoomDeliveryRepositoryImpl(get()) }

    single<FirebaseAdminRepository> { FirebaseAdminRepositoryImpl(get()) }

    single<FirestorePathRepository> { FirestorePathRepositoryImpl(get(), get(), get()) }

    factory { GetCheckoutDataUseCase(get()) }
    factory { SaveToDatastoreUseCase(get()) }
    factory { GetUserInfoFromDatastoreUseCase(get()) }
    factory { ClearDatastoreUseCase(get()) }
    factory { ClearOrderUseCase(get()) }

    factory { GetIsReadyToPayUseCase(get()) }
    factory { GetCanUseGooglePayUseCase(get()) }
    factory { FetchAllowedPaymentMethods(get()) }
    factory { CreatePaymentsClientUseCase(get()) }
    factory { GetIsNetworkConnectedUseCase(get()) }
    factory { GetPaymentDataRequestUseCase(get()) }

    factory { GetDeliveryPathUseCase(get()) }
    factory { GetAllDeliveryPathsUseCase(get(), get(), get()) }

    factory { CreateNewCheckoutUseCase(get()) }
    factory { ProcessSumUpCheckoutUseCase(get()) }
    factory { SaveCheckoutReferenceUseCase(get()) }

    factory { GetLastFirestoreDatabaseUpdateUseCase(get(), get()) }

    factory { GetAllProductsUseCase(get(), get(), get()) }
    factory { GetAllCheesesUseCase(get()) }
    factory { UpdateProductUseCase(get()) }
    factory { AddNewProductUseCase(get()) }
    factory { DeleteProductUseCase(get()) }

    factory { HomeDatabase(get()) }
    factory { DeliveryDatabase(get()) }

    viewModelOf(::CartViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::MainViewModel)
    viewModelOf(::DeliveryViewModel)
    viewModelOf(::CheckoutViewModel)
    single { AdminViewModel(get(), get(), get()) }
}

val provideHttpClientModule = module {
    single {
        // TODO: DETERMINE IF MANUAL HEADER IS NEEDED
        HttpClient(CIO) {
            install(DefaultRequest) {
                url {
                    protocol = URLProtocol.HTTPS
                    host = OPEN_ROUTE_BASE_URL_WITHOUT_HTTPS
                }
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        BearerTokens(BuildConfig.OPEN_ROUTE_TOKEN, null)
                    }
                }
            }
            install(Logging) {
                logger = Logger.ANDROID
                level = LogLevel.ALL
                sanitizeHeader { header -> header == HttpHeaders.Authorization }
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

val provideJson = module {
    single<Json> {
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}

val provideDatastore = module {
    single<CheckoutDatastorePreference> { CheckoutDatastorePreferenceImpl(get()) }
    single<SharedDatastore> { SharedDatastoreImpl(get()) }
}

val provideFirebaseDatabase = module {
    single<FirebaseFirestore> { Firebase.firestore }
    single<FirestoreDatabase> { FirestoreDatabase(get()) }
    single<FirestoreAdminDatasource> { FirestoreAdminDatasource(get()) }
    single<FirestoreDataSource> { FirestoreDataSource(get()) }
}

val provideOpenRouteDatasource = module {
    val client = HttpClient(CIO) {
        install(DefaultRequest) {
            url {
                protocol = URLProtocol.HTTPS
                host = OPEN_ROUTE_BASE_URL_WITHOUT_HTTPS
            }
        }
        install(Auth) {
            bearer {
                loadTokens {
                    BearerTokens(BuildConfig.OPEN_ROUTE_TOKEN, null)
                }
            }
        }
        install(Logging) {
            logger = Logger.ANDROID
            level = LogLevel.ALL
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
    }
    single<OpenRouteDataSource> { OpenRouteDataSource(client, get()) }
}

val provideAddressApiDataSource = module {
    val client = HttpClient(CIO) {
        install(DefaultRequest) {
            url {
                protocol = URLProtocol.HTTPS
                host = ADDRESS_API_BASE_URL_WITHOUT_HTTPS
            }
        }
        install(Logging) {
            logger = Logger.ANDROID
            level = LogLevel.ALL
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
    }
    single<AddressApiDataSource> { AddressApiDataSource(client, get()) }
}

val provideGeocoder = module {
    single { Geocoder(get()) }
}

val provideRoomFromagerieDatabase = module {
    single { provideDataBase(get()) }
    single { provideHomeDao(get()) }
    single { provideDeliveryDao(get()) }
}

fun provideHomeDao(db: FromagerieDatabase): HomeDao = db.homeDao
fun provideDeliveryDao(db: FromagerieDatabase): DeliveryDao = db.deliveryDao
fun provideDataBase(application: Application): FromagerieDatabase =
    Room.databaseBuilder(
        application,
        FromagerieDatabase::class.java,
        "lafromagerie_database"
    ).fallbackToDestructiveMigration().build()