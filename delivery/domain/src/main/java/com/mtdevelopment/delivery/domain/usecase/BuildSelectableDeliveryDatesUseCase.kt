package com.mtdevelopment.delivery.domain.usecase

import com.mtdevelopment.delivery.domain.model.DeliveryPath
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * One delivery date the customer may pick, and the tournée that would serve it.
 *
 * @property pathId Path the date belongs to. The customer picking a date is how they pick a path
 *   when several tournées serve their address; nothing else records the choice.
 * @property isPastDeadline The order window closed (see [ORDER_CUTOFF_HOUR]). The date is still
 *   listed — hiding it would make the list silently shift and look like a bug — but it cannot be
 *   selected.
 */
data class SelectableDeliveryDate(
    val date: LocalDate,
    val pathId: String,
    val pathName: String,
    val isPastDeadline: Boolean
)

/** Orders close the day before delivery, at noon. */
const val ORDER_CUTOFF_HOUR = 12

/** Day used when a path carries a [DeliveryPath.deliveryDay] that does not parse. */
private val FALLBACK_DELIVERY_DAY = DayOfWeek.FRIDAY

/**
 * How far ahead to scan for occurrences before giving up. A biweekly path needs eight weeks to
 * yield four dates; this bound only exists so a nonsense frequency cannot spin forever.
 */
private const val MAX_DAYS_SCANNED = 365

/**
 * Builds the delivery dates offered to a customer, merged across every path that serves them.
 *
 * Extracted from the date-picker Composable, where it was unreachable by tests: the week-parity
 * arithmetic and the cut-off are the kind of rules that decide whether a real order is deliverable,
 * so they belong here.
 *
 * With a single path this reproduces the previous behaviour exactly — the next [limit] occurrences
 * of the path's delivery day, each flagged against the cut-off. With several paths the lists are
 * merged and sorted, so the customer of a city served by two tournées simply sees the next dates in
 * chronological order and picks one; that choice is what assigns them a path.
 *
 * Dates are deduplicated: two paths delivering on the same day produce one tile. The order carries
 * only the date, so there would be nothing to distinguish the two options by, and offering the same
 * day twice would read as a bug.
 *
 * @param paths The paths that serve the customer. Empty yields an empty list.
 * @param now Injected rather than read from the system clock so the cut-off is testable.
 * @param limit How many distinct dates to return.
 */
class BuildSelectableDeliveryDatesUseCase {

    operator fun invoke(
        paths: List<DeliveryPath>,
        now: LocalDateTime,
        limit: Int = 4
    ): List<SelectableDeliveryDate> {
        if (paths.isEmpty() || limit <= 0) return emptyList()

        return paths
            .flatMap { path -> nextDatesFor(path, now.toLocalDate(), limit) }
            .sortedBy { it.date }
            .distinctBy { it.date }
            .take(limit)
            .map { it.copy(isPastDeadline = isPastDeadline(it.date, now)) }
    }

    private fun nextDatesFor(
        path: DeliveryPath,
        from: LocalDate,
        limit: Int
    ): List<SelectableDeliveryDate> {
        val targetDay = runCatching { DayOfWeek.valueOf(path.deliveryDay.uppercase()) }
            .getOrDefault(FALLBACK_DELIVERY_DAY)
        val weekFields = WeekFields.of(Locale.FRANCE)

        val dates = mutableListOf<SelectableDeliveryDate>()
        var current = from
        var scanned = 0
        while (dates.size < limit && scanned < MAX_DAYS_SCANNED) {
            if (current.dayOfWeek == targetDay) {
                val weekNumber = current.get(weekFields.weekOfWeekBasedYear())
                val isAvailable = when (path.deliveryFrequency) {
                    "BIWEEKLY_EVEN" -> weekNumber % 2 == 0
                    "BIWEEKLY_ODD" -> weekNumber % 2 != 0
                    else -> true // "WEEKLY"
                }
                if (isAvailable) {
                    dates.add(
                        SelectableDeliveryDate(
                            date = current,
                            pathId = path.id,
                            pathName = path.pathName,
                            isPastDeadline = false
                        )
                    )
                }
            }
            current = current.plusDays(1)
            scanned++
        }
        return dates
    }

    private fun isPastDeadline(date: LocalDate, now: LocalDateTime): Boolean =
        now.isAfter(date.minusDays(1).atTime(ORDER_CUTOFF_HOUR, 0))
}
