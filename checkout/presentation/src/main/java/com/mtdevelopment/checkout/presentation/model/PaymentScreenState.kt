package com.mtdevelopment.checkout.presentation.model

import com.mtdevelopment.checkout.domain.model.NewCheckoutResult
import com.mtdevelopment.core.model.CartItems

/**
 * UI State for the checkout and payment process.
 * 
 * @property isLoading Whether a long-running operation (like API call) is in progress.
 * @property error Error message to display to the user, if any.
 * @property isPaymentSuccess Flag set to true when the transaction is confirmed as 'PAID' by the gateway.
 * @property isGooglePayAvailable Whether Google Pay is supported on this device/account.
 * @property buyerName Full name of the customer.
 * @property buyerAddress Physical delivery address.
 * @property buyerEmail Contact email (required for receipt and payment processing).
 * @property buyerBillingAddress Address used for card authorization.
 * @property cartItems Summary of products being purchased.
 * @property totalPrice Total transaction amount in cents.
 * @property deliveryDate Selected delivery date (timestamp).
 * @property checkoutResult Result of the SumUp checkout session creation.
 * @property orderId The ID of the order record created in Firestore.
 * @property checkoutNote Optional customer notes for delivery instructions.
 * @property isAwaitingHostedCheckoutReturn True once the hosted SumUp page has been opened
 *   and we are waiting for the customer to come back. SumUp's hosted checkout never
 *   auto-redirects (it only shows a "return to merchant" button), so this flag lets the
 *   checkout screen trigger verification when the app regains focus — not only when the
 *   deep-link callback fires. Reset as soon as verification is triggered.
 */
data class PaymentScreenState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isPaymentSuccess: Boolean = false,
    val isGooglePayAvailable: Boolean = false,
    val buyerName: String? = null,
    val buyerAddress: String? = null,
    val buyerEmail: String? = null,
    val buyerBillingAddress: String? = null,
    val cartItems: CartItems? = null,
    val totalPrice: Long? = null,
    val deliveryDate: Long? = null,
    val checkoutResult: NewCheckoutResult? = null,
    var orderId: String? = null,
    var checkoutNote: String? = null,
    val isAwaitingHostedCheckoutReturn: Boolean = false
)
