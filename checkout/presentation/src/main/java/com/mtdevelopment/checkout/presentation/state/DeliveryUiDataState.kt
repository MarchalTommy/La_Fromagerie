package com.mtdevelopment.checkout.presentation.state

import com.mtdevelopment.checkout.presentation.model.UiDeliveryPath

data class DeliveryUiDataState(
    val shouldDatePickerBeClickable: Boolean = false,
    val datePickerVisibility: Boolean = false,
    val dateFieldText: String = "",
    val userNameFieldText: String = "",
    val userAddressFieldText: String = "",
    val shouldShowLocalisationPermission: Boolean = false,
    val localisationSuccess: Boolean = false,
    val userLocationOnPath: Boolean = false,
    val showDeliveryPathPicker: Boolean = false,
    val isLoading: Boolean = false,
    val columnScrollingEnabled: Boolean = true,

    val userCity: String = "",
    val userCityLocation: Pair<Double, Double> = Pair(0.0, 0.0),
    val selectedPath: UiDeliveryPath? = null,

    val deliveryPaths: List<UiDeliveryPath> = emptyList()
)