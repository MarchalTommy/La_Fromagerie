package com.mtdevelopment.delivery.domain.repository

interface AddressApiRepository {

    suspend fun reverseGeocodeCity(
        name: String,
        zip: Int
    ): com.mtdevelopment.delivery.domain.model.CityInformation?
}