# Implementation Plan: Data Retention, History Pagination & Export

**Branch**: `010-data-retention-and-export` | **Date**: 2026-09-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/010-data-retention-and-export/spec.md`

---

## Summary

Resolve unbounded memory growth and unrecoverable data loss in BT Watcher by implementing:
1. **Incremental History Pagination**: Bounded Room queries (`LIMIT :limit OFFSET :offset`) for both single-device history (`DeviceDetailScreen`) and global timeline (`DeviceListScreen`), maintaining $< 50\text{ MB}$ heap footprint regardless of database size.
2. **Lifecycle Data Retention**: Configurable history retention policy (30, 90, 180 days [default], 365 days, Unlimited) stored in `PreferencesRepository`, with non-blocking automated background pruning that deletes obsolete event rows while preserving parent device metadata.
3. **Storage Access Framework (SAF) File Export**: Secure local data export (`ACTION_CREATE_DOCUMENT`) to JSON and CSV formats directly from `SettingsScreen` and pre-wipe safety prompt in `ClearAllHistoryDialog`.
4. **Strict Schema Migration (Room v1 -> v2)**: Bumping database version to 2, introducing composite index `(device_id, timestamp)`, implementing `MIGRATION_1_2` in `ALL_MIGRATIONS`, and exporting Room schema JSON.

---

## Technical Context

**Language/Version**: Kotlin 2.2.10 (JVM 11 target)  
**Primary Dependencies**: AndroidX Compose (BOM 2024.09.00), Room 2.7.0 (`room-runtime`, `room-ktx`), DataStore Preferences 1.1.7, Kotlinx Coroutines 1.10.2  
**Storage**: SQLite via Room Database (`AppDatabase`), AndroidX DataStore (`PreferencesRepository`)  
**Testing**: JUnit 4, Robolectric 4.16.1, Compose UI Test JUnit4  
**Target Platform**: Android (API 24 - Android 7.0 through API 36)  
**Project Type**: Native Android Application (Single module `:app`)  
**Performance Goals**: Sub-300ms screen rendering with 10,000+ events; memory bounded under 50 MB heap.  
**Constraints**: Zero destructive schema migration (`fallbackToDestructiveMigration` disabled); Storage Access Framework (SAF) without legacy runtime storage permissions.  
**Scale/Scope**: Scales gracefully from small personal logs to 100,000+ continuous background Bluetooth tracking events.  

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Constitution template contains default placeholders; skipped gracefully with zero violations.
- Strict architecture rules defined in `AppDatabase.kt` (non-destructive migrations, mandatory schema export) are fully adhered to.

---

## Project Structure

### Documentation (this feature)

```text
specs/010-data-retention-and-export/
├── spec.md              # Feature specification
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 research and architectural decisions
├── data-model.md        # Phase 1 data schema, entities, and migration definitions
├── quickstart.md        # Phase 1 validation scenarios and test commands
├── contracts/
│   └── export-and-retention-contract.md # Interfaces, models, and UI contracts
└── checklists/
    └── requirements.md  # Spec quality validation checklist
```

### Source Code Impact (repository root)

```text
app/
├── schemas/
│   └── com.example.data.AppDatabase/
│       ├── 1.json                              # Existing Room schema
│       └── 2.json                              # [NEW] Bumped Room schema v2
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── data/
│   │   │   │   ├── AppDatabase.kt              # [MODIFY] Bump version 2, MIGRATION_1_2 in ALL_MIGRATIONS
│   │   │   │   ├── dao/EventDao.kt             # [MODIFY] Add paged queries & deleteEventsOlderThan
│   │   │   │   ├── entity/EventEntity.kt       # [MODIFY] Add composite index (device_id, timestamp)
│   │   │   │   └── repository/
│   │   │   │       ├── DeviceRepository.kt     # [MODIFY] Add paged events & prune methods
│   │   │   │       └── PreferencesRepository.kt# [MODIFY] Add KEY_HISTORY_RETENTION_DAYS
│   │   │   ├── service/
│   │   │   │   └── BluetoothWatcherService.kt  # [MODIFY] Trigger pruning on start & periodic tick
│   │   │   ├── ui/
│   │   │   │   ├── screens/
│   │   │   │   │   ├── DeviceDetailScreen.kt   # [MODIFY] Infinite scroll / paginated device events
│   │   │   │   │   ├── DeviceListScreen.kt     # [MODIFY] Infinite scroll / paginated timeline
│   │   │   │   │   └── SettingsScreen.kt       # [MODIFY] Retention selector, SAF Export, Pre-wipe prompt
│   │   │   │   └── viewmodel/
│   │   │   │       └── DeviceViewModel.kt      # [MODIFY] Paged states, exportHistory, retention flows
│   │   │   └── util/
│   │   │       └── ExportHelper.kt             # [NEW] JSON & CSV streaming serialisers
│   │   └── res/values/strings.xml              # [MODIFY] Vietnamese labels for retention & export
│   └── test/
│       └── java/com/example/
│           ├── DataRetentionAndExportTest.kt   # [NEW] Tests for pruning, export, pagination
│           └── DatabaseMigrationFrameworkTest.kt# [MODIFY] Validate v1 -> v2 migration
```

---

## Planned Execution Phases

### Phase 1: Storage Layer & Room v2 Migration
- Add composite index on `EventEntity`.
- Bump `AppDatabase` version to 2, write `MIGRATION_1_2`, update `ALL_MIGRATIONS`.
- Add `getEventsPaged`, `getEventsByDeviceIdPaged`, and `deleteEventsOlderThan` in `EventDao`.
- Export new schema `2.json` and verify `DatabaseMigrationFrameworkTest`.

### Phase 2: DataStore & Repository Retention Logic
- Add `KEY_HISTORY_RETENTION_DAYS` in `PreferencesRepository`.
- Expose retention methods in `DeviceRepository` and `DeviceViewModel`.
- Wire background pruning into `BluetoothWatcherService.onStartCommand()`.

### Phase 3: Export Engine & Utilities
- Implement `ExportHelper` with streaming JSON and CSV serializers.
- Expose `exportHistoryToUri` in `DeviceViewModel`.

### Phase 4: UI Enhancements
- **SettingsScreen**: Add Retention dropdown/dialog, Add "Xuất lịch sử ra file" with SAF document launcher.
- **Clear All Dialog**: Update dialog to include "Xuất dữ liệu trước" shortcut button.
- **DeviceListScreen & DeviceDetailScreen**: Connect paginated flows with incremental loading upon scrolling.

### Phase 5: Testing & Verification
- Create `DataRetentionAndExportTest.kt` verifying:
  - Pruning accurately deletes events older than threshold and preserves younger events & device rows.
  - Export generates valid JSON/CSV with exact match of counts and values.
  - Pagination loads incremental slices accurately.
- Run complete project test suite `./gradlew.bat testDebugUnitTest`.
