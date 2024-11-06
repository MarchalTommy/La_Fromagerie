package com.mtdevelopment.cart.presentation.state

import com.mtdevelopment.cart.presentation.model.UiBasketObject
import java.util.UUID

data class CartUiState(
    val isCartVisible: Boolean = false,
    val cartObject: UiBasketObject = UiBasketObject(
        UUID.randomUUID().toString(),
        emptyList(),
        "0,00€"
    ),


    )
