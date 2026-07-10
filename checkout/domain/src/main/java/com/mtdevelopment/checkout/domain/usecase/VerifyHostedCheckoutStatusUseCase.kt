package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.model.Checkout
import com.mtdevelopment.checkout.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow

/**
 * Verifies the outcome of a hosted-checkout payment when the customer returns from the
 * SumUp page, by resiliently polling SumUp for the checkout reference (= orderId) with an
 * amount-integrity check. Replaces a fragile single-shot lookup that mis-reported a
 * still-processing checkout — or a momentary connectivity blip — as "not paid".
 *
 * See [PaymentRepository.pollHostedCheckoutStatus] for the exact emission contract.
 */
class VerifyHostedCheckoutStatusUseCase(
    private val paymentRepository: PaymentRepository
) {
    operator fun invoke(
        reference: String,
        expectedAmountCents: Long?
    ): Flow<Result<Checkout>> =
        paymentRepository.pollHostedCheckoutStatus(reference, expectedAmountCents)
}
