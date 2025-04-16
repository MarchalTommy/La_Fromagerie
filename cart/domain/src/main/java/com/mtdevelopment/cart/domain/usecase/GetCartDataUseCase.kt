package com.mtdevelopment.cart.domain.usecase

import com.mtdevelopment.core.repository.SharedDatastore

class GetCartDataUseCase(
    private val datastore: SharedDatastore
) {

    operator fun invoke() = datastore.cartItemsFlow
}