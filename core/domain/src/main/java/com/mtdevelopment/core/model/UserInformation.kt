package com.mtdevelopment.core.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class UserInformation(
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("address")
    val address: String,
    @SerializedName("billingAddress")
    val billingAddress: String,
    @SerializedName("lastSelectedPath")
    val lastSelectedPath: String,
    // Everything below is additive and NULLABLE on purpose. This object is persisted as JSON
    // and read back with Gson, which fills a missing field with null regardless of the Kotlin
    // type — a non-null field with a default would therefore arrive null from any payload
    // written before this change and blow up at first use.
    @SerializedName("phone")
    val phone: String? = null,
    /** [com.mtdevelopment.core.model.FulfillmentType] name; null reads as DELIVERY. */
    @SerializedName("fulfillmentType")
    val fulfillmentType: String? = null,
    @SerializedName("pickupPointId")
    val pickupPointId: String? = null,
    @SerializedName("pickupLabel")
    val pickupLabel: String? = null,
    @SerializedName("pickupAddress")
    val pickupAddress: String? = null,
    @SerializedName("pickupTimeRange")
    val pickupTimeRange: String? = null
)
