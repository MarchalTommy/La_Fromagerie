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

/**
 * Adds the per-city street restrictions used to split a city between two delivery paths.
 *
 * Purely additive: existing rows default to `{}`, i.e. every cached city stays covered in full,
 * which is how they already behaved. The real restrictions arrive with the next Firestore refresh.
 * Doing this rather than falling back to a destructive migration keeps the path cache warm — a
 * wiped cache would make every address undeliverable on a first launch without network.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE paths ADD COLUMN cityStreets TEXT NOT NULL DEFAULT '{}'")
    }
}

/**
 * Adds the per-city commune centers, so a cached path can be read back without geocoding.
 *
 * Additive like [MIGRATION_5_6]: existing rows default to `{}`, meaning "no coordinate stored", and
 * the reader geocodes those cities exactly as it always did. A destructive fallback here would wipe
 * the path cache — which is the one thing that keeps the app usable when the address API is slow or
 * unreachable, and the whole point of the change this migration serves.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE paths ADD COLUMN cityCoordinates TEXT NOT NULL DEFAULT '{}'")
    }
}

/**
 * Adds the optional shop-collection price. Nullable with no default: null means "this product
 * costs the same wherever it is collected", which is exactly what every row cached before this
 * column existed should read as.
 *
 * Numbered 7 → 8, not 6 → 7: version 7 already exists in the field as the cityCoordinates
 * schema of [MIGRATION_6_7], shipped in 1.0.1, and this column was written on a branch that
 * forked before it. Two schemas under one version number is precisely what Room's identity
 * hash catches, at startup, with a crash — and `fallbackToDestructiveMigration` does not
 * cover it, since it only fires when the version number itself changes.
 *
 * Additive rather than a bare version bump, for the same reason as [MIGRATION_5_6]: the
 * destructive fallback is a safety net for unhandled jumps, not a shortcut — a wiped cache
 * makes every address undeliverable on a first launch without network.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE products ADD COLUMN priceInCentsPickupShop INTEGER DEFAULT NULL")
    }
}

@Database(
    entities = [ProductEntity::class, PathEntity::class],
    version = 8,
)
@TypeConverters(
    Converters::class,
    CoordinatesConverter::class,
    MapConverter::class,
    StreetsMapConverter::class,
    CityCoordinatesConverter::class
)
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

/** City name → the center of that commune. Absent city = no coordinate stored yet. */
class CityCoordinatesConverter {
    @TypeConverter
    fun toCoordinatesMap(value: String): Map<String, com.mtdevelopment.delivery.data.model.Coordinate> {
        return Json.decodeFromString(value)
    }

    @TypeConverter
    fun fromCoordinatesMap(map: Map<String, com.mtdevelopment.delivery.data.model.Coordinate>): String {
        return Json.encodeToString(map)
    }
}

/** City name → the streets of that city served by the path. Absent city = whole city served. */
class StreetsMapConverter {
    @TypeConverter
    fun toStreetsMap(value: String): Map<String, List<String>> {
        return Json.decodeFromString(value)
    }

    @TypeConverter
    fun fromStreetsMap(map: Map<String, List<String>>): String {
        return Json.encodeToString(map)
    }
}