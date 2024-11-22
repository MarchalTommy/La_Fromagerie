package com.mtdevelopment.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductData(
    @SerialName("id")
    val id: String = "",
    @SerialName("name")
    val name: String = "Unknown Cheese",
    @SerialName("priceCents")
    val priceCents: Long = 0L,
    @SerialName("imgUrl")
    val imgUrl: String? = null,
    @SerialName("type")
    val type: ProductType = ProductType.FROMAGE,
    @SerialName("description")
    val description: String = "",
    @SerialName("allergens")
    val allergens: List<String>? = null,
)

fun String.toProductType(): ProductType {
    return ProductType.valueOf(this)
}

fun ProductData.toProduct(): Product {
    return Product(
        id = this.id,
        name = this.name,
        priceInCents = this.priceCents,
        imageUrl = this.imgUrl ?: "",
        type = this.type.name,
        description = this.description.replace("\\n", "\n"),
        allergens = this.allergens,
    )
}

fun Product.toProductData(): ProductData {
    return ProductData(
        id = this.id,
        name = this.name,
        priceCents = this.priceInCents,
        imgUrl = this.imageUrl,
        type = ProductType.valueOf(this.type),
        description = this.description.replace("\n", "\\n"),
        allergens = this.allergens,
    )
}