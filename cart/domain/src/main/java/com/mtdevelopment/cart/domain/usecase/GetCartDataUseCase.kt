package com.mtdevelopment.cart.domain.usecase

import android.util.Log
import com.mtdevelopment.core.model.CartItem
import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.core.model.FulfillmentType
import com.mtdevelopment.core.repository.CatalogPriceSource
import com.mtdevelopment.core.repository.SharedDatastore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Reads the basket, priced for the fulfillment mode currently in force.
 *
 * The price stored on a basket line is the one it was added at, and is treated here as a
 * fallback only: the effective price is resolved on every read from
 * [CatalogPriceSource]. Nothing rewrites the stored basket to keep it in step with the mode,
 * because a value maintained by a repair job is a value that can silently be stale — and a
 * stale basket price is money charged wrong. Resolving on read makes "the basket is priced
 * for the current mode" true by construction: there is no derived state left to go bad.
 *
 * The mode is read from the datastore rather than passed in, so every reader of the basket
 * agrees on one answer without having to thread the mode through the call chain.
 */
class GetCartDataUseCase(
    private val datastore: SharedDatastore,
    private val catalogPriceSource: CatalogPriceSource
) {

    /**
     * @return A Flow emitting the basket, each line valued at the current price for the
     * active mode, with [CartItems.totalPrice] recomputed from those lines.
     */
    operator fun invoke(): Flow<CartItems?> =
        datastore.cartItemsFlow.combine(datastore.fulfillmentTypeFlow) { cart, storedMode ->
            cart?.pricedFor(FulfillmentType.fromStoredValue(storedMode))
        }

    private suspend fun CartItems.pricedFor(mode: FulfillmentType): CartItems {
        val lines = cartItems.map { item -> item?.pricedFor(mode) }
        return copy(
            cartItems = lines,
            totalPrice = lines.sumOf { (it?.price ?: 0L) * (it?.quantity ?: 0) }
        )
    }

    /**
     * Falls back to the stored price whenever the catalogue cannot answer — a renamed or
     * withdrawn product, or a failed read. The line is kept either way: losing the ability to
     * pay is a worse failure than charging a price that is merely out of date. The fallback is
     * logged so that the degradation is noisy, which is the whole point of resolving on read.
     */
    private suspend fun CartItem.pricedFor(mode: FulfillmentType): CartItem {
        val resolved = catalogPriceSource.unitPriceCents(name, mode)
        if (resolved == null) {
            Log.w(TAG, "No catalogue price for '$name' in $mode, falling back to stored $price")
            return this
        }
        return copy(price = resolved)
    }

    private companion object {
        const val TAG = "GetCartDataUseCase"
    }
}
