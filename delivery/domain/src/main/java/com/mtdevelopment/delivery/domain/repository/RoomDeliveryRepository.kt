package com.mtdevelopment.delivery.domain.repository

interface RoomDeliveryRepository {
    suspend fun persistPath(path: com.mtdevelopment.delivery.domain.model.DeliveryPath)
    suspend fun deletePath(path: com.mtdevelopment.delivery.domain.model.DeliveryPath)
    suspend fun updatePath(path: com.mtdevelopment.delivery.domain.model.DeliveryPath)
    suspend fun getPathById(
        id: String,
        onSuccess: (com.mtdevelopment.delivery.domain.model.DeliveryPath) -> Unit
    )

    suspend fun getPaths(onSuccess: (List<com.mtdevelopment.delivery.domain.model.DeliveryPath>) -> Unit)
}