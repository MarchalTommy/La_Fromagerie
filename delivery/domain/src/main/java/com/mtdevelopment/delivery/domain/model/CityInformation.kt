package com.mtdevelopment.delivery.domain.model

import androidx.annotation.Keep
import com.google.android.gms.maps.model.LatLng

@Keep
data class CityInformation(
    val name: String,
    val zip: Int,
    val location: LatLng
)