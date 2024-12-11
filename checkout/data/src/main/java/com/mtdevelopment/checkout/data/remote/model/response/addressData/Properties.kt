package com.mtdevelopment.checkout.data.remote.model.response.addressData

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Properties(
    val banId: String,
    val city: String,
    val citycode: String,
    val context: String,
    val id: String,
    val importance: Double,
    val label: String,
    val municipality: String,
    val name: String,
    val population: Int,
    val postcode: String,
    val score: Double,
    val type: String,
    val x: Double,
    val y: Double
)