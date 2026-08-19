package com.mtdevelopment.admin.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.Exclude
import com.mtdevelopment.core.model.PickupPoint
import com.mtdevelopment.core.model.PickupPointType

/**
 * Data Transfer Object for a pickup point, used for Firestore serialization.
 *
 * Firestore's `set`/`add` use their own reflective POJO mapper, not kotlinx.serialization, so
 * the **Kotlin property names are the Firestore field names** — hence the snake_case, and hence
 * the fact that a `@SerialName` here would be purely decorative. Same rule as
 * [DataDeliveryPath]; get it backwards and the document silently gains camelCase keys nothing
 * reads.
 *
 * [type] is stored as the enum name rather than the enum itself so an older build reading a
 * type it does not know degrades through [PickupPointType.fromStoredValue] instead of failing
 * to map the document.
 *
 * Every field carries a default: Firestore's mapper needs a no-arg construction path, and a
 * document written by a future version with fewer keys must still map.
 *
 * [id] is `@get:Exclude`d from the write. The identity of a point IS its document id, and
 * `add()` only learns that id after the document exists — so without this every created point
 * stored an `id: ""` field that nothing could ever read as anything but wrong. Reads are
 * unaffected: both sides map the id off `DocumentSnapshot.id`, never off the document body.
 */
@Keep
data class DataPickupPoint(
    @get:Exclude val id: String = "",
    val type: String = PickupPointType.SHOP.name,
    val label: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val time_range: String = "",
    val opening_days: List<String> = emptyList(),
    val closed_dates: List<String> = emptyList(),
    val date: String? = null
)

/** Maps a domain [PickupPoint] to its Firestore DTO. */
fun PickupPoint.toDataPickupPoint() = DataPickupPoint(
    id = id,
    type = type.name,
    label = label,
    address = address,
    latitude = latitude,
    longitude = longitude,
    time_range = timeRange,
    // The fields that do not apply to this kind of point are written empty rather than left
    // to carry stale values from a point that changed type mid-edit.
    opening_days = if (type == PickupPointType.SHOP) openingDays else emptyList(),
    closed_dates = if (type == PickupPointType.SHOP) closedDates else emptyList(),
    date = if (type == PickupPointType.MARKET) date else null
)

/** Maps a Firestore DTO back to the domain model. */
fun DataPickupPoint.toPickupPoint() = PickupPoint(
    id = id,
    type = PickupPointType.fromStoredValue(type),
    label = label,
    address = address,
    latitude = latitude,
    longitude = longitude,
    timeRange = time_range,
    openingDays = opening_days,
    closedDates = closed_dates,
    date = date
)
