package com.mtdevelopment.checkout.domain.usecase

import com.mtdevelopment.checkout.domain.model.DeliveryPath
import com.mtdevelopment.checkout.domain.repository.FirestorePathRepository

class GetDeliveryPathUseCase(
    private val repository: FirestorePathRepository
) {
    operator fun invoke(
        pathName: String,
        onSuccess: (DeliveryPath?) -> Unit,
        onFailure: () -> Unit
    ) {
        repository.getDeliveryPath(
            pathName = pathName,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }
}