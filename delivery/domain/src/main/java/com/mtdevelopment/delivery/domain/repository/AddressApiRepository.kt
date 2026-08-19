package com.mtdevelopment.delivery.domain.repository

import com.mtdevelopment.delivery.domain.model.CityInformation
import com.mtdevelopment.delivery.domain.model.CommuneLookup

interface AddressApiRepository {

    suspend fun reverseGeocodeCity(
        name: String,
        zip: Int
    ): CityInformation?

    /**
     * Same lookup as [reverseGeocodeCity], but says **why** it failed — see [CommuneLookup].
     *
     * Used when the answer is about the data rather than about building a path: validating what the
     * shop typed needs to tell an unknown commune from an unreachable API.
     */
    suspend fun lookupCommune(
        name: String,
        zip: Int
    ): CommuneLookup

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