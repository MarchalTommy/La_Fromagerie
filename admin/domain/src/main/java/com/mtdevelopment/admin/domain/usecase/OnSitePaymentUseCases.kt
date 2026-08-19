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
 *
 * ⚠️ **Known coupling, not yet resolved: this writes payment into the lifecycle field.**
 * "Encaissé" is a fact about money, and the only place it is recorded is
 * [OrderStatus.PAID] — the same field that also says where an order is in its life. Lot 1
 * separated those two ideas by giving payment its own [PaymentMode] column precisely so they
 * would stop being one thing; this reintroduces the conflation from the other end, and there
 * is currently no way to express "collected, and paid for" or "paid, but not yet prepared".
 *
 * The coupling bites through [CancelStaleOnSiteOrdersUseCase], which sweeps on
 * `status == PENDING` alone. Anything moved off PENDING is therefore permanently out of reach
 * of the write-off — including an unpaid collected order that someone advanced to
 * [OrderStatus.IN_PREPARATION]. **Today that cannot happen**: the only two writers of
 * IN_PREPARATION are `DeliveryHelperScreen` and the `DeliveryAddDialog` it opens, and
 * collected orders are kept out of the delivery round. So the exposure is a coupling waiting
 * for a screen that does not exist yet, not a live bug — which is exactly why it is written
 * down rather than guarded against with a runtime check nothing can currently trigger.
 *
 * Resolving it means a payment state of its own (a `paid_at`, or a status on [PaymentMode]),
 * which changes the Firestore schema and is a decision for the shop, not for this use case.
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
 *
 * ⚠️ **The `status == PENDING` filter is the sweep's whole reach, and that is narrower than
 * it looks.** An unpaid collected order that leaves PENDING by any route — today only
 * [MarkOrderPaidOnSiteUseCase], which is the correct exit — never comes back into scope, so a
 * future screen that moves a collected order to [OrderStatus.IN_PREPARATION] would put it
 * beyond write-off for good. Nothing writes IN_PREPARATION for a collected order today (see
 * [MarkOrderPaidOnSiteUseCase] for where that is enforced and why the two use cases have to
 * be read together). Widen this filter before adding such a screen, not after.
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

        val stale = orders
            .filter { it.paymentMode == PaymentMode.ON_SITE && it.status == OrderStatus.PENDING }
            // An unparseable date is left alone rather than guessed at: cancelling an order
            // because its date could not be read would be the worst possible reading of it.
            .filter { order -> order.deliveryDate.toLocalDate()?.isBefore(cutoff) == true }

        // Written as a loop rather than folded into the filter chain: every line above decides
        // what to cancel, this one does the cancelling. A predicate that writes to Firestore
        // reads as a predicate right up until someone reorders the chain around it.
        val writtenOff = mutableListOf<String>()
        stale.forEach { order ->
            if (firebaseAdminRepository.updateOrderStatus(order.id, OrderStatus.CANCELED)
                    .isSuccess
            ) {
                writtenOff.add(order.id)
            }
        }
        return writtenOff
    }
}
