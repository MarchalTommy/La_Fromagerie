package com.mtdevelopment.checkout.data.remote.model.response.addressData

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Properties(
    val banId: String? = null,
    val city: String? = null,
    val citycode: String? = null,
    val context: String? = null,
    val id: String? = null,
    val importance: Double? = null,
    val label: String? = null,
    val municipality: String? = null,
    val name: String? = null,
    val population: Int? = null,
    val postcode: String? = null,
    val score: Double? = null,
    val type: String? = null,
    val x: Double? = null,
    val y: Double? = null
)