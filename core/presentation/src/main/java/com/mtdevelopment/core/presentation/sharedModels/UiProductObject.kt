package com.mtdevelopment.core.presentation.sharedModels

import com.mtdevelopment.core.model.CartItem
import com.mtdevelopment.core.model.FulfillmentType
import com.mtdevelopment.core.model.Product
import com.mtdevelopment.core.model.ProductType
import com.mtdevelopment.core.util.serializableType
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

/**
 * A product as the customer currently sees it.
 *
 * [priceInCents] is the price for the fulfillment mode in force, resolved once where products
 * enter the UI state rather than threaded through every composable that renders a price. That
 * keeps the catalogue, the detail screen and the cart honest by construction: they display
 * what the customer will actually be charged, without knowing the mode exists.
 *
 * @property priceInCentsBeforeDiscount The delivery price, set only when the active mode is
 *   cheaper. Purely for showing the saving; never charged.
 * @property priceInCentsPickupShop The shop-collection price as stored, carried so the admin
 *   editor can read and write it. Null means the product costs the same everywhere.
 */
@Serializable
data class UiProductObject(
    val id: String,
    val name: String,
    val priceInCents: Long,
    var imageUrl: String? = null,
    val type: ProductType,
    val description: String = "",
    val allergens: List<String>? = null,
    var quantity: Int = 0,
    val isAvailable: Boolean = true,
    val priceInCentsBeforeDiscount: Long? = null,
    val priceInCentsPickupShop: Long? = null
) {

    companion object {
        val typeMap = mapOf(typeOf<UiProductObject>() to serializableType<UiProductObject>())
    }

}

/**
 * Resolves the product to what [fulfillmentType] actually costs.
 *
 * Defaults to delivery so every existing caller keeps its behaviour unchanged.
 */
fun Product.toUiProductObject(
    fulfillmentType: FulfillmentType = FulfillmentType.DELIVERY
) = UiProductObject(
    id = id,
    name = name,
    priceInCents = priceFor(fulfillmentType),
    priceInCentsBeforeDiscount = priceInCents.takeIf { it > priceFor(fulfillmentType) },
    priceInCentsPickupShop = priceInCentsPickupShop,
    imageUrl = imageUrl,
    type = ProductType.valueOf(type),
    description = description,
    allergens = allergens,
    isAvailable = isAvailable
)

/**
 * Back to the domain model. Only the admin edits products, and it always works in delivery
 * prices, so [UiProductObject.priceInCents] is the delivery price on that path.
 */
fun UiProductObject.toDomainProduct() = Product(
    id = id,
    name = name,
    priceInCents = priceInCents,
    priceInCentsPickupShop = priceInCentsPickupShop,
    imageUrl = imageUrl ?: "",
    type = type.name,
    description = description,
    allergens = allergens?.map { allergen ->
        allergen.trim().replaceFirstChar { firstChar -> firstChar.uppercaseChar() }
    },
    isAvailable = isAvailable
)

fun UiProductObject.toCartItem() = CartItem(
    name = this.name,
    price = this.priceInCents,
    quantity = this.quantity
)
