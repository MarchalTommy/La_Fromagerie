package com.mtdevelopment.admin.presentation.model

import com.mtdevelopment.core.model.PickupPoint

/**
 * UI state of the pickup point management screens.
 *
 * @property points Every configured point: the shop first, then market dates in
 *   chronological order.
 * @property isLoading True while a read or a write is in flight.
 * @property error Message to surface, or null. Set on a failed read or write so a silent
 *   no-op is never mistaken for "there is nothing configured".
 */
data class PickupPointsState(
    val points: List<PickupPoint> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
