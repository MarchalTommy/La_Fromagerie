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
 * @property priceInCentsPickupShop Unit price when the customer collects at the shop, in
 *   cents. Null means this product costs the same wherever it is collected — the common
 *   case, and what every product written before this field existed reads as.
 */
data class Product(
        val id: String,
        val name: String,
        val priceInCents: Long,
        val imageUrl: String,
        val type: String,
        val description: String = "",
        val allergens: List<String>? = null,
        val isAvailable: Boolean = true,
        val priceInCentsPickupShop: Long? = null
) {

    /**
     * The price actually charged for [fulfillmentType].
     *
     * Delivery is the reference and the market uses it unchanged: only collecting at the
     * shop can differ. A shop price is never allowed to exceed the delivery one — the admin
     * editor refuses it — which is what makes "the total can only go down when the customer
     * switches mode" true by construction, with no runtime check anywhere.
     */
    fun priceFor(fulfillmentType: FulfillmentType): Long = when (fulfillmentType) {
        FulfillmentType.PICKUP_SHOP -> priceInCentsPickupShop ?: priceInCents
        FulfillmentType.DELIVERY, FulfillmentType.PICKUP_MARKET -> priceInCents
    }
}
