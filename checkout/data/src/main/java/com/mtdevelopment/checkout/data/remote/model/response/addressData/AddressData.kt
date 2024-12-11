package com.mtdevelopment.checkout.data.remote.model.response.addressData

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class AddressData(
    val attribution: String,
    val features: List<Feature>,
    val filters: Filters,
    val licence: String,
    val limit: Int,
    val query: String,
    val type: String,
    val version: String
)