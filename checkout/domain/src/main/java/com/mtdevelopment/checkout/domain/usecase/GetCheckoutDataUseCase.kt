package com.mtdevelopment.checkout.domain.usecase

import android.util.Log
import com.mtdevelopment.checkout.domain.model.LocalCheckoutInformation
import com.mtdevelopment.core.model.CartItem
import com.mtdevelopment.core.model.CartItems
import com.mtdevelopment.core.model.FulfillmentType
import com.mtdevelopment.core.repository.CatalogPriceSource
import com.mtdevelopment.core.repository.SharedDatastore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Assembles everything the payment screen needs, including the amount actually charged.
 *
 * [LocalCheckoutInformation.totalPrice] is what reaches Google Pay, SumUp and the stored
 * order, so the unit prices behind it are resolved here, on read, from [CatalogPriceSource]
 * for the mode currently in force — the price stored on each basket line serves only as a
 * fallback. The alternative, keeping the stored basket authoritative and repairing it
 * whenever the mode changes, made the charged amount depend on a job having run; when it did
 * not run the customer was charged the wrong price with nothing failing. Resolving on read
 * removes that derived state, so the amount is correct by construction instead of by upkeep.
 */
class GetCheckoutDataUseCase(
    private val sharedDatastore: SharedDatastore,
    private val catalogPriceSource: CatalogPriceSource
) {
    operator fun invoke(): Flow<LocalCheckoutInformation?> {

        val user = sharedDatastore.userInformationFlow
        val cart = sharedDatastore.cartItemsFlow
        val deliveryDate = sharedDatastore.deliveryDateFlow
        val fulfillmentType = sharedDatastore.fulfillmentTypeFlow

        var localCheckoutInformation =
            combine(user, cart, fulfillmentType) { user, cart, storedMode ->
                if (cart != null && user != null) {
                    val priced = cart.pricedFor(FulfillmentType.fromStoredValue(storedMode))
                    LocalCheckoutInformation(
                        buyerName = user.name,
                        buyerAddress = user.address,
                        buyerEmail = user.email,
                        cartItems = priced,
                        totalPrice = priced.totalPrice,
                        deliveryDate = 0L,
                        billingAddress = user.billingAddress,
                        buyerPhone = user.phone,
                        fulfillmentType = user.fulfillmentType,
                        pickupPointId = user.pickupPointId,
                        pickupLabel = user.pickupLabel,
                        pickupAddress = user.pickupAddress,
                        pickupTimeRange = user.pickupTimeRange
                    )
                } else {
                    null
                }
            }

        localCheckoutInformation =
            localCheckoutInformation.combine(deliveryDate) { localCheckoutInformation, deliveryDate ->
                localCheckoutInformation?.copy(deliveryDate = deliveryDate)
            }

        return localCheckoutInformation
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
     * withdrawn product, or a failed read. The fallback is mandatory: it degrades to exactly
     * the behaviour that shipped before prices were resolved on read, so a catalogue problem
     * can never leave a customer unable to pay. It is logged because the point of the change
     * is to make this failure audible rather than silent.
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
        const val TAG = "GetCheckoutDataUseCase"
    }
}
