package com.mtdevelopment.checkout.data.repository

import com.google.android.gms.maps.model.LatLng
import com.mtdevelopment.checkout.data.remote.source.AddressApiDataSource
import com.mtdevelopment.checkout.domain.model.CityInformation
import com.mtdevelopment.checkout.domain.repository.AddressApiRepository

class AddressApiRepositoryImpl(
    val addressApiDataSource: AddressApiDataSource
) : AddressApiRepository {

    override suspend fun reverseGeocodeCity(name: String, zip: Int): CityInformation? {
        val result = addressApiDataSource.getLngLatFromCity(name, zip)
        val properties = result.data?.features?.first()?.properties
        val geometry = result.data?.features?.first()?.geometry

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