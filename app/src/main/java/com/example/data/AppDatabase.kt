package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.DeviceDao
import com.example.data.dao.EventDao
import com.example.data.entity.DeviceEntity
import com.example.data.entity.EventEntity

/**
 * Main Room database for BT Watcher.
 *
 * ============================================================================
 * CRITICAL DATABASE ARCHITECTURE & MIGRATION CONTRACT:
 * ============================================================================
 *
 * 1. DATA SAFETY GUARANTEE:
 *    Destructive migration (`fallbackToDestructiveMigration`) is deliberately DISABLED.
 *    BT Watcher records personal device history, disconnect alerts, and critical GPS
 *    coordinates. Silently wiping user data on app updates or schema changes is strictly
 *    forbidden.
 *
 * 2. SCHEMA EVOLUTION RULES:
 *    Any future change to entity definitions (DeviceEntity, EventEntity, or new tables/indices)
 *    MUST adhere to the following mandatory workflow:
 *    a) Increment `version` in `@Database(version = N, ...)`.
 *    b) Implement an explicit `Migration(from = N - 1, to = N)` using standard SQLite DDL.
 *    c) Append the new Migration instance to [ALL_MIGRATIONS].
 *    d) Rebuild the project to generate the new schema JSON artifact under `app/schemas/`.
 *    e) Commit entity code, Migration definition, and the exported schema JSON together in Git.
 *
 * 3. STRICT ENFORCEMENT:
 *    Never increment `version` without providing an explicit Migration in [ALL_MIGRATIONS].
 *    Room will intentionally throw an [IllegalStateException] if a required migration path
 *    is missing, preventing accidental data loss or silent database corruptions.
 */
@Database(
    entities = [DeviceEntity::class, EventEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration từ Version 1 lên Version 2:
         * Bổ sung composite index `index_events_device_id_timestamp` trên bảng `events`
         * để tối ưu hoá truy vấn phân trang lịch sử sự kiện theo từng thiết bị.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_events_device_id_timestamp` ON `events` (`device_id`, `timestamp`)")
            }
        }

        /**
         * Registry of all sequential migrations from version 1 onwards.
         */
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2
        )

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bt_watcher_database"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
