package com.mtdevelopment.checkout.domain.repository

import com.mtdevelopment.checkout.domain.model.DeliveryPath

interface RoomDeliveryRepository {
    suspend fun persistPath(path: DeliveryPath)
    suspend fun deletePath(path: DeliveryPath)
    suspend fun updatePath(path: DeliveryPath)
    suspend fun getPathById(id: String, onSuccess: (DeliveryPath) -> Unit)
    suspend fun getPaths(onSuccess: (List<DeliveryPath>) -> Unit)
}