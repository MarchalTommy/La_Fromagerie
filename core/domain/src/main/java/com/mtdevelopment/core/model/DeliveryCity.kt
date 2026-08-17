package com.mtdevelopment.core.model

import kotlinx.serialization.Serializable

/**
 * One city covered by a [DeliveryPath], with an optional street-level restriction.
 *
 * The restriction is carried per (path, city) pair — not per path — so that a single city can be
 * split between two paths by street while the other cities of those paths stay fully covered.
 * Concretely, "Boujailles" can appear on path A restricted to one group of streets and on path B
 * restricted to another, while "Frasne" appears on path A with no restriction at all.
 *
 * @property name City name as entered by the admin (accents and dashes preserved; comparison is
 *   always done through `isSameCity`/`normalizeCityName`).
 * @property postcode French postal code.
 * @property streets Street allow-list for this city **on this path**. Empty means the whole city
 *   is covered — which is the common case and must stay the default.
 * @property latitude Center of the commune, resolved once when the path is saved. Null on a path
 *   that has not been saved since this field existed; the reader geocodes those on the fly.
 * @property longitude See [latitude].
 */
@Serializable
data class DeliveryCity(
    val name: String,
    val postcode: Int,
    val streets: List<String> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    /** True when this path covers the entire city, with no street-level restriction. */
    val coversWholeCity: Boolean
        get() = streets.isEmpty()

    /**
     * The commune's center, or null if it has never been resolved.
     *
     * Carrying the coordinate on the city rather than in a parallel list on the path is what removes
     * the alignment hazard: `DeliveryPath.locations` is a positional mirror of `cities`, and
     * `DetermineDeliveryEligibilityUseCase` indexes one by the other, so any code path producing a
     * shorter `locations` attributes the wrong center to every city after the gap.
     */
    val location: Pair<Double, Double>?
        get() = if (latitude != null && longitude != null) latitude to longitude else null
}
