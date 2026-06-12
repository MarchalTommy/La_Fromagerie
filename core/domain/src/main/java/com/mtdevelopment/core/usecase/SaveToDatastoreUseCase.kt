package com.mtdevelopment.core.usecase

import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.core.model.UserInformation
import com.mtdevelopment.core.repository.SharedDatastore

class SaveToDatastoreUseCase(
    private val sharedDatastore: SharedDatastore
) {

    /**
     * Persists every non-null argument. Arguments are independent: passing several at once saves
     * them all instead of only the first one.
     */
    suspend operator fun invoke(
        cartItems: CartItems? = null,
        userInformation: UserInformation? = null,
        deliveryDate: Long? = null
    ) {
        cartItems?.let { sharedDatastore.setCartItems(it) }
        userInformation?.let { sharedDatastore.setUserInformation(it) }
        deliveryDate?.let { sharedDatastore.setDeliveryDate(it) }
    }

}