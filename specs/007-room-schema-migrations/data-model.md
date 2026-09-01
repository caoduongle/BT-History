# Data Model: Safe Room Schema & Migration Architecture

**Feature**: `007-room-schema-migrations`  
**Date**: 2026-09-02  

## 1. Database Schema Version 1 Specification

The application database `bt_watcher_database` operates at **version 1** and contains two primary tables: `devices` and `events`.

### Table: `devices` (`DeviceEntity`)

Stores discovered, paired, and tracked Bluetooth devices.

| Column | Type | Nullable | Primary Key | Default / Notes |
|--------|------|----------|-------------|-----------------|
| `id` | `INTEGER` | `false` | `PRIMARY KEY AUTOINCREMENT` | Unique device identifier |
| `name` | `TEXT` | `false` | `false` | Human-readable name (fallback: "Thiết bị Bluetooth") |
| `mac_address` | `TEXT` | `false` | `false` | Bluetooth MAC Address (`UNIQUE INDEX`) |
| `device_type` | `TEXT` | `false` | `false` | Discriminator (`HEADSET`, `SPEAKER`, `WATCH`, `CAR`, `PHONE`, `OTHER`) |
| `is_connected` | `INTEGER` | `false` | `false` | SQLite Boolean (`0` = disconnected, `1` = connected) |
| `last_event_timestamp` | `INTEGER` | `false` | `false` | Unix epoch milliseconds |
| `last_event_type` | `TEXT` | `false` | `false` | Last event name (`CONNECT`, `DISCONNECT`) |
| `last_latitude` | `REAL` | `true` | `false` | Last known latitude |
| `last_longitude` | `REAL` | `true` | `false` | Last known longitude |
| `last_location_address`| `TEXT` | `true` | `false` | Reverse-geocoded address description |

**Indices**:
- `index_devices_mac_address` (`UNIQUE` on `mac_address`)

---

### Table: `events` (`EventEntity`)

Appends connection, disconnection, and location telemetry logs.

| Column | Type | Nullable | Primary Key | Foreign Key / Notes |
|--------|------|----------|-------------|---------------------|
| `id` | `INTEGER` | `false` | `PRIMARY KEY AUTOINCREMENT` | Unique event log identifier |
| `device_id` | `INTEGER` | `false` | `false` | References `devices(id)` on `CASCADE` delete |
| `event_type` | `TEXT` | `false` | `false` | Event discriminator (`CONNECT`, `DISCONNECT`) |
| `timestamp` | `INTEGER` | `false` | `false` | Unix epoch milliseconds |
| `latitude` | `REAL` | `true` | `false` | GPS latitude at event time |
| `longitude` | `REAL` | `true` | `false` | GPS longitude at event time |
| `accuracy` | `REAL` | `true` | `false` | GPS accuracy radius in meters |
| `location_address` | `TEXT` | `true` | `false` | Reverse-geocoded physical address |
| `is_unexpected_disconnect` | `INTEGER` | `false` | `false` | SQLite Boolean (`0` = intentional, `1` = sudden drop) |

**Indices**:
- `index_events_device_id` on `device_id`
- `index_events_timestamp` on `timestamp`

**Foreign Keys**:
- `FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE`

---

## 2. Schema Export Artifact

Upon enabling `exportSchema = true` and setting `room.schemaLocation`, Room's KSP processor generates:
```text
app/schemas/com.example.data.AppDatabase/
└── 1.json
```
This JSON file contains the canonical database hash, SQLite table definitions, indices, and foreign keys.

---

## 3. Migration Architecture & Contract

### Migration Lifecycle (v1 -> v2+)
```
┌─────────────────────────────────────────────────────────────┐
│ Developer introduces schema change (new column / table)    │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ 1. Increment version in @Database(version = 2, ...)         │
│ 2. Create Migration(1, 2) implementing sql statements       │
│ 3. Add Migration(1, 2) into ALL_MIGRATIONS array            │
│ 4. Rebuild project: Room exports 2.json to app/schemas/     │
│ 5. Commit entity, migration, and 2.json together in Git     │
└─────────────────────────────────────────────────────────────┘
```

### Safety Rule
- `fallbackToDestructiveMigration()` is removed from runtime.
- If schema version is incremented without an explicit `Migration`, Room will throw `IllegalStateException`, failing unit tests immediately during CI/build time rather than destroying user data silently at runtime.
