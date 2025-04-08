package com.mtdevelopment.delivery.data.source.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mtdevelopment.delivery.data.model.entity.PathEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun persistPath(product: PathEntity)

    @Delete
    suspend fun deletePath(product: PathEntity)

    @Update
    suspend fun updatePath(product: PathEntity)

    @Query("SELECT * FROM paths WHERE id = :id")
    fun getPathById(id: String): Flow<PathEntity>

    @Query("SELECT * FROM paths")
    fun getAllPaths(): Flow<List<PathEntity>>

    // TODO: Contains dans un query ?
//    @Query("SELECT * FROM paths WHERE cities")

}