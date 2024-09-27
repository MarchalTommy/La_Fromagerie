package com.mtdevelopment.lafromagerie.navigation

import com.mtdevelopment.core.presentation.sharedModels.UiProductObject
import kotlinx.serialization.Serializable


@Serializable
object HomeScreen

@Serializable
data class DetailDestination(
    val productObject: UiProductObject
)

@Serializable
object CheckoutScreen