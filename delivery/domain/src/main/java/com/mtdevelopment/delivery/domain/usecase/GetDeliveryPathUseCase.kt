package com.mtdevelopment.delivery.domain.usecase

class GetDeliveryPathUseCase(
    private val repository: com.mtdevelopment.delivery.domain.repository.FirestorePathRepository
) {
    operator fun invoke(
        pathName: String,
        onSuccess: (com.mtdevelopment.delivery.domain.model.DeliveryPath?) -> Unit,
        onFailure: () -> Unit
    ) {
        repository.getDeliveryPath(
            pathName = pathName,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }
}