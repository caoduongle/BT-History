# Data Model: Data Retention, History Pagination & Export

**Branch**: `010-data-retention-and-export`  
**Date**: 2026-09-02  

---

## 1. Persistent Preferences Schema (`PreferencesRepository`)

| Preference Key | Type | Default Value | Allowed Values | Description |
|:---|:---:|:---:|:---:|:---|
| `history_retention_days` | `Int` | `180` | `30`, `90`, `180`, `365`, `0` | Number of days to retain historical events. `0` indicates Unlimited. |

---

## 2. Room Database Schema Evolution (Version 1 -> Version 2)

### Current `events` Table (Version 1)
```sql
CREATE TABLE IF NOT EXISTS `events` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `device_id` INTEGER NOT NULL,
    `event_type` TEXT NOT NULL,
    `timestamp` INTEGER NOT NULL,
    `latitude` REAL,
    `longitude` REAL,
    `accuracy` REAL,
    `location_address` TEXT,
    `is_unexpected_disconnect` INTEGER NOT NULL,
    FOREIGN KEY(`device_id`) REFERENCES `devices`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS `index_events_device_id` ON `events` (`device_id`);
CREATE INDEX IF NOT EXISTS `index_events_timestamp` ON `events` (`timestamp`);
```

### Version 2 Addition
Add composite index to eliminate filesort on device-specific history pagination queries:
```sql
CREATE INDEX IF NOT EXISTS `index_events_device_id_timestamp` ON `events` (`device_id`, `timestamp`);
```

### Migration Definition: `MIGRATION_1_2`
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_events_device_id_timestamp` ON `events` (`device_id`, `timestamp`)")
    }
}
```

---

## 3. Paginated DAO Queries (`EventDao`)

```kotlin
// Global paginated timeline
@Query("SELECT * FROM events ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
suspend fun getEventsPaged(limit: Int, offset: Int): List<EventEntity>

// Device-specific paginated history
@Query("SELECT * FROM events WHERE device_id = :deviceId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
suspend fun getEventsByDeviceIdPaged(deviceId: Long, limit: Int, offset: Int): List<EventEntity>

// Prune events older than cutoff timestamp
@Query("DELETE FROM events WHERE timestamp < :cutoffTimestamp")
suspend fun deleteEventsOlderThan(cutoffTimestamp: Long): Int

// Total count of events
@Query("SELECT COUNT(*) FROM events")
fun getTotalEventCountFlow(): Flow<Int>

@Query("SELECT COUNT(*) FROM events WHERE device_id = :deviceId")
fun getEventCountByDeviceIdFlow(deviceId: Long): Flow<Int>
```

---

## 4. Export Data Formats

### JSON Export Format (`application/json`)
```json
{
  "version": 1,
  "exported_at": "2026-09-02T09:15:00Z",
  "app_version": "1.0.0",
  "devices": [
    {
      "id": 1,
      "name": "Sony WH-1000XM5",
      "mac_address": "00:11:22:33:44:55",
      "device_type": "HEADSET",
      "is_connected": true,
      "last_seen_timestamp": 1756804500000,
      "last_latitude": 10.7769,
      "last_longitude": 106.7009,
      "last_location_address": "Quận 1, TP. Hồ Chí Minh",
      "events": [
        {
          "id": 101,
          "event_type": "CONNECT",
          "timestamp": 1756804500000,
          "latitude": 10.7769,
          "longitude": 106.7009,
          "accuracy": 15.0,
          "location_address": "Quận 1, TP. Hồ Chí Minh",
          "is_unexpected_disconnect": false
        }
      ]
    }
  ]
}
```

### CSV Export Format (`text/csv`)
Header:
```csv
device_name,mac_address,device_type,event_type,timestamp,date_time,latitude,longitude,accuracy,location_address,is_unexpected_disconnect
"Sony WH-1000XM5","00:11:22:33:44:55","HEADSET","CONNECT",1756804500000,"2026-09-02 09:15:00",10.7769,106.7009,15.0,"Quận 1, TP. Hồ Chí Minh",false
```
