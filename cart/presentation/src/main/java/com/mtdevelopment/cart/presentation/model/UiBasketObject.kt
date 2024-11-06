package com.mtdevelopment.cart.presentation.model

import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class UiBasketObject(
    val id: String,
    var content: List<UiProductObject>,
    var totalPrice: String
)
