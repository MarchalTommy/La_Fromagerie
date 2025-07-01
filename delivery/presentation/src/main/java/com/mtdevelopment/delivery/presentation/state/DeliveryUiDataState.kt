package com.mtdevelopment.delivery.presentation.state

import com.mtdevelopment.delivery.domain.model.AutoCompleteSuggestion
import com.mtdevelopment.delivery.presentation.model.UiDeliveryPath

data class DeliveryUiDataState(
    val shouldDatePickerBeClickable: Boolean = false,
    val datePickerVisibility: Boolean = false,
    val dateFieldText: String = "",
    val userNameFieldText: String = "",

    val deliveryAddressSearchQuery: String = "",
    val deliveryAddressSuggestions: List<AutoCompleteSuggestion> = emptyList(),

    val billingAddressSearchQuery: String = "",
    val billingAddressSuggestions: List<AutoCompleteSuggestion> = emptyList(),

    val addressSuggestionsLoading: Boolean = false,
    val showAddressSuggestions: Boolean = false,
    val showBillingAddressSuggestions: Boolean = false,

    val shouldShowLocalisationPermission: Boolean = false,
    val localisationSuccess: Boolean = false,
    val userLocationOnPath: Boolean = false,
    val userLocationCloseFromPath: Boolean = false,
    val isLoading: Boolean = false,
    val isError: String = "",
    val columnScrollingEnabled: Boolean = true,

    val userCity: String = "",
    val userCityLocation: Pair<Double, Double>? = null,
    val selectedPath: UiDeliveryPath? = null,

    val deliveryPaths: List<UiDeliveryPath> = emptyList(),
    val isBillingDifferent: Boolean = false
)