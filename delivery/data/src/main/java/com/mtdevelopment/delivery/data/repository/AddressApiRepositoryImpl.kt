package com.mtdevelopment.delivery.data.repository

import com.google.android.gms.maps.model.LatLng
import com.mtdevelopment.core.util.NetWorkResult
import com.mtdevelopment.delivery.data.model.response.addressData.AddressData
import com.mtdevelopment.delivery.data.source.remote.AddressApiDataSource
import com.mtdevelopment.delivery.domain.model.CityInformation
import com.mtdevelopment.delivery.domain.repository.AddressApiRepository

class AddressApiRepositoryImpl(
    private val addressApiDataSource: AddressApiDataSource
) : AddressApiRepository {

    override suspend fun reverseGeocodeCity(name: String, zip: Int): CityInformation? {
        val result = addressApiDataSource.getLngLatFromCity(name, zip)

        if (result is NetWorkResult.Error) {
            return null
        }

        val properties =
            (result as NetWorkResult<AddressData>).data?.features?.first()?.properties
        val geometry =
            (result as NetWorkResult<AddressData>).data?.features?.first()?.geometry

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

    override suspend fun geocodeAddress(address: String): CityInformation? {
        val result = addressApiDataSource.getLngLatFromAddress(address)

        if (result is NetWorkResult.Error) {
            return null
        }

        val properties =
            (result as NetWorkResult<AddressData>).data?.features?.first()?.properties
        val geometry =
            (result as NetWorkResult<AddressData>).data?.features?.first()?.geometry

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