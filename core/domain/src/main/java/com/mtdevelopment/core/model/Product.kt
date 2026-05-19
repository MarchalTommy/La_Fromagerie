package com.mtdevelopment.core.model


/**
 * Core domain model representing a product in the catalog.
 * 
 * @property id Unique identifier for the product.
 * @property name Product name (e.g., "Fromage de chèvre").
 * @property priceInCents Unit price in cents (to avoid floating point errors).
 * @property imageUrl URL to the product image on Cloudinary or Firebase.
 * @property type Category of the product (e.g., "CHEESE", "MILK").
 * @property description Detailed product description.
 * @property allergens List of allergen names present in the product.
 * @property isAvailable Whether the product is currently in stock and purchasable.
 */
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
