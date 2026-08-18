package com.mtdevelopment.delivery.data.repository

import com.mtdevelopment.core.util.NetWorkResult
import com.mtdevelopment.delivery.data.model.response.addressData.AddressData
import com.mtdevelopment.delivery.data.model.response.addressData.Feature
import com.mtdevelopment.delivery.data.model.response.addressData.Geometry
import com.mtdevelopment.delivery.data.model.response.addressData.Properties
import com.mtdevelopment.delivery.data.source.remote.AddressApiDataSource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Geocoding a commune to its center. Every delivery path depends on all of its cities resolving
 * here, so the interesting cases are the failures — and one of them used to be a crash.
 */
class AddressApiRepositoryImplGeocodingTest {

    private val dataSource: AddressApiDataSource = mockk()
    private val repository = AddressApiRepositoryImpl(dataSource)

    private fun municipality(name: String, postcode: String) = Feature(
        geometry = Geometry(coordinates = listOf(6.29, 46.80), type = "Point"),
        properties = Properties(name = name, city = name, postcode = postcode, type = "municipality"),
        type = "Feature"
    )

    @Test
    fun `resolves a commune to its center`() = runTest {
        coEvery { dataSource.getLngLatFromCity(any(), any()) } returns
                NetWorkResult.Success(AddressData(features = listOf(municipality("Malpas", "25160"))))

        val result = repository.reverseGeocodeCity("Malpas", 25160)

        assertEquals("Malpas", result?.name)
        assertEquals(25160, result?.zip)
        // The API answers lng/lat; LatLng takes lat first, so the pair must come back swapped.
        assertEquals(46.80, result?.location?.latitude)
        assertEquals(6.29, result?.location?.longitude)
    }

    /**
     * Regression: a query matching no commune answers **200 with an empty feature list**. That is
     * not a NetWorkResult.Error, so it slipped past the data source's try/catch, and `first()` threw
     * NoSuchElementException inside an `async` of a scope with no exception handler — crashing the
     * app instead of dropping one city.
     */
    @Test
    fun `an empty feature list yields null instead of throwing`() = runTest {
        coEvery { dataSource.getLngLatFromCity(any(), any()) } returns
                NetWorkResult.Success(AddressData(features = emptyList()))

        assertNull(repository.reverseGeocodeCity("Nulle-Part", 99999))
    }

    @Test
    fun `a network error yields null`() = runTest {
        coEvery { dataSource.getLngLatFromCity(any(), any()) } returns
                NetWorkResult.Error("timeout", "HttpRequestTimeoutException")

        assertNull(repository.reverseGeocodeCity("Boujailles", 25560))
    }

    /** Same trap on the customer-facing path: a typed address matching nothing must not crash. */
    @Test
    fun `geocoding an address that matches nothing yields null instead of throwing`() = runTest {
        coEvery { dataSource.getLngLatFromAddress(any()) } returns
                NetWorkResult.Success(AddressData(features = emptyList()))

        assertNull(repository.geocodeAddress("12 rue qui n'existe pas, Nulle-Part"))
    }
}
