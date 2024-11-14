package com.mtdevelopment.home.domain.model


data class Product(
        val id: String,
        val name: String,
        val priceInCents: Long,
        val imageUrl: String,
        val type: String,
        val description: String = "",
        val allergens: List<String>? = null,
)
