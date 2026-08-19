package com.mtdevelopment.delivery.data.model.response.firestore

import androidx.annotation.Keep
import com.mtdevelopment.core.model.DeliveryCity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Read shape of a `delivery_paths` document.
 *
 * Two on-disk shapes are accepted, because documents written before the split-city feature only
 * have the parallel arrays:
 * - [cityEntries] (`city_entries`): canonical, one entry per city with its own street restriction.
 * - [cities] + [postcodes]: legacy parallel arrays, zipped positionally.
 *
 * [toDeliveryCities] applies the precedence. Documents convert to the new shape the first time the
 * admin saves them; no data migration is required.
 *
 * The legacy path-level `streets` field is intentionally absent: it could not express a
 * per-city restriction, and applying it path-wide is exactly what made the unrestricted cities of a
 * path undeliverable.
 */
@Keep
@Serializable
data class DataDeliveryPathsResponse(
    @SerialName("id")
    val id: String = "",
    @SerialName("path_name")
    val path_name: String? = null,
    @SerialName("cities")
    val cities: List<String>? = null,
    @SerialName("delivery_day")
    val deliveryDay: String = "",
    @SerialName("delivery_frequency")
    val deliveryFrequency: String = "WEEKLY",
    @SerialName("postcodes")
    val postcodes: List<Int>? = null,
    @SerialName("city_entries")
    val cityEntries: List<DataDeliveryCityResponse>? = null
)

/**
 * @property lat Center of the commune, written by the admin app when the path is saved. Absent on
 *   documents last written before this field existed — the reader geocodes those on the fly, so the
 *   addition is purely additive and needs no migration.
 * @property lng See [lat].
 */
@Keep
@Serializable
data class DataDeliveryCityResponse(
    @SerialName("city")
    val city: String = "",
    @SerialName("postcode")
    val postcode: Int = 0,
    @SerialName("streets")
    val streets: List<String> = emptyList(),
    @SerialName("lat")
    val lat: Double? = null,
    @SerialName("lng")
    val lng: Double? = null
)

/**
 * Resolves the document to domain cities, preferring the canonical `city_entries` shape and
 * falling back to zipping the legacy parallel arrays. Returns an empty list when neither shape
 * carries usable data, which the repository treats as "skip this path".
 */
fun DataDeliveryPathsResponse.toDeliveryCities(): List<DeliveryCity> {
    cityEntries?.takeIf { it.isNotEmpty() }?.let { entries ->
        return entries.map { entry ->
            DeliveryCity(
                name = entry.city,
                postcode = entry.postcode,
                streets = entry.streets,
                latitude = entry.lat,
                longitude = entry.lng
            )
        }
    }

    val names = cities ?: return emptyList()
    val codes = postcodes ?: return emptyList()
    return names.zip(codes).map { (name, postcode) ->
        DeliveryCity(name = name, postcode = postcode)
    }
}
