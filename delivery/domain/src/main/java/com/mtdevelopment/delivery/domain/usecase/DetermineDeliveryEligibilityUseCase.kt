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
    /**
     * At least one path covers this exact address. [DeliveryEligibilityResult.candidatePaths] holds
     * every path that qualifies — usually one, but several when the shop genuinely serves the
     * address on more than one tournée and the customer gets to pick a date.
     */
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
 * @property matchingPath The path to use when there is nothing to arbitrate; equal to the first
 *   entry of [candidatePaths]. Kept as a convenience for callers that only ever need one path.
 * @property candidatePaths Every path that serves this address, in path order. Empty unless
 *   [eligibility] is [DeliveryEligibility.DELIVERABLE]. More than one entry means the address is
 *   legitimately served by several tournées and the customer must be offered the choice — see
 *   [DetermineDeliveryEligibilityUseCase] for when that happens.
 * @property resolvedCity The customer's city, recovered from the matched path when geocoding
 *   could not supply it.
 * @property resolvedLocation The customer's coordinates, recovered from the matched city's center
 *   when geocoding could not supply them.
 */
data class DeliveryEligibilityResult(
    val eligibility: DeliveryEligibility,
    val matchingPath: DeliveryPath?,
    val candidatePaths: List<DeliveryPath>,
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
 * A street restriction is scoped to **one city of one path**, so it can only ever disqualify that
 * city. A path listing street restrictions for Boujailles still covers Frasne in full.
 *
 * ## Resolution
 *
 * Each (path, city) pair whose city matches falls into one of three tiers:
 *
 * - **street match** — the city restricts streets and one of them is in the address. Scored by the
 *   length of the matched label, so the most specific wins;
 * - **whole city** — the city carries no restriction at all;
 * - **miss** — the city restricts streets and none matched.
 *
 * A street match always beats a whole-city match: the shop went to the trouble of naming that
 * street, so it is the more deliberate answer. Within the winning tier, every path that ties is
 * returned in [DeliveryEligibilityResult.candidatePaths] and the **customer** picks, by choosing a
 * delivery date. Nothing here resolves a tie by list order — that order comes from Firestore
 * document iteration, so silently taking the first would hand the same customer a different
 * tournée from one session to the next.
 *
 * The scoring exists for one concrete case: a split city where one path lists "Rue du Moulin" and
 * the other "Rue du Moulin Neuf". Street labels are matched loosely inside the address text, so an
 * address on Moulin Neuf contains both labels. The longer label is the one the customer actually
 * lives on. The reverse never collides — "Rue du Moulin Neuf" is not contained in "3 rue du
 * Moulin".
 *
 * When no tier matched but at least one city missed on its streets, the address is a new street, a
 * hamlet or a typo: [DeliveryEligibility.STREET_NOT_COVERED], whether that city sits on one path or
 * five. A street list is a deliberate statement that the rest of the city is not served, so there
 * is nothing safe to fall back on. Otherwise proximity decides: within
 * [MAX_DISTANCE_FOR_PICKUP_METERS] the customer may ask for support, beyond it they are not
 * eligible.
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

    /** One (path, city) pair that matched on the city name. */
    private data class CityMatch(
        val path: DeliveryPath,
        val cityName: String,
        val cityLocation: Pair<Double, Double>?,
        val tier: Tier,
        /** Length of the matched street label; 0 outside [Tier.STREET]. */
        val score: Int
    )

    private enum class Tier { STREET, WHOLE_CITY, MISS }

    operator fun invoke(
        paths: List<DeliveryPath>,
        userCity: String?,
        userStreet: String?,
        addressText: String?,
        userLocation: Pair<Double, Double>?
    ): DeliveryEligibilityResult {
        var closestDistance = Double.MAX_VALUE
        val matches = mutableListOf<CityMatch>()

        for (path in paths) {
            path.cities.forEachIndexed { index, city ->
                // The city's own coordinate first, and `locations` only as a fallback for paths
                // cached before the coordinate travelled with the city. Reading the parallel list
                // by index is the fragile half: it is a positional mirror of `cities`, so anything
                // producing a shorter one silently attributes the wrong center — and therefore the
                // wrong pickup distance — to every city after the gap.
                val cityLocation = city.location ?: path.locations?.getOrNull(index)

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

                if (!matchesCity(city, userCity, addressText)) return@forEachIndexed

                val matchedStreetLength = longestMatchingStreet(city.streets, userStreet, addressText)
                val tier = when {
                    city.coversWholeCity -> Tier.WHOLE_CITY
                    matchedStreetLength > 0 -> Tier.STREET
                    else -> Tier.MISS
                }

                matches.add(
                    CityMatch(
                        path = path,
                        cityName = city.name,
                        cityLocation = cityLocation,
                        tier = tier,
                        score = if (tier == Tier.STREET) matchedStreetLength else 0
                    )
                )
            }
        }

        val candidateMatches = selectCandidates(matches)
        // A path listing the same city twice must not look like a choice between two tournées.
        val candidatePaths = candidateMatches.map { it.path }.distinctBy { it.id }

        // Recover what geocoding could not give us, preferably from the path we actually chose.
        val recoverySource = candidateMatches.firstOrNull() ?: matches.firstOrNull()
        val resolvedCity = userCity?.takeUnless { it.isBlank() } ?: recoverySource?.cityName
        val resolvedLocation = userLocation ?: recoverySource?.cityLocation
        if (userLocation == null && resolvedLocation != null) {
            // An exact city match is as good as being on the path for the proximity check.
            closestDistance = 0.0
        }

        val eligibility = when {
            candidatePaths.isNotEmpty() -> DeliveryEligibility.DELIVERABLE
            matches.any { it.tier == Tier.MISS } -> DeliveryEligibility.STREET_NOT_COVERED
            closestDistance <= MAX_DISTANCE_FOR_PICKUP_METERS -> DeliveryEligibility.ASK_FOR_SUPPORT
            else -> DeliveryEligibility.NOT_ELIGIBLE
        }

        return DeliveryEligibilityResult(
            eligibility = eligibility,
            matchingPath = candidatePaths.firstOrNull(),
            candidatePaths = candidatePaths,
            resolvedCity = resolvedCity,
            resolvedLocation = resolvedLocation
        )
    }

    /**
     * Keeps the most specific tier that produced a match, then everything tied at the top of it.
     * Returning several is not a failure — it is the shop genuinely covering the address twice.
     */
    private fun selectCandidates(matches: List<CityMatch>): List<CityMatch> {
        val streetMatches = matches.filter { it.tier == Tier.STREET }
        if (streetMatches.isNotEmpty()) {
            val bestScore = streetMatches.maxOf { it.score }
            return streetMatches.filter { it.score == bestScore }
        }
        return matches.filter { it.tier == Tier.WHOLE_CITY }
    }

    private fun matchesCity(
        city: DeliveryCity,
        userCity: String?,
        addressText: String?
    ): Boolean = isSameCity(userCity, city.name) ||
            containsNormalized(addressText, city.name, wholeWord = true)

    /**
     * Length of the longest street label of [streets] that matches the address, or 0 when none do.
     * The length is measured on the normalized label so it stays comparable across accents and
     * punctuation.
     */
    private fun longestMatchingStreet(
        streets: List<String>,
        userStreet: String?,
        addressText: String?
    ): Int = streets
        .filter { street ->
            isSameCity(userStreet, street) || containsNormalized(addressText, street, wholeWord = false)
        }
        .maxOfOrNull { it.normalizeCityName().length }
        ?: 0

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
