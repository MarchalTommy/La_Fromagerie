package com.mtdevelopment.delivery.domain.repository

import com.mtdevelopment.delivery.domain.model.AutoCompleteSuggestion
import com.mtdevelopment.delivery.domain.model.CityInformation

interface AddressApiRepository {

    suspend fun reverseGeocodeCity(
        name: String,
        zip: Int
    ): CityInformation?

    suspend fun getAutoCompleteSuggestions(
        query: String
    ): List<AutoCompleteSuggestion?>
}