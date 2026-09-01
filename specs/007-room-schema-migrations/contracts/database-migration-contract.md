# Database Migration & Schema Evolution Contract

**Feature**: `007-room-schema-migrations`  
**Target Component**: `com.example.data.AppDatabase`  

---

## 1. Principles

1. **Non-Destructive Guarantee**: User Bluetooth tracking logs and GPS history must NEVER be wiped automatically during application upgrades.
2. **Schema Baseline Traceability**: Every database schema version MUST be captured as a canonical JSON file under `app/schemas/com.example.data.AppDatabase/{version}.json`.
3. **Deterministic Upgrade Path**: There must always be a continuous, tested migration path from version `N` to version `N+1`.

---

## 2. Developer Migration Protocol

When modifying entities (`DeviceEntity`, `EventEntity`) or adding new tables:

### Step 1: Update Entities
Make entity modifications (e.g. adding `@ColumnInfo(name = "battery_level") val batteryLevel: Int? = null`).

### Step 2: Increment Database Version
In `AppDatabase.kt`:
```kotlin
@Database(
    entities = [DeviceEntity::class, EventEntity::class],
    version = CURRENT_VERSION + 1, // e.g., 2
    exportSchema = true
)
```

### Step 3: Implement Explicit Migration
Define a standalone, named `Migration` object:
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE devices ADD COLUMN battery_level INTEGER DEFAULT NULL")
    }
}
```

### Step 4: Register in `ALL_MIGRATIONS`
```kotlin
val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    MIGRATION_1_2,
)
```

### Step 5: Export & Commit
1. Run `./gradlew.bat testDebugUnitTest` to trigger KSP schema export.
2. Verify that `app/schemas/com.example.data.AppDatabase/{new_version}.json` is created.
3. Commit the new schema JSON together with the code changes.
