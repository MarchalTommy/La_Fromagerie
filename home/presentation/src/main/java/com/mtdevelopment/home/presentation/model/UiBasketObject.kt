package com.mtdevelopment.home.presentation.model

data class UiBasketObject(
    val id: String,
    val content: List<UiProductObject>,
    val totalPrice: Double
)
