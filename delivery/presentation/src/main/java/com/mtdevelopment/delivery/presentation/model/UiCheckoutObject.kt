package com.mtdevelopment.delivery.presentation.model

import com.mtdevelopment.cart.presentation.model.UiBasketObject
import com.mtdevelopment.delivery.domain.model.DeliveryPath
import kotlinx.serialization.Serializable

// TODO: Create an object with only pre-formatted data. Store all data in datastore and generates
//  the checkoutObject from domain ? This way would allow for a pre-fill of the user data and
//  delivery path ?
@Serializable
data class UiCheckoutObject(
    val cartItems: UiBasketObject,
    val userInfo: UserInfo,
    val deliveryPath: DeliveryPath,
    val deliveryDate: Long
)
