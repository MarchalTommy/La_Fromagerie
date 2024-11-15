package com.mtdevelopment.lafromagerie.di

import android.app.Application
import androidx.room.Room
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
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
import com.mtdevelopment.home.data.repository.FirebaseRepositoryImpl
import com.mtdevelopment.home.data.repository.RoomRepositoryImpl
import com.mtdevelopment.home.data.source.local.HomeDatabaseDatasource
import com.mtdevelopment.home.data.source.local.dao.HomeDao
import com.mtdevelopment.home.data.source.remote.FirestoreDatabase
import com.mtdevelopment.home.domain.repository.FirebaseRepository
import com.mtdevelopment.home.domain.repository.RoomRepository
import com.mtdevelopment.home.domain.usecase.AddNewProductUseCase
import com.mtdevelopment.home.domain.usecase.DeleteProductUseCase
import com.mtdevelopment.home.domain.usecase.GetAllCheesesUseCase
import com.mtdevelopment.home.domain.usecase.GetAllProductsUseCase
import com.mtdevelopment.home.domain.usecase.GetLastDatabaseUpdateUseCase
import com.mtdevelopment.home.domain.usecase.UpdateProductUseCase
import com.mtdevelopment.home.presentation.viewmodel.HomeViewModel
import com.mtdevelopment.lafromagerie.FromagerieDatabase
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
    mainAppModule,
    provideDatastore,
    provideHttpClientModule,
    provideFirebaseDatabase,
    provideRoomFromagerieDatabase
)

val mainAppModule = module {
    single { SumUpDataSource(get()) }
    single<NetworkRepository> { NetworkRepositoryImpl(get()) }
    single<PaymentRepository> { PaymentRepositoryImpl(get(), get()) }

    single<FirebaseRepository> { FirebaseRepositoryImpl(get()) }
    single<RoomRepository> { RoomRepositoryImpl(get()) }

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

    factory { CreateNewCheckoutUseCase(get()) }
    factory { ProcessSumUpCheckoutUseCase(get()) }
    factory { SaveCheckoutReferenceUseCase(get()) }

    factory { GetAllProductsUseCase(get(), get(), get()) }
    factory { GetLastDatabaseUpdateUseCase(get()) }
    factory { GetAllCheesesUseCase(get()) }
    factory { UpdateProductUseCase(get()) }
    factory { AddNewProductUseCase(get()) }
    factory { DeleteProductUseCase(get()) }

    factory { HomeDatabaseDatasource(get()) }

    viewModelOf(::CartViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::MainViewModel)
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

val provideFirebaseDatabase = module {
    single<FirebaseFirestore> { Firebase.firestore }
    single<FirestoreDatabase> { FirestoreDatabase(get()) }
}

val provideRoomFromagerieDatabase = module {
    single { provideDataBase(get()) }
    single { provideDao(get()) }
}

fun provideDao(db: FromagerieDatabase): HomeDao = db.dao
fun provideDataBase(application: Application): FromagerieDatabase =
    Room.databaseBuilder(
        application,
        FromagerieDatabase::class.java,
        "table_post"
    ).fallbackToDestructiveMigration().build()