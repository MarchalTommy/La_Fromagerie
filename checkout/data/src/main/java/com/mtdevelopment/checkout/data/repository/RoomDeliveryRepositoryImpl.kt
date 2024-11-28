package com.mtdevelopment.checkout.data.repository

import com.mtdevelopment.checkout.data.remote.model.entity.toPath
import com.mtdevelopment.checkout.data.remote.model.entity.toPathEntity
import com.mtdevelopment.checkout.data.remote.source.local.DeliveryDatabase
import com.mtdevelopment.checkout.domain.model.DeliveryPath
import com.mtdevelopment.checkout.domain.repository.RoomDeliveryRepository

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

    override suspend fun getPathById(id: String, onSuccess: (DeliveryPath) -> Unit) {
        db.getPathById(id).collect {
            onSuccess.invoke(it.toPath())
        }
    }

    override suspend fun getPaths(onSuccess: (List<DeliveryPath>) -> Unit) {
        db.getAllPaths().collect { pathsList ->
            onSuccess.invoke(pathsList.map { path ->
                path.toPath()
            })
        }
    }


}