package com.mtdevelopment.lafromagerie

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mtdevelopment.delivery.data.model.entity.PathEntity
import com.mtdevelopment.home.data.model.ProductEntity
import com.mtdevelopment.home.data.source.local.dao.HomeDao
import kotlinx.serialization.json.Json

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE paths ADD COLUMN deliveryFrequency TEXT NOT NULL DEFAULT 'WEEKLY'")
    }
}

@Database(
    entities = [ProductEntity::class, PathEntity::class],
    version = 5,
)
@TypeConverters(Converters::class, CoordinatesConverter::class, MapConverter::class)
abstract class FromagerieDatabase : RoomDatabase() {
    abstract val homeDao: HomeDao
    abstract val deliveryDao: com.mtdevelopment.delivery.data.source.local.dao.DeliveryDao
}

class Converters {
    @TypeConverter
    fun fromList(value: List<String>) = Json.encodeToString(value)

    @TypeConverter
    fun toList(value: String) = Json.decodeFromString<List<String>>(value)
}

class CoordinatesConverter {
    @TypeConverter
    fun fromList(value: List<com.mtdevelopment.delivery.data.model.Coordinate>) =
        Json.encodeToString(value)

    @TypeConverter
    fun toList(value: String) =
        Json.decodeFromString<List<com.mtdevelopment.delivery.data.model.Coordinate>>(value)
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