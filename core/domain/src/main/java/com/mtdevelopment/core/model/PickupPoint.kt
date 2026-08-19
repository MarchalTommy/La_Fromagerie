package com.mtdevelopment.core.model

import com.mtdevelopment.core.domain.toLocalDate
import kotlinx.serialization.Serializable

/**
 * What kind of place an order can be collected at.
 *
 * The two kinds are shaped differently on purpose, which is why they are one type with a
 * discriminator rather than two: the shop recurs (it opens on the same weekdays every
 * week), a market does not. Markets come round roughly once a month with no usable
 * weekly rhythm, so each one is entered as its own dated point.
 */
enum class PickupPointType {
    /** The shop itself: recurring opening days, minus explicit closures. */
    SHOP,

    /** One market on one date, at its own address. */
    MARKET;

    companion object {
        /** Reads a stored value, falling back to [SHOP] for anything unknown or absent. */
        fun fromStoredValue(value: String?): PickupPointType =
            runCatching { valueOf(value.orEmpty()) }.getOrDefault(SHOP)
    }
}

/**
 * A place and time at which the customer can collect an order.
 *
 * Deliberately **not** modelled as a `DeliveryPath`. A path is geocoded, cached in Room and
 * matched against the customer's address street by street; none of that means anything for
 * a counter the customer walks up to. Sharing the type would have dragged all of it along.
 *
 * @property id Firestore document id. Blank for a point that has never been saved.
 * @property type Shop or market — decides which of the fields below carry meaning.
 * @property label Name shown to the customer, e.g. "Marché de Pontarlier".
 * @property address Postal address, geocoded for the map pin.
 * @property latitude Geocoded latitude, null while the address has not been resolved.
 * @property longitude Geocoded longitude, null while the address has not been resolved.
 * @property timeRange Opening window shown to the customer, e.g. "8h-13h". Displayed only —
 *   the customer picks a day, never a slot.
 * @property openingDays [PickupPointType.SHOP] only: `DayOfWeek` names the shop opens on.
 * @property closedDates [PickupPointType.SHOP] only: `dd/MM/yyyy` dates to skip despite
 *   falling on an opening day — holidays, and the days the owner is simply away. Without
 *   these, a recurring shop could not be closed at all.
 * @property date [PickupPointType.MARKET] only: the single `dd/MM/yyyy` date it happens on.
 */
@Serializable
data class PickupPoint(
    val id: String = "",
    val type: PickupPointType = PickupPointType.SHOP,
    val label: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timeRange: String = "",
    val openingDays: List<String> = emptyList(),
    val closedDates: List<String> = emptyList(),
    val date: String? = null
) {

    /**
     * Whether this point is complete enough to be worth storing.
     *
     * A shop with no opening day and a market with no date are both invisible to the
     * customer — saving them would silently produce a point nobody can ever order from,
     * which reads as a bug rather than as an empty configuration.
     */
    val canBeSaved: Boolean
        get() = label.isNotBlank() && address.isNotBlank() && when (type) {
            PickupPointType.SHOP -> openingDays.isNotEmpty()
            PickupPointType.MARKET -> date?.toLocalDate() != null
        }
}
