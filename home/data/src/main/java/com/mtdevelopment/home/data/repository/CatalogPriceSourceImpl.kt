package com.mtdevelopment.home.data.repository

import android.util.Log
import com.mtdevelopment.core.model.FulfillmentType
import com.mtdevelopment.core.repository.CatalogPriceSource
import com.mtdevelopment.core.util.DataResult
import com.mtdevelopment.home.domain.repository.ProductRepository

/**
 * Resolves catalogue prices from the product repository.
 *
 * This implementation lives in `home:data` because that is the only module that already sees
 * both [ProductRepository] and `core:domain`; the port it satisfies sits in `core:domain` so
 * that the basket and checkout modules never have to depend on the catalogue.
 *
 * The lookup goes through the repository's normal path, which serves the Room `products`
 * cache unless a refresh is pending — so pricing keeps working with no network, which is the
 * condition under which a customer must still be able to pay.
 *
 * Products are matched **by name**, already the join key the `orders` collection uses for its
 * product map. A renamed product therefore resolves to nothing and the caller falls back to
 * the stored price, which is the intended degradation: an unexpected price is worse than a
 * slightly stale one.
 */
class CatalogPriceSourceImpl(
    private val productRepository: ProductRepository
) : CatalogPriceSource {

    override suspend fun unitPriceCents(productName: String, mode: FulfillmentType): Long? {
        val result = runCatching { productRepository.getAllProducts() }
            .getOrElse { error ->
                // Never propagate: a catalogue read that throws must degrade to the stored
                // price, not take down the basket or the payment screen.
                Log.w(TAG, "Catalogue read threw while pricing '$productName'", error)
                return null
            }

        if (result !is DataResult.Success) {
            Log.w(TAG, "Catalogue unavailable while pricing '$productName' ($result)")
            return null
        }

        return result.data.firstOrNull { it.name == productName }?.priceFor(mode)
    }

    private companion object {
        const val TAG = "CatalogPriceSource"
    }
}
