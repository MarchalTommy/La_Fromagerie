package com.mtdevelopment.admin.data.di

import com.google.common.net.HttpHeaders
import com.mtdevelopment.admin.data.repository.CloudinaryRepositoryImpl
import com.mtdevelopment.admin.data.repository.FirebaseAdminRepositoryImpl
import com.mtdevelopment.admin.data.repository.GoogleRouteRepositoryImpl
import com.mtdevelopment.admin.data.repository.LocationRepositoryImpl
import com.mtdevelopment.admin.data.source.FirestoreAdminDatasource
import com.mtdevelopment.admin.data.source.GoogleRouteDataSource
import com.mtdevelopment.admin.domain.repository.CloudinaryRepository
import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.admin.domain.repository.GoogleRouteRepository
import com.mtdevelopment.admin.domain.repository.LocationRepository
import com.mtdevelopment.core.data.Constants.GOOGLE_ROUTE_BASE_URL_WITHOUT_HTTPS
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

fun adminDataModule() = listOf(
    provideGoogleRouteDatasource,
    adminDataModule
)

val adminDataModule = module {
    single<FirestoreAdminDatasource> { FirestoreAdminDatasource(get()) }

    single<FirebaseAdminRepository> { FirebaseAdminRepositoryImpl(get()) }
    single<CloudinaryRepository> { CloudinaryRepositoryImpl(get()) }
    single<GoogleRouteRepository> { GoogleRouteRepositoryImpl(get()) }
    single<LocationRepository> { LocationRepositoryImpl(get()) }
}

val provideGoogleRouteDatasource = module {
    val client = HttpClient(CIO) {
        install(DefaultRequest) {
            url {
                protocol = URLProtocol.HTTPS
                host = GOOGLE_ROUTE_BASE_URL_WITHOUT_HTTPS
            }
        }
        install(Logging) {
            logger = Logger.ANDROID
            level = LogLevel.ALL
            sanitizeHeader { header -> header == HttpHeaders.AUTHORIZATION }
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }
    single<GoogleRouteDataSource> {
        GoogleRouteDataSource(
            client,
            get()
        )
    }
}