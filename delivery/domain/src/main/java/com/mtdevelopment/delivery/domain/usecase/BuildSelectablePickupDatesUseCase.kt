package com.mtdevelopment.delivery.domain.usecase

import com.mtdevelopment.core.domain.toLocalDate
import com.mtdevelopment.core.model.PickupPoint
import com.mtdevelopment.core.model.PickupPointType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * One date the customer may collect an order on, and the point it would be collected at.
 *
 * @property pointId Which pickup point serves this date. Two markets on the same day are two
 *   separate entries: unlike delivery tournées, the point is what the customer is choosing.
 * @property isPastDeadline The order window closed (see [ORDER_CUTOFF_HOUR]). Listed but not
 *   selectable, matching the delivery picker — hiding it would make the list shift silently
 *   and read as a bug.
 */
data class SelectablePickupDate(
    val date: LocalDate,
    val pointId: String,
    val pointLabel: String,
    val pointAddress: String,
    val timeRange: String,
    val isPastDeadline: Boolean
)

/**
 * How far ahead to scan for shop opening days before giving up. Only exists so a point with no
 * usable opening day cannot spin forever.
 */
private const val MAX_DAYS_SCANNED = 365

/**
 * Builds the collection dates offered to a customer, merged across every point of the chosen
 * kind.
 *
 * Mirrors [BuildSelectableDeliveryDatesUseCase] deliberately, down to sharing
 * [ORDER_CUTOFF_HOUR]: a customer should not have to learn that collecting closes at a
 * different time from being delivered, and two cut-off rules would drift apart the first time
 * one of them changed.
 *
 * The two kinds of point produce dates differently, which is the whole reason they are
 * distinct: the shop recurs on its opening days, a market happens once. Both are handled here
 * so callers pass whichever points match the mode the customer selected and get one merged,
 * chronological list back.
 *
 * Unlike the delivery picker, dates are **not** deduplicated. Two markets on the same Saturday
 * are two genuinely different places to go, and collapsing them would hide one.
 *
 * @param points Points to offer. Empty yields an empty list.
 * @param now Injected rather than read from the system clock so the cut-off is testable.
 * @param limit How many dates to return.
 */
class BuildSelectablePickupDatesUseCase {

    operator fun invoke(
        points: List<PickupPoint>,
        now: LocalDateTime,
        limit: Int = 4
    ): List<SelectablePickupDate> {
        if (points.isEmpty() || limit <= 0) return emptyList()

        return points
            .flatMap { point -> datesFor(point, now.toLocalDate(), limit) }
            .sortedBy { it.date }
            .take(limit)
            .map { it.copy(isPastDeadline = isPastDeadline(it.date, now)) }
    }

    private fun datesFor(
        point: PickupPoint,
        from: LocalDate,
        limit: Int
    ): List<SelectablePickupDate> = when (point.type) {
        PickupPointType.MARKET -> {
            // A market that has already happened is simply gone; there is no next occurrence
            // to fall back on.
            point.date?.toLocalDate()
                ?.takeIf { !it.isBefore(from) }
                ?.let { listOf(point.toSelectable(it)) }
                .orEmpty()
        }

        PickupPointType.SHOP -> {
            val openDays = point.openingDays.mapNotNull { day ->
                runCatching { DayOfWeek.valueOf(day.uppercase()) }.getOrNull()
            }.toSet()

            if (openDays.isEmpty()) {
                emptyList()
            } else {
                // Closures are compared as stored strings rather than parsed dates: they are
                // written by the same dd/MM/yyyy formatter, and a date that fails to parse
                // should not silently reopen a day the shop declared closed.
                val closed = point.closedDates.toSet()
                val dates = mutableListOf<SelectablePickupDate>()
                var current = from
                var scanned = 0
                while (dates.size < limit && scanned < MAX_DAYS_SCANNED) {
                    if (current.dayOfWeek in openDays && !closed.contains(current.toStoredDate())) {
                        dates.add(point.toSelectable(current))
                    }
                    current = current.plusDays(1)
                    scanned++
                }
                dates
            }
        }
    }

    private fun PickupPoint.toSelectable(date: LocalDate) = SelectablePickupDate(
        date = date,
        pointId = id,
        pointLabel = label,
        pointAddress = address,
        timeRange = timeRange,
        isPastDeadline = false
    )

    /** Same rule as delivery: orders close the day before, at noon. */
    private fun isPastDeadline(date: LocalDate, now: LocalDateTime): Boolean =
        now.isAfter(date.minusDays(1).atTime(ORDER_CUTOFF_HOUR, 0))

    /** Formats a date the way closures are stored, so the comparison is string-to-string. */
    private fun LocalDate.toStoredDate(): String =
        "%02d/%02d/%04d".format(dayOfMonth, monthValue, year)
}
