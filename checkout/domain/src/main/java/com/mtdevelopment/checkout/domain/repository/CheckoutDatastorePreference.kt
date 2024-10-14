package com.mtdevelopment.checkout.domain.repository

import kotlinx.coroutines.flow.Flow


interface CheckoutDatastorePreference {

    val sumUpTokenFlow: Flow<String>

    suspend fun setSumUpToken(token: String)

    val sumUpRefreshTokenFlow: Flow<String>

    suspend fun setSumUpRefreshToken(token: String)

    val sumUpTokenValidityFlow: Flow<Long>

    suspend fun setSumUpTokenValidity(duration: Long)

}