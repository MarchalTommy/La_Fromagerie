package com.mtdevelopment.core.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class ProductData(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("name")
    val name: String = "Unknown Cheese",
    @SerializedName("priceCents")
    val priceCents: Long = 0L,
    @SerializedName("imgUrl")
    val imgUrl: String? = null,
    @SerializedName("type")
    val type: ProductType = ProductType.FROMAGE,
    @SerializedName("description")
    val description: String = "",
    @SerializedName("allergens")
    val allergens: List<String>? = null,
    @SerializedName("isAvailable")
    val isAvailable: Boolean = true
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
        isAvailable = this.isAvailable
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
        isAvailable = this.isAvailable
    )
}