package com.mtdevelopment.delivery.data.repository

import com.google.android.gms.maps.model.LatLng
import com.mtdevelopment.core.domain.normalizeCityName
import com.mtdevelopment.core.util.NetWorkResult
import com.mtdevelopment.delivery.data.model.response.addressData.AddressData
import com.mtdevelopment.delivery.data.source.remote.AddressApiDataSource
import com.mtdevelopment.delivery.domain.model.CityInformation
import com.mtdevelopment.delivery.domain.repository.AddressApiRepository

class AddressApiRepositoryImpl(
    private val addressApiDataSource: AddressApiDataSource
) : AddressApiRepository {

    /**
     * `firstOrNull`, not `first`: a query that matches no commune answers **200 with an empty
     * feature list**, which is not a [NetWorkResult.Error] and so slips past the data source's
     * try/catch. `first()` then threw NoSuchElementException inside the `async` of an ad-hoc
     * `CoroutineScope(Dispatchers.IO)` that installs no exception handler — i.e. it crashed the app
     * rather than dropping one city. A commune the API does not know is a data problem to report,
     * never a crash.
     */
    override suspend fun reverseGeocodeCity(name: String, zip: Int): CityInformation? {
        val result = addressApiDataSource.getLngLatFromCity(name, zip)

        if (result is NetWorkResult.Error) {
            return null
        }

        val feature = (result as? NetWorkResult.Success)?.data?.features?.firstOrNull()
        val properties = feature?.properties
        val geometry = feature?.geometry

        return if (geometry != null) {
            CityInformation(
                name = properties?.name ?: "",
                zip = properties?.postcode?.toInt() ?: 0,
                location = LatLng(
                    geometry.coordinates[1],
                    geometry.coordinates[0],
                )
            )
        } else {
            null
        }
    }

    override suspend fun getStreetSuggestions(
        query: String,
        city: String,
        postcode: Int
    ): List<String> {
        if (query.isBlank()) return emptyList()

        val result = addressApiDataSource.getStreetsInCity(query, city, postcode)
        val features = (result as? NetWorkResult.Success)?.data?.features ?: return emptyList()

        // The commune is already in the query, but the API is a fuzzy search: it will still slip
        // in a neighbouring commune when the query matches little. Match the commune again, on the
        // same normalization the customer matcher uses.
        val normalizedCity = city.normalizeCityName()
        return features
            .mapNotNull { it.properties }
            .filter { it.city?.normalizeCityName() == normalizedCity }
            .mapNotNull { it.name?.takeIf { name -> name.isNotBlank() } }
            .distinctBy { it.normalizeCityName() }
    }

    /** Same empty-feature-list trap as [reverseGeocodeCity], reached here by a customer typing an
     * address that matches nothing. */
    override suspend fun geocodeAddress(address: String): CityInformation? {
        val result = addressApiDataSource.getLngLatFromAddress(address)

        if (result is NetWorkResult.Error) {
            return null
        }

        val feature = (result as? NetWorkResult.Success)?.data?.features?.firstOrNull()
        val properties = feature?.properties
        val geometry = feature?.geometry

        return if (geometry != null) {
            CityInformation(
                name = properties?.name ?: "",
                zip = properties?.postcode?.toInt() ?: 0,
                location = LatLng(
                    geometry.coordinates[1],
                    geometry.coordinates[0],
                )
            )
        } else {
            null
        }
    }
}