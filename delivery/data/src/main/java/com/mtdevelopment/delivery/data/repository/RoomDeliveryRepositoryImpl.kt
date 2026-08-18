package com.mtdevelopment.delivery.data.repository

import com.mtdevelopment.delivery.data.model.entity.toPath
import com.mtdevelopment.delivery.data.model.entity.toPathEntity
import com.mtdevelopment.delivery.data.source.local.DeliveryDatabase
import com.mtdevelopment.delivery.domain.model.DeliveryPath
import com.mtdevelopment.delivery.domain.repository.RoomDeliveryRepository
import kotlinx.coroutines.flow.first

class RoomDeliveryRepositoryImpl(
    private val db: DeliveryDatabase
) : RoomDeliveryRepository {
    override suspend fun persistPath(path: DeliveryPath) {
        db.persistPath(path.toPathEntity())
    }

    override suspend fun deletePath(path: DeliveryPath) {
        db.deletePath(path.toPathEntity())
    }

    override suspend fun updatePath(path: DeliveryPath) {
        db.updatePath(path.toPathEntity())
    }

    override suspend fun getPathById(
        id: String,
        onSuccess: (DeliveryPath) -> Unit
    ) {
        db.getPathById(id).collect {
            onSuccess.invoke(it.toPath())
        }
    }

    override suspend fun getPathsOnce(): List<DeliveryPath> =
        db.getAllPaths().first().map { it.toPath() }


}