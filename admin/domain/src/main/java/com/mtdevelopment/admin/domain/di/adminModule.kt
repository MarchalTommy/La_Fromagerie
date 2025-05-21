package com.mtdevelopment.admin.domain.di

import com.mtdevelopment.admin.domain.usecase.AddNewPathUseCase
import com.mtdevelopment.admin.domain.usecase.AddNewProductUseCase
import com.mtdevelopment.admin.domain.usecase.DeletePathUseCase
import com.mtdevelopment.admin.domain.usecase.DeleteProductUseCase
import com.mtdevelopment.admin.domain.usecase.GetAllOrdersUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateDeliveryPathUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateProductUseCase
import com.mtdevelopment.admin.domain.usecase.UploadImageUseCase
import org.koin.dsl.module

fun adminDomainModule() = listOf(adminDomainModule)

val adminDomainModule = module {
    factory { UpdateProductUseCase(get()) }
    factory { UploadImageUseCase(get()) }
    factory { AddNewProductUseCase(get()) }
    factory { DeleteProductUseCase(get()) }

    factory { UpdateDeliveryPathUseCase(get()) }
    factory { DeletePathUseCase(get()) }
    factory { AddNewPathUseCase(get()) }

    factory { GetAllOrdersUseCase(get()) }
}