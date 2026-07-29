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
 */
@Serializable
data class DeliveryCity(
    val name: String,
    val postcode: Int,
    val streets: List<String> = emptyList()
) {
    /** True when this path covers the entire city, with no street-level restriction. */
    val coversWholeCity: Boolean
        get() = streets.isEmpty()
}
