package com.mtdevelopment.home.presentation.model

data class UiProductObject(
    val id: String,
    val name: String,
    val price: Double,
    val imageUrl: String? = null,
    val imageRes: Int? = null,
    val type: ProductType,
    val description: String
) {

}
