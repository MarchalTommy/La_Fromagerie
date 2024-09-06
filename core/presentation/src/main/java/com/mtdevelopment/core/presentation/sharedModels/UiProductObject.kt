package com.mtdevelopment.core.presentation.sharedModels

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.mtdevelopment.core.util.serializableType
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

@Serializable
data class UiProductObject(
    val id: String,
    val name: String,
    val price: Double,
    val imageUrl: String? = null,
    val imageRes: Int? = null,
    val type: ProductType,
    val description: String,
    var quantity: Int = 0
) {

    fun toPrice(): String {
        return this.price.toString().replace(".", ",") + "€"
    }

    companion object {
        val typeMap = mapOf(typeOf<UiProductObject>() to serializableType<UiProductObject>())
    }
}
