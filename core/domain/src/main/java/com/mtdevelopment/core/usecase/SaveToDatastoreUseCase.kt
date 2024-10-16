package com.mtdevelopment.core.usecase

import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.core.model.UserInformation
import com.mtdevelopment.core.repository.SharedDatastore

class SaveToDatastoreUseCase(
    private val sharedDatastore: SharedDatastore
) {

    suspend operator fun invoke(
        cartItems: CartItems? = null,
        userInformation: UserInformation? = null,
        deliveryDate: Long? = null
    ) {
        when {
            cartItems != null -> sharedDatastore.setCartItems(cartItems)
            userInformation != null -> sharedDatastore.setUserInformation(userInformation)
            deliveryDate != null -> sharedDatastore.setDeliveryDate(deliveryDate)
        }
    }

}