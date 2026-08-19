package com.mtdevelopment.delivery.data.model.response.firestore

import com.mtdevelopment.core.model.DeliveryCity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The read half of the Firestore contract. The write half is asserted in admin/data's
 * `DataDeliveryPathTest`; together they cover the full domain → Firestore → domain loop.
 */
class DataDeliveryPathsResponseTest {

    /**
     * The stored center is what lets a path be rebuilt without geocoding. It is nullable because
     * documents last written by an older admin build carry none, and those cities are looked up on
     * the fly — which is what makes the field additive rather than a migration.
     */
    @Test
    fun `city_entries carries the commune centers when the document has them`() {
        val response = DataDeliveryPathsResponse(
            id = "path-a",
            path_name = "Parcours A",
            deliveryDay = "FRIDAY",
            cityEntries = listOf(
                DataDeliveryCityResponse("Malpas", 25160, emptyList(), lat = 46.80, lng = 6.29),
                DataDeliveryCityResponse("Frasne", 25560)
            )
        )

        val cities = response.toDeliveryCities()

        assertEquals(46.80 to 6.29, cities[0].location)
        assertEquals(null, cities[1].location)
    }

    @Test
    fun `city_entries carries the per-city street restrictions`() {
        val response = DataDeliveryPathsResponse(
            id = "path-a",
            path_name = "Parcours A",
            deliveryDay = "TUESDAY",
            cityEntries = listOf(
                DataDeliveryCityResponse(
                    city = "Boujailles",
                    postcode = 25560,
                    streets = listOf("Rue du Moulin", "Grande Rue")
                ),
                DataDeliveryCityResponse(city = "Frasne", postcode = 25560)
            )
        )

        assertEquals(
            listOf(
                DeliveryCity("Boujailles", 25560, listOf("Rue du Moulin", "Grande Rue")),
                DeliveryCity("Frasne", 25560)
            ),
            response.toDeliveryCities()
        )
    }

    /**
     * Documents written before the split-city feature only carry the parallel arrays. They must
     * keep working — they convert to the new shape the next time the admin saves the path.
     */
    @Test
    fun `legacy parallel arrays are zipped into unrestricted cities`() {
        val response = DataDeliveryPathsResponse(
            id = "path-a",
            path_name = "Parcours A",
            cities = listOf("Boujailles", "Frasne"),
            postcodes = listOf(25560, 25560)
        )

        val cities = response.toDeliveryCities()

        assertEquals(listOf("Boujailles", "Frasne"), cities.map { it.name })
        assertEquals(listOf(25560, 25560), cities.map { it.postcode })
        assertTrue(cities.all { it.coversWholeCity })
    }

    @Test
    fun `city_entries wins over the legacy arrays when a document carries both`() {
        val response = DataDeliveryPathsResponse(
            id = "path-a",
            path_name = "Parcours A",
            cities = listOf("Boujailles", "Frasne"),
            postcodes = listOf(25560, 25560),
            cityEntries = listOf(
                DataDeliveryCityResponse(
                    city = "Boujailles",
                    postcode = 25560,
                    streets = listOf("Rue du Moulin")
                )
            )
        )

        val cities = response.toDeliveryCities()

        assertEquals(1, cities.size)
        assertEquals(listOf("Rue du Moulin"), cities[0].streets)
    }

    @Test
    fun `a document carrying neither shape resolves to no cities`() {
        val response = DataDeliveryPathsResponse(id = "path-a", path_name = "Parcours A")

        assertTrue(response.toDeliveryCities().isEmpty())
    }
}
