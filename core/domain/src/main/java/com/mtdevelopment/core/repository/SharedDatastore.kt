package com.mtdevelopment.core.repository

import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.core.model.UserInformation
import kotlinx.coroutines.flow.Flow

interface SharedDatastore {

    val cartItemsFlow: Flow<CartItems?>

    suspend fun setCartItems(cartItems: CartItems)

    suspend fun clearCartItems()

    val userInformationFlow: Flow<UserInformation?>

    suspend fun setUserInformation(userInformation: UserInformation)

    suspend fun clearUserInformation()

    val deliveryDateFlow: Flow<Long>

    suspend fun setDeliveryDate(date: Long)

    suspend fun clearDeliveryDate()

    suspend fun clearOrder()

    val lastFirestoreUpdateTimeStamp: Flow<Long>

    suspend fun lastFirestoreUpdateTimestamp(timestamp: Long)

    suspend fun clearAllDatastore()

}