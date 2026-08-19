package com.mtdevelopment.admin.presentation.di

import com.mtdevelopment.admin.presentation.viewmodel.AdminViewModel
import org.koin.dsl.module

fun adminPresentationModule() = listOf(adminPresentationModule)

val adminPresentationModule = module {
    // Must NOT be a single: instances are stored in each screen's ViewModelStore.
    // A singleton would be cleared (viewModelScope cancelled) when the first screen
    // is popped, then reused dead on the next navigation (infinite loaders).
    //
    // Named, not positional. Twenty-two get() calls in a row are checked by nothing: two use
    // cases of the same arity could swap places and still compile, and the failure would first
    // be seen by the shop, on a delivery day, as the wrong write. The comments that used to
    // mark where each block started were an admission of the same problem. AppModule wires
    // CheckoutViewModel this way for exactly this reason.
    factory {
        AdminViewModel(
            updateProductUseCase = get(),
            deleteProductUseCase = get(),
            addNewProductUseCase = get(),
            updateDeliveryPathUseCase = get(),
            deleteDeliveryPathUseCase = get(),
            addNewDeliveryPathUseCase = get(),
            getAllOrdersUseCase = get(),
            uploadImageUseCase = get(),
            isInTrackingModeUseCase = get(),
            getOptimizedDeliveryUseCase = get(),
            getCurrentLocationOnceUseCase = get(),
            getAutocompleteSuggestionsUseCase = get(),
            shouldShowBatterieOptimizationUseCase = get(),
            getShouldShowBatterieOptimizationUseCase = get(),
            getPreparationStatusesUseCase = get(),
            updatePreparationStatusUseCase = get(),
            getAllPickupPointsUseCase = get(),
            addNewPickupPointUseCase = get(),
            updatePickupPointUseCase = get(),
            deletePickupPointUseCase = get(),
            markOrderPaidOnSiteUseCase = get(),
            cancelStaleOnSiteOrdersUseCase = get()
        )
    }
}
