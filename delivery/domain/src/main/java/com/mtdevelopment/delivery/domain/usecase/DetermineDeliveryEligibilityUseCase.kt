package com.mtdevelopment.delivery.domain.usecase

import com.mtdevelopment.core.domain.calculateDistance
import com.mtdevelopment.core.domain.isSameCity
import com.mtdevelopment.core.domain.normalizeCityName
import com.mtdevelopment.core.model.DeliveryCity
import com.mtdevelopment.delivery.domain.model.DeliveryPath

/**
 * Maximum distance (in meters) within which a customer who is not on any path may still ask the
 * shop for manual support.
 */
const val MAX_DISTANCE_FOR_PICKUP_METERS = 5000.0

/**
 * Outcome of matching a customer address against the delivery paths.
 */
enum class DeliveryEligibility {
    /** A path covers this exact address; [DeliveryEligibilityResult.matchingPath] is set. */
    DELIVERABLE,

    /**
     * The customer's city is covered, but only by paths that restrict it to a street list, and
     * none of those streets matched the address. The customer cannot pick a path by hand, so
     * guessing would send the van on the wrong day — we hand them to the shop instead.
     */
    STREET_NOT_COVERED,

    /** No path covers the city, but the customer is close enough to request manual support. */
    ASK_FOR_SUPPORT,

    /** Too far from any delivery path. */
    NOT_ELIGIBLE
}

/**
 * @property eligibility What the UI should do next.
 * @property matchingPath Set only when [eligibility] is [DeliveryEligibility.DELIVERABLE].
 * @property resolvedCity The customer's city, recovered from the matched path when geocoding
 *   could not supply it.
 * @property resolvedLocation The customer's coordinates, recovered from the matched city's center
 *   when geocoding could not supply them.
 */
data class DeliveryEligibilityResult(
    val eligibility: DeliveryEligibility,
    val matchingPath: DeliveryPath?,
    val resolvedCity: String?,
    val resolvedLocation: Pair<Double, Double>?
)

/**
 * Decides which delivery path — if any — serves a customer address.
 *
 * This used to live as a private function inside the delivery Composables, duplicated between the
 * typed-address flow and the GPS flow, and was therefore untestable. It is the single decision
 * point now; both flows geocode and then call this.
 *
 * The matching rule that matters, and the one the previous implementation got backwards: a street
 * restriction is scoped to **one city of one path**, so it can only ever disqualify that city. A
 * path listing street restrictions for Boujailles still covers Frasne in full. Previously any path
 * carrying a non-empty street list was excluded outright unless the street matched, which made
 * every other city of that path undeliverable.
 *
 * Resolution order:
 * 1. A path whose matching city restricts streets **and** whose street list contains the customer's
 *    street. This is the most specific answer, so it wins over an unrestricted match.
 * 2. A path whose matching city carries no street restriction (the whole city is covered).
 * 3. If the city is covered only by street-restricted entries and none matched, the address is a
 *    new street, a hamlet or a typo in a split city: [DeliveryEligibility.STREET_NOT_COVERED].
 * 4. Otherwise fall back to proximity: within [MAX_DISTANCE_FOR_PICKUP_METERS] the customer may
 *    ask for support, beyond it they are not eligible.
 *
 * All inputs are already-geocoded values so this stays a pure function, free of Android's
 * `Geocoder`.
 *
 * @param paths Every known delivery path.
 * @param userCity City resolved by the geocoder, if any.
 * @param userStreet Street (`Address.thoroughfare`) resolved by the geocoder, if any. Null on the
 *   autocomplete flow, which never runs the geocoder — [addressText] carries the street there.
 * @param addressText The full address as typed or as returned by autocomplete. Used both to match
 *   a city the geocoder missed and as the street-matching fallback.
 * @param userLocation Customer coordinates, if any. Without them no proximity check is possible.
 */
class DetermineDeliveryEligibilityUseCase {

