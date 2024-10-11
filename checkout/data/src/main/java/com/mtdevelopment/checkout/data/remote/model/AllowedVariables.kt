package com.mtdevelopment.checkout.data.remote.model

import org.json.JSONArray

val allowedCardNetworks = JSONArray(
    listOf(
        "MASTERCARD",
        "VISA"
    )
)

val allowedAuthMethods = JSONArray(
    listOf(
        "PAN_ONLY",
        "CRYPTOGRAM_3DS"
    )
)