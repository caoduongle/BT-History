# Research & Technical Decisions: Data Retention, History Pagination & Export

**Branch**: `010-data-retention-and-export`  
**Date**: 2026-09-02  

---

## 1. Event History Pagination Architecture

### Context
`EventDao.getAllEventsFlow()` and `getEventsByDeviceIdFlow()` query and load all rows into memory at once. With continuous background recording, the `events` table can grow to tens of thousands of rows, causing frame drops and potential OutOfMemory (OOM) errors.

### Decision
Implement reactive chunked pagination with Room `LIMIT :limit OFFSET :offset` (or keyset pagination `WHERE timestamp < :beforeTimestamp LIMIT :limit`) coordinated by `DeviceViewModel`.

### Rationale
1. **Low Complexity, High Performance**: Room LIMIT/OFFSET or keyset pagination eliminates full-table memory consumption, loading data in bounded slices (default 50 items per page).
2. **Seamless Compose Integration**: Works naturally with Compose `LazyColumn` and `LazyListState.layoutInfo.visibleItemsInfo` to trigger `loadMore()` when the user scrolls near the end of the list.
3. **No Unnecessary External Dependencies**: Adding `androidx.paging:paging-runtime` and `androidx.paging:paging-compose` introduces 3 new library dependencies with version compatibility risks under AGP 9.1.1 / Kotlin 2.2.10, and complicates Robolectric unit tests. Room chunking delivers the identical performance and memory bounds ($< 50\text{ MB}$ heap) without dependency bloat.
4. **Reactive Real-Time Updates**: When a new Bluetooth event occurs in the background, Room database updates notify the viewmodel to refresh the top slice, keeping real-time display intact.

### Alternatives Considered
- *Full Paging 3 (`androidx.paging`)*: Powerful for infinite virtual lists, but introduces heavy boilerplate (RemoteMediator, PagingDataDiffer in unit tests), dependency risks with newest KSP/AGP versions, and high friction for date-grouping headers in UI.
- *Status Quo (Full table query)*: Unacceptable due to memory bloat over time.

---

## 2. Data Retention Policy & Background Pruning

### Context
Events accumulate indefinitely. Users need control over history lifespan (30, 90, 180, 365 days, or Unlimited).

### Decision
1. **Preferences Storage**: Store retention setting in `PreferencesRepository` as `KEY_HISTORY_RETENTION_DAYS: Preferences.Key<Int>` (default `180`).
2. **Retention Options**:
   - `30`: 30 ngày
   - `90`: 90 ngày
   - `180`: 180 ngày (Mặc định)
   - `365`: 1 năm (365 ngày)
   - `0`: Không giới hạn (Unlimited)
3. **Pruning Implementation**:
   - `EventDao.deleteEventsOlderThan(cutoffTimestamp: Long): Int`
   - SQL: `DELETE FROM events WHERE timestamp < :cutoffTimestamp`
   - Preserves all parent `DeviceEntity` records (only obsolete event rows are deleted).
4. **Execution Triggers**:
   - **Service Startup**: When `BluetoothWatcherService` starts (`onStartCommand`), execute background pruning on `Dispatchers.IO`.
   - **Settings Change**: When user selects a new retention threshold in `SettingsScreen`, immediately trigger pruning in `DeviceViewModel`.
   - **Periodic Run**: On service run cycle, periodic background pruning executes every 24 hours.

### Rationale
- Zero battery overhead: Pruning is an indexed delete on `timestamp` running once per day or on service start.
- Does not affect device records: Known devices remain visible with their last known status even if ancient history is pruned.

---

## 3. Storage Access Framework (SAF) Export

### Context
Users need a way to export complete device and event history to a local file before wiping or for external analysis.

### Decision
1. Use Android's standard Storage Access Framework (`ActivityResultContracts.CreateDocument` / `ACTION_CREATE_DOCUMENT`).
2. Support JSON export (`application/json`) as the primary comprehensive format (structured, hierarchical, includes all device metadata and nested events), and CSV export (`text/csv`) for spreadsheet compatibility.
3. Stream data using `BufferedWriter` directly to the `OutputStream` obtained from `context.contentResolver.openOutputStream(uri)`, ensuring low memory footprint during export even with thousands of records.
4. Integrate an export prompt into the "Xoá toàn bộ lịch sử" dialog (`dialog_clear_all`) offering "Xuất dữ liệu trước" before confirmation.

### Rationale
- SAF works natively across Android 7.0 to 15+ without requiring legacy `WRITE_EXTERNAL_STORAGE` permissions.
- Direct streaming avoids allocating large JSON/CSV strings in heap memory.

---

## 4. Room Database Schema & Migration (Version 1 -> 2)

### Context
To optimize `WHERE device_id = :deviceId ORDER BY timestamp DESC LIMIT :limit OFFSET :offset` and deletion by timestamp, evaluate composite indexing.

### Decision
Bump `AppDatabase` version from `1` to `2`:
1. Add composite index `Index(value = ["device_id", "timestamp"])` on `EventEntity`.
2. Implement explicit `MIGRATION_1_2`:
   ```sql
   CREATE INDEX IF NOT EXISTS `index_events_device_id_timestamp` ON `events` (`device_id`, `timestamp`)
   ```
3. Register `MIGRATION_1_2` in `AppDatabase.ALL_MIGRATIONS`.
4. Export and commit Room schema JSON `app/schemas/com.example.data.AppDatabase/2.json`.
5. Maintain unit test validating migration fidelity from v1 to v2.

### Rationale
Strictly follows the non-destructive migration contract declared in `AppDatabase.kt`.
