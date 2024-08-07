package com.mtdevelopment.home.presentation.model

import kotlinx.coroutines.flow.Flow

data class UiBasketObject(
    val id: String,
    val content: Flow<List<UiProductObject>>,
    var totalPrice: Flow<String>
)
