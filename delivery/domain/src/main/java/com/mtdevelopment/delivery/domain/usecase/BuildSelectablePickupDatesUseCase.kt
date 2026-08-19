package com.mtdevelopment.delivery.domain.usecase

import com.mtdevelopment.core.domain.toLocalDate
import com.mtdevelopment.core.domain.toStoredDate
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
 *
 * There is no `isPastDeadline` here, unlike [SelectableDeliveryDate]. Every date this use case
 * returns can be ordered on; see the class KDoc for why the two pickers differ.
 */
data class SelectablePickupDate(
    val date: LocalDate,
    val pointId: String,
    val pointLabel: String,
    val pointAddress: String,
    val timeRange: String
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
 * Also unlike the delivery picker, a date whose window has closed is **not returned at all**.
 * The delivery picker greys such a date inside a calendar grid, where a missing day would
 * leave a hole; this one renders a short vertical list of cards, and a dead card at the top of
 * it is simply the first thing the customer reads. The list starts at the next date that can
 * actually be ordered, and [limit] then counts real options rather than being spent on
 * closed ones.
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

        val from = firstOrderableDate(now)
        return points
            .flatMap { point -> datesFor(point, from, limit) }
            .sortedBy { it.date }
            .take(limit)
    }

    /**
     * The earliest date still inside its order window.
     *
     * Walks forward from today using [isPastDeadline] itself rather than re-deriving the
     * cut-off arithmetic, so the two can never disagree about where the boundary is. It
     * terminates after at most two steps -- the predicate is monotonic in the date -- and the
     * bound is there only to make that impossible to get wrong later.
     */
    private fun firstOrderableDate(now: LocalDateTime): LocalDate {
        var candidate = now.toLocalDate()
        var steps = 0
        while (isPastDeadline(candidate, now) && steps < MAX_DAYS_SCANNED) {
            candidate = candidate.plusDays(1)
            steps++
        }
        return candidate
    }

    private fun datesFor(
        point: PickupPoint,
        from: LocalDate,
        limit: Int
    ): List<SelectablePickupDate> = when (point.type) {
        PickupPointType.MARKET -> {
            // A market whose window has closed is simply gone -- [from] is already the first
            // orderable day -- and there is no next occurrence to fall back on.
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
        timeRange = timeRange
    )

    /** Same rule as delivery: orders close the day before, at noon. */
    private fun isPastDeadline(date: LocalDate, now: LocalDateTime): Boolean =
        now.isAfter(date.minusDays(1).atTime(ORDER_CUTOFF_HOUR, 0))

}
