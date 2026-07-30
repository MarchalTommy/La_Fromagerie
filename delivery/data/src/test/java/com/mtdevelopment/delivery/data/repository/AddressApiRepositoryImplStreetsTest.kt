package com.mtdevelopment.delivery.data.repository

import com.mtdevelopment.core.util.NetWorkResult
import com.mtdevelopment.delivery.data.model.response.addressData.AddressData
import com.mtdevelopment.delivery.data.model.response.addressData.Feature
import com.mtdevelopment.delivery.data.model.response.addressData.Geometry
import com.mtdevelopment.delivery.data.model.response.addressData.Properties
import com.mtdevelopment.delivery.data.source.remote.AddressApiDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The shop picks street names from these suggestions, and the customer matcher later compares the
 * stored label against a typed address — so what comes out of here decides whether a split commune
 * routes correctly. It must never contain a street from a neighbouring commune.
 */
class AddressApiRepositoryImplStreetsTest {

    private val dataSource: AddressApiDataSource = mockk()
    private val repository = AddressApiRepositoryImpl(dataSource)

    private fun feature(name: String, city: String) = Feature(
        geometry = Geometry(coordinates = listOf(6.15, 46.85), type = "Point"),
        properties = Properties(name = name, city = city, postcode = "25560", type = "street"),
        type = "Feature"
    )

    private fun response(vararg features: Feature) =
        NetWorkResult.Success(AddressData(features = features.toList()))

    @Test
    fun `street names of the requested commune are returned`() = runTest {
        coEvery { dataSource.getStreetsInCity(any(), any(), any(), any()) } returns response(
            feature("Rue du Moulin", "Boujailles"),
            feature("Grande Rue", "Boujailles")
        )

        val result = repository.getStreetSuggestions("rue", "Boujailles", 25560)

        assertEquals(listOf("Rue du Moulin", "Grande Rue"), result)
    }

    /**
     * 25560 is Frasne, Boujailles and Courvière at once. The commune goes into the query so the
     * ranking favours it, but the search stays fuzzy — without this filter the shop could pin a
     * path to a street that is not even in the town.
     */
    @Test
    fun `streets from another commune sharing the postcode are dropped`() = runTest {
        coEvery { dataSource.getStreetsInCity(any(), any(), any(), any()) } returns response(
            feature("Rue du Moulin", "Boujailles"),
            feature("Rue du Moulin", "Frasne")
        )

        val result = repository.getStreetSuggestions("moulin", "Boujailles", 25560)

        assertEquals(listOf("Rue du Moulin"), result)
    }

    @Test
    fun `the commune is matched ignoring case and accents`() = runTest {
        coEvery { dataSource.getStreetsInCity(any(), any(), any(), any()) } returns response(
            feature("Rue de l'Église", "Courvière")
        )

        val result = repository.getStreetSuggestions("eglise", "COURVIERE", 25560)

        assertEquals(listOf("Rue de l'Église"), result)
    }

    @Test
    fun `the same street returned twice is listed once`() = runTest {
        coEvery { dataSource.getStreetsInCity(any(), any(), any(), any()) } returns response(
            feature("Grande Rue", "Boujailles"),
            feature("grande rue", "Boujailles")
        )

        val result = repository.getStreetSuggestions("rue", "Boujailles", 25560)

        assertEquals(listOf("Grande Rue"), result)
    }

    /** Losing the network must leave the shop typing by hand, not staring at a crash. */
    @Test
    fun `a network error yields no suggestions instead of throwing`() = runTest {
        coEvery { dataSource.getStreetsInCity(any(), any(), any(), any()) } returns
                NetWorkResult.Error("offline", "IOException")

        val result = repository.getStreetSuggestions("rue", "Boujailles", 25560)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `a blank query never reaches the network`() = runTest {
        val result = repository.getStreetSuggestions("   ", "Boujailles", 25560)

        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { dataSource.getStreetsInCity(any(), any(), any(), any()) }
    }

    /** Without the commune in the query the API returns ten streets of the neighbouring town. */
    @Test
    fun `the commune is passed to the data source, not only its postcode`() = runTest {
        coEvery {
            dataSource.getStreetsInCity(any(), any(), any(), any())
        } returns response(feature("Rue du Crêt", "Boujailles"))

        repository.getStreetSuggestions("cret", "Boujailles", 25560)

        coVerify { dataSource.getStreetsInCity("cret", "Boujailles", 25560, any()) }
    }

    @Test
    fun `features without a name are skipped`() = runTest {
        coEvery { dataSource.getStreetsInCity(any(), any(), any(), any()) } returns response(
            feature("", "Boujailles"),
            feature("Grande Rue", "Boujailles")
        )

        val result = repository.getStreetSuggestions("rue", "Boujailles", 25560)

        assertEquals(listOf("Grande Rue"), result)
    }
}
