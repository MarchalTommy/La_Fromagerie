package com.mtdevelopment.core.repository

import androidx.annotation.Keep
import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.core.model.UserInformation
import kotlinx.coroutines.flow.Flow

@Keep
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

    val lastFirestoreProductsUpdate: Flow<Long>

    suspend fun lastFirestoreProductsUpdate(timestamp: Long)

    val lastFirestorePathsUpdate: Flow<Long>

    suspend fun lastFirestorePathsUpdate(timestamp: Long)

    val shouldRefreshProducts: Flow<Boolean>

    suspend fun setShouldRefreshProducts(shouldRefresh: Boolean)

    val shouldRefreshPaths: Flow<Boolean>

    suspend fun setShouldRefreshPaths(shouldRefresh: Boolean)

    suspend fun clearAllDatastore()
}
