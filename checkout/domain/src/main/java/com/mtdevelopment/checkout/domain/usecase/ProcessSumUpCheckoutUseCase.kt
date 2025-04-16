package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.model.GooglePayData
import com.mtdevelopment.checkout.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.flowOf

class ProcessSumUpCheckoutUseCase(
    private val paymentRepository: PaymentRepository
) {
    // TODO: For now, mock it to send a boolean and a delay for as long as I'm using preprod Google Pay
    operator fun invoke(checkoutId: String, googlePayData: GooglePayData) =
//        paymentRepository.processCheckout(checkoutId, googlePayData)
        flowOf(Pair(true, 5000L))
}