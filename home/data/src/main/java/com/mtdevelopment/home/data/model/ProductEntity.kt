package com.mtdevelopment.home.data.model

import androidx.room.Entity
import com.mtdevelopment.core.model.Product
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "products", primaryKeys = ["id"])
data class ProductEntity(
    @SerialName("id")
    val id: String = "",
    @SerialName("name")
    val name: String = "Unknown Cheese",
    @SerialName("priceCents")
    val priceInCents: Long = 0L,
    @SerialName("imgUrl")
    val imageUrl: String? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("description")
    val description: String = "",
    @SerialName("allergens")
    val allergens: List<String>? = null
)

fun ProductEntity.toProduct(): Product {
    return Product(
        id = this.id,
        name = this.name,
        priceInCents = this.priceInCents,
        imageUrl = this.imageUrl ?: "",
        type = this.type ?: "",
        description = this.description,
        allergens = this.allergens,
    )
}

fun Product.toProductEntity(): ProductEntity {
    return ProductEntity(
        id = this.id,
        name = this.name,
        priceInCents = this.priceInCents,
        imageUrl = this.imageUrl,
        type = this.type,
        description = this.description,
        allergens = this.allergens,
    )
}