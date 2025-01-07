package com.mtdevelopment.lafromagerie

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.mtdevelopment.checkout.data.remote.model.Coordinate
import com.mtdevelopment.checkout.data.remote.model.entity.PathEntity
import com.mtdevelopment.checkout.data.remote.source.local.dao.DeliveryDao
import com.mtdevelopment.home.data.model.ProductEntity
import com.mtdevelopment.home.data.source.local.dao.HomeDao
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Database(
    entities = [ProductEntity::class, PathEntity::class],
    version = 3,
)
@TypeConverters(Converters::class, CoordinatesConverter::class, MapConverter::class)
abstract class FromagerieDatabase : RoomDatabase() {
    abstract val homeDao: HomeDao
    abstract val deliveryDao: DeliveryDao
}

class Converters {
    @TypeConverter
    fun fromList(value: List<String>) = Json.encodeToString(value)

    @TypeConverter
    fun toList(value: String) = Json.decodeFromString<List<String>>(value)
}

class CoordinatesConverter {
    @TypeConverter
    fun fromList(value: List<Coordinate>) = Json.encodeToString(value)

    @TypeConverter
    fun toList(value: String) = Json.decodeFromString<List<Coordinate>>(value)
}

class MapConverter {
    @TypeConverter
    fun fromString(value: String): Map<String, Int> {
        return Json.decodeFromString(value)
    }

    @TypeConverter
    fun fromStringMap(map: Map<String, Int>): String {
        return Json.encodeToString(map)
    }
}