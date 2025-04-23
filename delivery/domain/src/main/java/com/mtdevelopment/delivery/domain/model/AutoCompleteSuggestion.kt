package com.mtdevelopment.delivery.domain.model

data class AutoCompleteSuggestion(
    val city: String? = null,
    val postCode: String? = null,
    val fulltext: String? = null,
    val lat: Double? = null,
    val long: Double? = null
)
