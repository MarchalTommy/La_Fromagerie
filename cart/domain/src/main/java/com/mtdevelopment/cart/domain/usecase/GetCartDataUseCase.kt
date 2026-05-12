package com.mtdevelopment.cart.domain.usecase

import com.mtdevelopment.core.repository.SharedDatastore

/**
 * Use case to retrieve the current cart items from the shared data store.
 * Returns a [Flow] of the current cart state.
 */
class GetCartDataUseCase(
    private val datastore: SharedDatastore
) {

    /**
     * Executes the use case.
     * @return A Flow emitting the current list of products in the cart.
     */
    operator fun invoke() = datastore.cartItemsFlow
}