# Quickstart & Verification Guide: Data Retention, History Pagination & Export

**Branch**: `010-data-retention-and-export`  
**Date**: 2026-09-02  

---

## 1. Automated Verification Commands

Run targeted Robolectric unit tests verifying pagination, pruning, export, and Room schema migration:

```bash
# 1. Run Data Retention & Export Unit Tests
./gradlew.bat testDebugUnitTest --tests "com.example.DataRetentionAndExportTest"

# 2. Run Database Migration Test from v1 to v2
./gradlew.bat testDebugUnitTest --tests "com.example.DatabaseMigrationFrameworkTest"

# 3. Run Full Project Test Suite
./gradlew.bat testDebugUnitTest
```

---

## 2. Verification Scenarios

### Scenario 1: Event History Pagination
1. Insert 100 test events into Room database.
2. Observe `DeviceViewModel.paginatedTimelineEvents`. Initial emission contains only the first page (e.g. 50 items).
3. Invoke `viewModel.loadMoreTimelineEvents()`. Observe that next 50 items are appended, totaling 100 items, and `hasMoreTimelineEvents` transitions to `false`.

### Scenario 2: Data Retention Pruning
1. Set retention threshold to 30 days (`viewModel.setHistoryRetentionDays(30)`).
2. Insert an event with `timestamp = System.currentTimeMillis() - 40 * 86,400,000L` (40 days old).
3. Insert another event with `timestamp = System.currentTimeMillis() - 5 * 86,400,000L` (5 days old).
4. Run `viewModel.pruneExpiredEvents()`.
5. Verify that the 40-day-old event is deleted, the 5-day-old event remains, and the device entity is intact.

### Scenario 3: History Export to File
1. Prepare known devices and events in the database.
2. Call `viewModel.exportHistoryToUri(tempFileUri, ExportFormat.JSON)`.
3. Parse the generated file content:
   - Must be valid JSON matching the schema in `data-model.md`.
   - Total device count and event count match database records.
   - All GPS coordinates, timestamps, and address strings match.

### Scenario 4: Database Migration v1 -> v2
1. Verify Room schema JSON for version 2 is exported to `app/schemas/com.example.data.AppDatabase/2.json`.
2. Verify `MIGRATION_1_2` successfully applies without errors or data loss.
