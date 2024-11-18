package com.mtdevelopment.home.data.model

import com.mtdevelopment.core.presentation.sharedModels.ProductType
import com.mtdevelopment.home.domain.model.Product
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductData(
    @SerialName("id")
    val id: String = "",
    @SerialName("name")
    val name: String = "Unknown Cheese",
    @SerialName("priceCents")
    val priceInCents: Long = 0L,
    @SerialName("imgUrl")
    val imageUrl: String? = null,
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
        priceInCents = this.priceInCents,
        imageUrl = this.imageUrl ?: "",
        type = this.type.name,
        description = this.description.replace("\\n", "\n"),
        allergens = this.allergens,
    )
}

fun Product.toProductData(): ProductData {
    return ProductData(
        id = this.id,
        name = this.name,
        priceInCents = this.priceInCents,
        imageUrl = this.imageUrl,
        type = ProductType.valueOf(this.type),
        description = this.description.replace("\n", "\\n"),
        allergens = this.allergens,
    )
}