package com.mtdevelopment.checkout.domain.repository

import com.mtdevelopment.checkout.domain.model.NewCheckoutResult
import com.mtdevelopment.core.model.Order
import kotlinx.coroutines.flow.Flow


interface CheckoutDatastorePreference {

    val sumUpTokenFlow: Flow<String>

    suspend fun setSumUpToken(token: String)

    val sumUpRefreshTokenFlow: Flow<String>

    suspend fun setSumUpRefreshToken(token: String)

    val sumUpTokenValidityFlow: Flow<Long>

    suspend fun setSumUpTokenValidity(duration: Long)

    val checkoutReferencesFlow: Flow<List<String>>

    suspend fun saveCheckoutReference(reference: String)

    val createdCheckoutFlow: Flow<NewCheckoutResult>

    suspend fun saveCreatedCheckout(data: NewCheckoutResult)

    val isCheckoutSuccessfulFlow: Flow<Boolean>

    suspend fun setIsCheckoutSuccessful(isSuccess: Boolean)

    suspend fun resetCheckoutStatus()

    val orderFlow: Flow<Order?>

    suspend fun saveOrder(order: Order)
}