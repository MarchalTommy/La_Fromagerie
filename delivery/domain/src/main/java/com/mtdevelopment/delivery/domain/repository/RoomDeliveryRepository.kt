package com.mtdevelopment.delivery.domain.repository

import com.mtdevelopment.delivery.domain.model.DeliveryPath

interface RoomDeliveryRepository {
    suspend fun persistPath(path: DeliveryPath)
    suspend fun deletePath(path: DeliveryPath)
    suspend fun updatePath(path: DeliveryPath)
    suspend fun getPathById(
        id: String,
        onSuccess: (DeliveryPath) -> Unit
    )

    /**
     * Reads the cached paths once and returns.
     *
     * Deliberately a snapshot, not a stream. This replaced a `getPaths(onSuccess)` that collected
     * Room's `Flow` — which never completes: it re-invoked its callback on every write to the table
     * and kept the calling coroutine alive for as long as its scope lived. Both callers need a
     * snapshot, and the streaming shape actively hurt them: the cache cleanup replayed its delete
     * loop against a frozen id set every time the table changed (so it could remove a path a later
     * fetch had just re-inserted), and the offline read re-emitted a new list under the UI mid-use.
     */
    suspend fun getPathsOnce(): List<DeliveryPath>
}