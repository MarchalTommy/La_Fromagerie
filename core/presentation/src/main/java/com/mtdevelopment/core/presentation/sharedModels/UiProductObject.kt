package com.mtdevelopment.core.presentation.sharedModels

import com.mtdevelopment.core.util.serializableType
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

@Serializable
data class UiProductObject(
    val id: String,
    val name: String,
    val priceInCents: Long,
    val imageUrl: String? = null,
    val imageRes: Int? = null,
    val type: ProductType,
    val description: String = "",
    val allergens: List<String>? = null,
    var quantity: Int = 0
) {

    companion object {
        val typeMap = mapOf(typeOf<UiProductObject>() to serializableType<UiProductObject>())
    }
}
