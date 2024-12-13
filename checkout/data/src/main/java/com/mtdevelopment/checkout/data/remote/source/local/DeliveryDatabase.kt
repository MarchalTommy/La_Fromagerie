package com.mtdevelopment.checkout.data.remote.source.local

import com.mtdevelopment.checkout.data.remote.model.entity.PathEntity
import com.mtdevelopment.checkout.data.remote.source.local.dao.DeliveryDao

class DeliveryDatabase(
    private val dao: DeliveryDao
) {

    suspend fun persistPath(path: PathEntity) {
        dao.persistPath(path)
    }

    suspend fun deletePath(path: PathEntity) {
        dao.deletePath(path)
    }

    suspend fun updatePath(path: PathEntity) {
        dao.updatePath(path)
    }

    fun getPathById(id: String) = dao.getPathById(id)

    fun getAllPaths() = dao.getAllPaths()

}