    operator fun invoke(
        paths: List<DeliveryPath>,
        userCity: String?,
        userStreet: String?,
        addressText: String?,
        userLocation: Pair<Double, Double>?
    ): DeliveryEligibilityResult {
        var resolvedCity = userCity
        var resolvedLocation = userLocation
        var closestDistance = Double.MAX_VALUE

        // Paths whose matching city restricts streets AND whose street list contains the address.
        val streetMatchedPaths = mutableListOf<DeliveryPath>()
        // Paths whose matching city has no street restriction at all.
        val wholeCityPaths = mutableListOf<DeliveryPath>()
        // Paths that cover the city but only on streets that did not match.
        val streetMissedPaths = mutableListOf<DeliveryPath>()

        // Recovery values, used when the geocoder gave us neither city nor coordinates.
        var recoveredCity: String? = null
        var recoveredLocation: Pair<Double, Double>? = null

        for (path in paths) {
            path.cities.forEachIndexed { index, city ->
                val cityLocation = path.locations?.getOrNull(index)

                if (userLocation != null && cityLocation != null) {
                    val distance = calculateDistance(
                        userLocation.first,
                        userLocation.second,
                        cityLocation.first,
                        cityLocation.second
                    ).toDouble()
                    if (distance < closestDistance) {
                        closestDistance = distance
                    }
                }

                if (!matchesCity(city, resolvedCity, addressText)) return@forEachIndexed

                if (recoveredCity == null) {
                    recoveredCity = city.name
                    recoveredLocation = cityLocation
                }

                when {
                    city.coversWholeCity -> wholeCityPaths.add(path)
                    matchesStreet(city.streets, userStreet, addressText) ->
                        streetMatchedPaths.add(path)

                    else -> streetMissedPaths.add(path)
                }
            }
        }

        // Recover what geocoding could not give us from the city we matched on.
        if (resolvedCity.isNullOrBlank()) {
            resolvedCity = recoveredCity
        }
        if (resolvedLocation == null && recoveredLocation != null) {
            resolvedLocation = recoveredLocation
            // An exact city match is as good as being on the path for the proximity check.
            closestDistance = 0.0
        }

        val matchingPath = streetMatchedPaths.firstOrNull() ?: wholeCityPaths.firstOrNull()

        val eligibility = when {
            matchingPath != null -> DeliveryEligibility.DELIVERABLE
            streetMissedPaths.isNotEmpty() -> DeliveryEligibility.STREET_NOT_COVERED
            closestDistance <= MAX_DISTANCE_FOR_PICKUP_METERS -> DeliveryEligibility.ASK_FOR_SUPPORT
            else -> DeliveryEligibility.NOT_ELIGIBLE
        }

        return DeliveryEligibilityResult(
            eligibility = eligibility,
            matchingPath = matchingPath,
            resolvedCity = resolvedCity,
            resolvedLocation = resolvedLocation
        )
    }

    private fun matchesCity(
        city: DeliveryCity,
        userCity: String?,
        addressText: String?
    ): Boolean = isSameCity(userCity, city.name) || containsNormalized(addressText, city.name, wholeWord = true)

    private fun matchesStreet(
        streets: List<String>,
        userStreet: String?,
        addressText: String?
    ): Boolean = streets.any { street ->
        isSameCity(userStreet, street) || containsNormalized(addressText, street, wholeWord = false)
    }

    /**
     * Accent- and case-insensitive containment. City names are matched on word boundaries so that
     * "Frasne" does not match "Frasne-le-Château"; street names are matched loosely because the
     * address text around them varies ("12 rue du Moulin, 25560 Boujailles").
     */
    private fun containsNormalized(
        haystack: String?,
        needle: String,
        wholeWord: Boolean
    ): Boolean {
        if (haystack == null) return false
        val normalizedHaystack = haystack.normalizeCityName()
        val normalizedNeedle = needle.normalizeCityName()
        if (normalizedHaystack.isEmpty() || normalizedNeedle.isEmpty()) return false
        return if (wholeWord) {
            "\\b${Regex.escape(normalizedNeedle)}\\b".toRegex().containsMatchIn(normalizedHaystack)
        } else {
            normalizedHaystack.contains(normalizedNeedle)
        }
    }
}
