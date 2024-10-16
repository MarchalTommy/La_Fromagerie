package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.model.LocalCheckoutInformation
import com.mtdevelopment.core.repository.SharedDatastore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetCheckoutDataUseCase(
    private val sharedDatastore: SharedDatastore
) {
    operator fun invoke(): Flow<LocalCheckoutInformation?> {

        val user = sharedDatastore.userInformationFlow
        val cart = sharedDatastore.cartItemsFlow
        val deliveryDate = sharedDatastore.deliveryDateFlow

        var localCheckoutInformation = user.combine(cart) { user, cart ->
            val totalPrice = cart?.cartItems?.sumOf { it.price * it.quantity }
            if (cart != null && user != null) {
                LocalCheckoutInformation(
                    buyerName = user.name,
                    buyerAddress = user.address,
                    cartItems = cart,
                    totalPrice = totalPrice ?: 0L,
                    deliveryDate = 0L
                )
            } else {
                null
            }
        }

        localCheckoutInformation =
            localCheckoutInformation.combine(deliveryDate) { localCheckoutInformation, deliveryDate ->
                localCheckoutInformation?.copy(deliveryDate = deliveryDate)
            }

        return localCheckoutInformation
    }

}