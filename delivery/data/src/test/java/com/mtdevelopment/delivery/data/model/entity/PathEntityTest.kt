package com.mtdevelopment.delivery.data.model.entity

import com.mtdevelopment.core.model.DeliveryCity
import com.mtdevelopment.delivery.domain.model.DeliveryPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Room is the normal read path for delivery paths, so a street restriction that does not survive
 * the cache round-trip is a restriction that silently disappears on the second app launch.
 */
class PathEntityTest {

    private val splitPath = DeliveryPath(
        id = "path-a",
        pathName = "Parcours A",
        cities = listOf(
            DeliveryCity("Boujailles", 25560, listOf("Rue du Moulin", "Grande Rue")),
            DeliveryCity("Frasne", 25560)
        ),
        locations = listOf(46.85 to 6.15, 46.85 to 6.17),
        deliveryDay = "TUESDAY",
        geoJson = null
    )

    @Test
    fun `street restrictions survive the Room round-trip`() {
        val restored = splitPath.toPathEntity().toPath()

        assertEquals(splitPath.cities, restored.cities)
        assertEquals(listOf("Rue du Moulin", "Grande Rue"), restored.cities[0].streets)
    }

    @Test
    fun `a city with no restriction stays unrestricted through the Room round-trip`() {
        val restored = splitPath.toPathEntity().toPath()

        assertTrue(restored.cities[1].streets.isEmpty())
        assertTrue(restored.cities[1].coversWholeCity)
    }

    @Test
    fun `city order and postcodes survive the Room round-trip`() {
        val restored = splitPath.toPathEntity().toPath()

        assertEquals(listOf("Boujailles", "Frasne"), restored.cities.map { it.name })
        assertEquals(listOf(25560, 25560), restored.cities.map { it.postcode })
    }

    /**
     * Rows written by a build that predates the street columns come back with an empty
     * `cityStreets` map: every cached city must then be treated as covered in full, which is the
     * behaviour those rows already had.
     */
    @Test
    fun `rows cached without street data degrade to whole-city coverage`() {
        val legacyRow = splitPath.toPathEntity().copy(cityStreets = emptyMap())

        val restored = legacyRow.toPath()

        assertTrue(restored.cities.all { it.coversWholeCity })
        assertEquals(listOf("Boujailles", "Frasne"), restored.cities.map { it.name })
    }

    @Test
    fun `a path without geocoded locations does not blow up on the way into Room`() {
        val entity = splitPath.copy(locations = null).toPathEntity()

        assertTrue(entity.locations.isEmpty())
        assertEquals(splitPath.cities, entity.toPath().cities)
    }
}
