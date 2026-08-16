package com.mtdevelopment.delivery.data.model.response.firestore

import com.mtdevelopment.core.model.PickupPoint
import com.mtdevelopment.core.model.PickupPointType

/**
 * Read DTO for a `pickup_points` document.
 *
 * Populated by hand from the raw document map, so **the raw Firestore keys are the contract**
 * here — not any annotation. Same discipline as [DataDeliveryPathsResponse]: the admin flavour
 * writes these documents through its own POJO DTO, and the two only agree because both use the
 * stored snake_case names.
 */
data class DataPickupPointResponse(
    val id: String,
    val type: String?,
    val label: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val timeRange: String?,
    val openingDays: List<String>,
    val closedDates: List<String>,
    val date: String?
)

/**
 * Maps the raw document to the read DTO.
 *
 * Numbers go through [Number] rather than a direct cast: Firestore hands integers back as
 * `Long`, and a generic cast is erased at runtime so it succeeds here and fails later at the
 * point of use.
 */
internal fun Map<String, Any?>?.toPickupPointResponse(documentId: String) = DataPickupPointResponse(
    id = documentId,
    type = this?.get("type")?.toString(),
    label = this?.get("label")?.toString(),
    address = this?.get("address")?.toString(),
    latitude = (this?.get("latitude") as? Number)?.toDouble(),
    longitude = (this?.get("longitude") as? Number)?.toDouble(),
    timeRange = this?.get("time_range")?.toString(),
    openingDays = (this?.get("opening_days") as? List<*>)?.mapNotNull { it?.toString() }
        ?: emptyList(),
    closedDates = (this?.get("closed_dates") as? List<*>)?.mapNotNull { it?.toString() }
        ?: emptyList(),
    date = this?.get("date")?.toString()
)

/** Resolves the read DTO to the domain model, defaulting anything unknown rather than failing. */
fun DataPickupPointResponse.toPickupPoint() = PickupPoint(
    id = id,
    type = PickupPointType.fromStoredValue(type),
    label = label.orEmpty(),
    address = address.orEmpty(),
    latitude = latitude,
    longitude = longitude,
    timeRange = timeRange.orEmpty(),
    openingDays = openingDays,
    closedDates = closedDates,
    date = date
)
