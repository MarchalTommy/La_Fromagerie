package com.mtdevelopment.delivery.domain.repository

import com.mtdevelopment.delivery.domain.model.CityInformation

interface AddressApiRepository {

    suspend fun reverseGeocodeCity(
        name: String,
        zip: Int
    ): CityInformation?

    suspend fun geocodeAddress(
        address: String
    ): CityInformation?

    /**
     * Street names of [city] matching [query], for the shop to pick from when restricting a path
     * to part of a commune.
     *
     * Returns names only — the editor stores a plain label, and that label is what the customer
     * matcher compares against. Never throws: on a network failure the shop can still type the
     * street by hand.
     */
    suspend fun getStreetSuggestions(
        query: String,
        city: String,
        postcode: Int
    ): List<String>
}