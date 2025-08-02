package com.mtdevelopment.admin.domain.di

import com.mtdevelopment.admin.domain.usecase.AddNewPathUseCase
import com.mtdevelopment.admin.domain.usecase.AddNewProductUseCase
import com.mtdevelopment.admin.domain.usecase.DeletePathUseCase
import com.mtdevelopment.admin.domain.usecase.DeleteProductUseCase
import com.mtdevelopment.admin.domain.usecase.DetermineNextDeliveryStopUseCase
import com.mtdevelopment.admin.domain.usecase.GetAllOrdersUseCase
import com.mtdevelopment.admin.domain.usecase.GetCurrentLocationOnceUseCase
import com.mtdevelopment.admin.domain.usecase.GetCurrentLocationUseCase
import com.mtdevelopment.admin.domain.usecase.GetIsInTrackingModeUseCase
import com.mtdevelopment.admin.domain.usecase.GetOptimizedDeliveryUseCase
import com.mtdevelopment.admin.domain.usecase.GetShouldShowBatterieOptimizationUseCase
import com.mtdevelopment.admin.domain.usecase.SetIsInTrackingModeUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateDeliveryPathUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateProductUseCase
import com.mtdevelopment.admin.domain.usecase.UpdateShouldShowBatterieOptimizationUseCase
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

    factory { UpdateShouldShowBatterieOptimizationUseCase(get()) }
    factory { GetOptimizedDeliveryUseCase(get(), get()) }
    factory { GetCurrentLocationUseCase(get()) }
    factory { GetCurrentLocationOnceUseCase(get()) }
    factory { DetermineNextDeliveryStopUseCase() }
    factory { GetShouldShowBatterieOptimizationUseCase(get()) }
    factory { SetIsInTrackingModeUseCase(get()) }
    factory { GetIsInTrackingModeUseCase(get()) }
}