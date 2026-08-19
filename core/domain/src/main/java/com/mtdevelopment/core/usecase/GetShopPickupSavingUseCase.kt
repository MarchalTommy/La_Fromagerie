package com.mtdevelopment.core.usecase

import com.mtdevelopment.core.model.CartItem
import com.mtdevelopment.core.model.FulfillmentType
import com.mtdevelopment.core.repository.CatalogPriceSource
import com.mtdevelopment.core.repository.SharedDatastore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * What the current basket would cost less, in cents, if collected at the shop instead of
 * delivered.
 *
 * The shop price is opt-in per product and never above the delivery one, so this is always a
 * saving or nothing -- which is what lets the customer-facing copy say "moins cher" without a
 * sign check. Zero means there is nothing worth telling the customer: an empty basket, or a
 * basket of products the shop charges the same for either way.
 *
 * Deliberately computed from [CatalogPriceSource] rather than from the prices stored on the
 * basket lines. Those are a fallback, priced for whichever mode was in force when the line was
 * added; comparing two modes needs both answers from the catalogue, at the same instant.
 *
 * A product the catalogue cannot price contributes **nothing** rather than a guess. Quoting a
 * saving that turns out not to exist at checkout is worse than quoting none: the customer
 * chose to drive over on the strength of it.
 */
class GetShopPickupSavingUseCase(
    private val datastore: SharedDatastore,
    private val catalogPriceSource: CatalogPriceSource
) {

    /**
     * @return A Flow emitting the saving in cents, recomputed whenever the basket changes.
     */
    operator fun invoke(): Flow<Long> = datastore.cartItemsFlow.map { cart ->
        cart?.cartItems?.filterNotNull().orEmpty().sumOf { it.saving() }
    }

    private suspend fun CartItem.saving(): Long {
        val delivered = catalogPriceSource.unitPriceCents(name, FulfillmentType.DELIVERY)
            ?: return 0L
        val collected = catalogPriceSource.unitPriceCents(name, FulfillmentType.PICKUP_SHOP)
            ?: return 0L
        // Guarded rather than assumed: the invariant that a shop price never exceeds the
        // delivery one is enforced in the admin editor, and a document written before that
        // guard existed would otherwise turn into a negative "saving" here.
        return ((delivered - collected).coerceAtLeast(0L)) * quantity
    }
}
