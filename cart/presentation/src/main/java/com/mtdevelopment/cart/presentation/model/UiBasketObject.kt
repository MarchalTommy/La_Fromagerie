package com.mtdevelopment.cart.presentation.model

import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import kotlinx.coroutines.flow.Flow

data class UiBasketObject(
    val id: String,
    val content: Flow<List<UiProductObject>>,
    var totalPrice: Flow<String>
)
