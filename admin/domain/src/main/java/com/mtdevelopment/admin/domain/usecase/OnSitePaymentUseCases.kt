package com.mtdevelopment.admin.domain.usecase

import com.mtdevelopment.admin.domain.repository.FirebaseAdminRepository
import com.mtdevelopment.core.domain.toLocalDate
import com.mtdevelopment.core.model.Order
import com.mtdevelopment.core.model.OrderStatus
import com.mtdevelopment.core.model.PaymentMode
import java.time.LocalDate

/** How long an uncollected, unpaid order is kept before it is written off. */
const val ON_SITE_ORDER_GRACE_DAYS = 3L

/**
 * Records that the shop took the money for an order paid on collection.
 *
 * Refuses anything that was paid online: those orders are settled by the payment chain, and
 * a manual "encaissé" on one would paper over whatever went wrong there instead of
 * surfacing it.
 */
class MarkOrderPaidOnSiteUseCase(
    private val firebaseAdminRepository: FirebaseAdminRepository
) {
    suspend operator fun invoke(order: Order): Result<Unit> {
        if (order.paymentMode != PaymentMode.ON_SITE) {
            return Result.failure(
                IllegalArgumentException("Cette commande n'est pas à encaisser sur place")
            )
        }
        return firebaseAdminRepository.updateOrderStatus(order.id, OrderStatus.PAID)
    }
}

/**
 * Writes off orders that were to be paid on collection and never were.
 *
 * ⚠️ Strictly limited to [PaymentMode.ON_SITE]. An online order stuck in PENDING means a
 * payment in flight or a finalization that failed — possibly with the customer already
 * charged. Cancelling those automatically would erase the only trace of it, so they are
 * left alone deliberately, to be investigated.
 *
 * Runs from the admin app rather than a scheduled backend job: it therefore only sweeps
 * when the shop opens its order list, which is enough for a write-off with no deadline of
 * its own. Moving it to a Cloud Function would make it run unattended — an open decision
 * in the spec, not an oversight.
 */
class CancelStaleOnSiteOrdersUseCase(
    private val firebaseAdminRepository: FirebaseAdminRepository
) {
    /**
     * @param today Injected so the grace period is testable.
     * @return the ids written off.
     */
    suspend operator fun invoke(
        orders: List<Order>,
        today: LocalDate = LocalDate.now()
    ): List<String> {
        val cutoff = today.minusDays(ON_SITE_ORDER_GRACE_DAYS)

        return orders
            .filter { it.paymentMode == PaymentMode.ON_SITE && it.status == OrderStatus.PENDING }
            // An unparseable date is left alone rather than guessed at: cancelling an order
            // because its date could not be read would be the worst possible reading of it.
            .filter { order -> order.deliveryDate.toLocalDate()?.isBefore(cutoff) == true }
            .mapNotNull { order ->
                order.id.takeIf {
                    firebaseAdminRepository
                        .updateOrderStatus(order.id, OrderStatus.CANCELED)
                        .isSuccess
                }
            }
    }
}
