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

    ///////////////////////////////////////////////////////////////////////////
    // Commune centers
    ///////////////////////////////////////////////////////////////////////////

    private val geolocatedPath = splitPath.copy(
        cities = listOf(
            DeliveryCity("Boujailles", 25560, listOf("Rue du Moulin"), 46.85, 6.15),
            DeliveryCity("Frasne", 25560, emptyList(), 46.85, 6.17)
        )
    )

    /**
     * The centers are what make a cached path readable without geocoding, so losing them in the
     * cache would put the whole path back on the network on every launch.
     */
    @Test
    fun `commune centers survive the Room round-trip`() {
        val restored = geolocatedPath.toPathEntity().toPath()

        assertEquals(geolocatedPath.cities, restored.cities)
        assertEquals(46.85 to 6.15, restored.cities[0].location)
        assertEquals(46.85 to 6.17, restored.cities[1].location)
    }

    /**
     * Rows written before `MIGRATION_6_7` come back with an empty map, exactly as the ALTER TABLE
     * default leaves them. Those cities simply have no stored center and get looked up on the next
     * refresh — the same behaviour they already had.
     */
    @Test
    fun `rows cached without commune centers degrade to no stored coordinate`() {
        val legacyRow = geolocatedPath.toPathEntity().copy(cityCoordinates = emptyMap())

        val restored = legacyRow.toPath()

        assertTrue(restored.cities.all { it.location == null })
        assertEquals(listOf("Boujailles", "Frasne"), restored.cities.map { it.name })
        // The street split is independent of the centers and must not go down with them.
        assertEquals(listOf("Rue du Moulin"), restored.cities[0].streets)
    }

    @Test
    fun `a city with no center is written without one rather than with a bogus zero`() {
        val entity = splitPath.toPathEntity()

        assertTrue(entity.cityCoordinates.isEmpty())
        assertTrue(entity.toPath().cities.all { it.location == null })
    }
}
