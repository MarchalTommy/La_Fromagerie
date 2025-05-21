package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.repository.CheckoutDatastorePreference
import com.mtdevelopment.core.model.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

class GetSavedOrderUseCase(
    private val checkoutDatastorePreference: CheckoutDatastorePreference
) {
    operator fun invoke(): Flow<Order> {
        return checkoutDatastorePreference.orderFlow.mapNotNull { it }
    }
}