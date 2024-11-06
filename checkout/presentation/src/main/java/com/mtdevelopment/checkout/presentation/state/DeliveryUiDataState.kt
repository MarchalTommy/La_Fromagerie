package com.mtdevelopment.checkout.presentation.state

data class DeliveryUiDataState(
    val shouldDatePickerBeClickable: Boolean = false,
    val datePickerVisibility: Boolean = false,
    val dateFieldText: String = "",
    val userNameFieldText: String = "",
    val userAddressFieldText: String = "",
    val localisationPermissionState: Boolean = false,
    val localisationSuccess: Boolean = false,
    val geolocIsOnPath: Boolean = false,
    val showDeliveryPathPicker: Boolean = false
)