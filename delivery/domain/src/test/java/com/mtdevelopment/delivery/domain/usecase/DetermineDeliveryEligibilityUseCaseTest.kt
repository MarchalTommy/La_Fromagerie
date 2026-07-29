package com.mtdevelopment.delivery.domain.usecase

import android.location.Location
import com.mtdevelopment.core.model.DeliveryCity
import com.mtdevelopment.delivery.domain.model.DeliveryPath
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Covers the real shop configuration that motivated the split-city feature:
 *
 * - Parcours A (Tuesday): Boujailles (rue du Moulin, Grande Rue), Frasne, Courvière
 * - Parcours B (Friday): Boujailles (rue de la Gare, Chemin des Prés), Arc-sous-Montenot, Villers
 *
 * Only Boujailles is split. Every other city is served in full by whichever path lists it.
 */
class DetermineDeliveryEligibilityUseCaseTest {

    private val useCase = DetermineDeliveryEligibilityUseCase()

    private val pathA = DeliveryPath(
        id = "path-a",
        pathName = "Parcours A",
        cities = listOf(
            DeliveryCity("Boujailles", 25560, listOf("Rue du Moulin", "Grande Rue")),
            DeliveryCity("Frasne", 25560),
            DeliveryCity("Courvière", 25560)
        ),
        locations = listOf(46.85 to 6.15, 46.85 to 6.17, 46.86 to 6.16),
        deliveryDay = "TUESDAY",
        geoJson = null
    )

    private val pathB = DeliveryPath(
        id = "path-b",
        pathName = "Parcours B",
        cities = listOf(
            DeliveryCity("Boujailles", 25560, listOf("Rue de la Gare", "Chemin des Prés")),
            DeliveryCity("Arc-sous-Montenot", 25270),
            DeliveryCity("Villers", 25560)
        ),
        locations = listOf(46.85 to 6.15, 46.80 to 6.05, 46.83 to 6.10),
        deliveryDay = "FRIDAY",
        geoJson = null
    )

    private val allPaths = listOf(pathA, pathB)

