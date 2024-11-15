package com.mtdevelopment.lafromagerie

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.mtdevelopment.home.data.model.ProductEntity
import com.mtdevelopment.home.data.source.local.dao.HomeDao
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Database(
    entities = [ProductEntity::class],
    version = 1,
)
@TypeConverters(Converters::class)
abstract class FromagerieDatabase : RoomDatabase() {
    abstract val dao: HomeDao
}

class Converters {
    @TypeConverter
    fun fromList(value: List<String>) = Json.encodeToString(value)

    @TypeConverter
    fun toList(value: String) = Json.decodeFromString<List<String>>(value)
}