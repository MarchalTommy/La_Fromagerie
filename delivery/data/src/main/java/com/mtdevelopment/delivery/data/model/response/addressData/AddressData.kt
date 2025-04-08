package com.mtdevelopment.delivery.data.model.response.addressData

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class AddressData(
    val attribution: String? = null,
    val features: List<Feature>,
    val filters: Filters? = null,
    val licence: String? = null,
    val limit: Int? = null,
    val query: String? = null,
    val type: String? = null,
    val version: String? = null
)