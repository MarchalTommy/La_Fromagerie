package com.mtdevelopment.admin.data.model

import androidx.annotation.Keep
import com.mtdevelopment.core.model.DeliveryPath

/**
 * Data Transfer Object (DTO) for delivery paths, used for Firestore serialization.
 *
 * Firestore's `set`/`add` use their own reflective POJO mapper, not kotlinx.serialization, so the
 * **Kotlin property names are the Firestore field names**. That is why they are snake_case here.
 *
 * [city_entries] is the canonical shape: one entry per city, carrying that city's street
 * restriction. [cities] and [postcodes] are a one-way mirror derived from it in
 * [toDataDeliveryPath] — written so that any client build still reading the old parallel-array
 * shape keeps working, never read back by current code. Keeping the projection in a single mapper
 * is what stops the two shapes from drifting apart.
 *
 * The legacy path-level `streets` field is deliberately **not** written: it meant "this whole path
 * only serves these streets", which older clients apply as an exclusion filter across every city of
 * the path. Writing it would make the unrestricted cities of a split path undeliverable there.
 */
@Keep
data class DataDeliveryPath(
    val id: String = "",
    val path_name: String? = null,
    val delivery_day: String = "",
    val delivery_frequency: String = "WEEKLY",
    val city_entries: List<DataDeliveryCity>? = null,
    val cities: List<String>? = null,
    val postcodes: List<Int>? = null
)

/**
 * One city of a path, with its street restriction. Empty [streets] means the whole city is covered.
 */
@Keep
data class DataDeliveryCity(
    val city: String = "",
    val postcode: Int = 0,
    val streets: List<String> = emptyList()
)

/**
 * Maps a domain [DeliveryPath] to its Firestore DTO, deriving the legacy parallel arrays from the
 * canonical city entries.
 */
fun DeliveryPath.toDataDeliveryPath() = DataDeliveryPath(
    id = id,
    path_name = pathName,
    delivery_day = deliveryDay,
    delivery_frequency = deliveryFrequency,
    city_entries = availableCities.map {
        DataDeliveryCity(
            city = it.name,
            postcode = it.postcode,
            streets = it.streets
        )
    },
    cities = availableCities.map { it.name },
    postcodes = availableCities.map { it.postcode }
)
