package com.mtdevelopment.checkout.domain.repository

import com.mtdevelopment.checkout.domain.model.CityInformation
import kotlinx.coroutines.flow.Flow

interface AddressApiRepository {

    suspend fun reverseGeocodeCity(name: String, zip: Int): CityInformation?
}