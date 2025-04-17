package com.mtdevelopment.admin.data.di

import com.mtdevelopment.admin.data.repository.CloudinaryRepositoryImpl
import com.mtdevelopment.admin.data.repository.FirebaseAdminRepositoryImpl
import com.mtdevelopment.admin.data.source.FirestoreAdminDatasource
import com.mtdevelopment.admin.domain.repository.CloudinaryRepository
import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import org.koin.dsl.module

fun adminDataModule() = listOf(adminDataModule)

val adminDataModule = module {
    single<FirestoreAdminDatasource> { FirestoreAdminDatasource(get()) }

    single<FirebaseAdminRepository> { FirebaseAdminRepositoryImpl(get()) }
    single<CloudinaryRepository> { CloudinaryRepositoryImpl(get()) }
}