    @Before
    fun setUp() {
        mockkStatic(Location::class)
        // Simple planar approximation: 1 degree of latitude/longitude == 111_000 meters.
        every {
            Location.distanceBetween(any(), any(), any(), any(), any())
        } answers {
            val lat1 = arg<Double>(0)
            val lon1 = arg<Double>(1)
            val lat2 = arg<Double>(2)
            val lon2 = arg<Double>(3)
            val results = arg<FloatArray>(4)
            val dLat = (lat2 - lat1) * 111_000.0
            val dLon = (lon2 - lon1) * 111_000.0
            results[0] = Math.sqrt(dLat * dLat + dLon * dLon).toFloat()
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(Location::class)
    }

    @Test
    fun `split city resolves to the path owning the customer street`() {
        val result = useCase(
            paths = allPaths,
            userCity = "Boujailles",
            userStreet = "Rue du Moulin",
            addressText = "12 Rue du Moulin, 25560 Boujailles",
            userLocation = 46.85 to 6.15
        )

        assertEquals(DeliveryEligibility.DELIVERABLE, result.eligibility)
        assertEquals("path-a", result.matchingPath?.id)
    }

    @Test
    fun `same split city resolves to the other path for a street of the other group`() {
        val result = useCase(
            paths = allPaths,
            userCity = "Boujailles",
            userStreet = "Chemin des Prés",
            addressText = "3 Chemin des Prés, 25560 Boujailles",
            userLocation = 46.85 to 6.15
        )

        assertEquals(DeliveryEligibility.DELIVERABLE, result.eligibility)
        assertEquals("path-b", result.matchingPath?.id)
    }

    /**
     * Non-regression test for the exclusion-filter bug: a path carrying street restrictions for one
     * of its cities used to become unusable for all its other cities. Frasne is listed on path A
     * with no restriction, so it must resolve to path A. This fails on the pre-fix implementation.
     */
    @Test
    fun `unrestricted city on a path that restricts another city is still deliverable`() {
        val result = useCase(
            paths = allPaths,
            userCity = "Frasne",
            userStreet = "Rue de la Gare",
            addressText = "5 Rue de la Gare, 25560 Frasne",
            userLocation = 46.85 to 6.17
        )

        assertEquals(DeliveryEligibility.DELIVERABLE, result.eligibility)
        assertEquals("path-a", result.matchingPath?.id)
    }

    @Test
    fun `unrestricted city on the other path is deliverable too`() {
        val result = useCase(
            paths = allPaths,
            userCity = "Arc-sous-Montenot",
            userStreet = "Route de Levier",
            addressText = "8 Route de Levier, 25270 Arc-sous-Montenot",
            userLocation = 46.80 to 6.05
        )

        assertEquals(DeliveryEligibility.DELIVERABLE, result.eligibility)
        assertEquals("path-b", result.matchingPath?.id)
    }

    @Test
    fun `split city with an unknown street refuses explicitly instead of guessing a path`() {
        val result = useCase(
            paths = allPaths,
            userCity = "Boujailles",
            userStreet = "Impasse des Lilas",
            addressText = "2 Impasse des Lilas, 25560 Boujailles",
            userLocation = 46.85 to 6.15
        )

        assertEquals(DeliveryEligibility.STREET_NOT_COVERED, result.eligibility)
        assertNull(result.matchingPath)
    }

    @Test
    fun `split city with no street resolved at all refuses rather than picking one path`() {
        val result = useCase(
            paths = allPaths,
            userCity = "Boujailles",
            userStreet = null,
            addressText = "25560 Boujailles",
            userLocation = 46.85 to 6.15
        )

        assertEquals(DeliveryEligibility.STREET_NOT_COVERED, result.eligibility)
        assertNull(result.matchingPath)
    }

    /**
     * The autocomplete flow never runs the geocoder, so `userStreet` is always null there and the
     * street has to be recognised inside the full address text.
     */
    @Test
    fun `street is matched from the address text when the geocoder gave no thoroughfare`() {
        val result = useCase(
            paths = allPaths,
            userCity = "Boujailles",
            userStreet = null,
            addressText = "7 rue de la gare 25560 Boujailles",
            userLocation = null
        )

        assertEquals(DeliveryEligibility.DELIVERABLE, result.eligibility)
        assertEquals("path-b", result.matchingPath?.id)
    }

    @Test
    fun `street matching ignores accents and case`() {
        val result = useCase(
            paths = allPaths,
            userCity = "Boujailles",
            userStreet = "CHEMIN DES PRES",
            addressText = null,
            userLocation = null
        )

        assertEquals(DeliveryEligibility.DELIVERABLE, result.eligibility)
        assertEquals("path-b", result.matchingPath?.id)
    }

    @Test
    fun `a street match wins over a whole-city match on another path`() {
        val cityWidePath = pathA.copy(
            id = "path-c",
            pathName = "Parcours C",
            cities = listOf(DeliveryCity("Boujailles", 25560))
        )

        val result = useCase(
            paths = listOf(cityWidePath, pathB),
            userCity = "Boujailles",
            userStreet = "Rue de la Gare",
            addressText = "1 Rue de la Gare, 25560 Boujailles",
            userLocation = null
        )

        assertEquals(DeliveryEligibility.DELIVERABLE, result.eligibility)
        assertEquals("path-b", result.matchingPath?.id)
    }

    @Test
    fun `city just outside every path but within range can ask for support`() {
        val result = useCase(
            paths = allPaths,
            userCity = "Bulle",
            userStreet = "Rue Principale",
            addressText = "4 Rue Principale, 25560 Bulle",
            // ~2 km from Boujailles' center, inside MAX_DISTANCE_FOR_PICKUP_METERS
            userLocation = 46.868 to 6.155
        )

        assertEquals(DeliveryEligibility.ASK_FOR_SUPPORT, result.eligibility)
        assertNull(result.matchingPath)
    }

    @Test
    fun `far away city is not eligible`() {
        val result = useCase(
            paths = allPaths,
            userCity = "Marseille",
            userStreet = "La Canebière",
            addressText = "1 La Canebière, 13001 Marseille",
            userLocation = 43.29 to 5.37
        )

        assertEquals(DeliveryEligibility.NOT_ELIGIBLE, result.eligibility)
        assertNull(result.matchingPath)
    }

    @Test
    fun `city and coordinates are recovered from the matched path when geocoding failed`() {
        val result = useCase(
            paths = allPaths,
            userCity = null,
            userStreet = null,
            addressText = "10 Route de Levier, 25270 Arc-sous-Montenot",
            userLocation = null
        )

        assertEquals(DeliveryEligibility.DELIVERABLE, result.eligibility)
        assertEquals("path-b", result.matchingPath?.id)
        assertEquals("Arc-sous-Montenot", result.resolvedCity)
        assertEquals(46.80 to 6.05, result.resolvedLocation)
    }

    /**
     * The city name must be a whole word in the address, so a longer word merely starting with it
     * is not a match.
     *
     * Note the limit of this guarantee, unchanged from the previous implementation: normalization
     * turns dashes into spaces, so a hyphenated commune like "Frasne-le-Haut" does contain the word
     * "Frasne" and would still match. Distinguishing those needs the postcode, which this matcher
     * does not consult.
     */
    @Test
    fun `city name is matched on word boundaries`() {
        val result = useCase(
            paths = listOf(pathA),
            userCity = null,
            userStreet = null,
            addressText = "1 Grande Rue, 25560 Frasnette",
            userLocation = null
        )

        assertEquals(DeliveryEligibility.NOT_ELIGIBLE, result.eligibility)
        assertNull(result.matchingPath)
    }

    @Test
    fun `no paths at all is not eligible rather than a crash`() {
        val result = useCase(
            paths = emptyList(),
            userCity = "Boujailles",
            userStreet = "Rue du Moulin",
            addressText = "12 Rue du Moulin, 25560 Boujailles",
            userLocation = 46.85 to 6.15
        )

        assertEquals(DeliveryEligibility.NOT_ELIGIBLE, result.eligibility)
        assertNull(result.matchingPath)
    }
}
