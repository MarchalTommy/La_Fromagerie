package com.mtdevelopment.cart.presentation.model

import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

// TODO: Clean ? No flow into data class I think
@Serializable
data class UiBasketObject(
    val id: String,
    val content: Flow<List<UiProductObject>>,
    var totalPrice: Flow<String>
)
