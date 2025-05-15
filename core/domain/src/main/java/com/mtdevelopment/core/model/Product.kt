package com.mtdevelopment.core.model


data class Product(
        val id: String,
        val name: String,
        val priceInCents: Long,
        val imageUrl: String,
        val type: String,
        val description: String = "",
        val allergens: List<String>? = null,
        val isAvailable: Boolean = true
)
