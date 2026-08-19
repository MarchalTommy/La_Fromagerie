package com.mtdevelopment.core.repository

import com.mtdevelopment.core.model.FulfillmentType

/**
 * Reads the catalogue price of a product for a given fulfillment mode.
 *
 * The basket stores the unit price a line was added at, but that copy is only a **fallback**:
 * the price actually charged is resolved here, at read time, from the mode currently in force.
 * Keeping the stored copy authoritative used to require a repair job that rewrote the basket
 * on every mode switch — and a derived value maintained by a job is a value that can be stale.
 * When that job did not run (catalogue not loaded, no live ViewModel), nothing broke loudly:
 * the customer was simply charged the wrong price. Resolving on read removes the derived state
 * entirely, so the invariant "the basket is priced for the current mode" holds by construction
 * rather than by upkeep.
 *
 * This port lives in `core:domain`, which depends on no other module, precisely so that
 * `cart:domain` and `checkout:domain` can price a basket without either of them gaining a
 * dependency on the catalogue module that owns the products.
 */
interface CatalogPriceSource {

    /**
     * Current unit price in cents for [productName] under [mode].
     *
     * Returns `null` when the price cannot be established — the product has been renamed or
     * withdrawn from the catalogue, or the catalogue read itself failed. Callers must then
     * fall back to the price stored on the basket line: a customer must never lose the ability
     * to pay because the catalogue could not be read.
     */
    suspend fun unitPriceCents(productName: String, mode: FulfillmentType): Long?
}
