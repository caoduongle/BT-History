# Tasks: Data Retention, History Pagination & Export

**Branch**: `010-data-retention-and-export`  
**Input**: Design artifacts from `specs/010-data-retention-and-export/`  

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify baseline build and test execution environment before making code changes

- [X] T001 Verify baseline unit test status with `./gradlew.bat testDebugUnitTest` in root

---

## Phase 2: Foundational (Room v2 Migration & Data Layer)

**Purpose**: Core schema evolution, composite index, DAO paged queries, and DataStore retention preference that ALL user stories depend on

**⚠️ CRITICAL**: Must complete before implementing User Stories 1 through 4

- [X] T002 [P] Add composite index `(device_id, timestamp)` to `EventEntity` in `app/src/main/java/com/example/data/entity/EventEntity.kt`
- [X] T003 Bump database version to 2, implement `MIGRATION_1_2`, and register in `ALL_MIGRATIONS` in `app/src/main/java/com/example/data/AppDatabase.kt`
- [X] T004 Add paged queries (`getEventsPaged`, `getEventsByDeviceIdPaged`), count queries, and `deleteEventsOlderThan` in `app/src/main/java/com/example/data/dao/EventDao.kt`
- [X] T005 [P] Add `KEY_HISTORY_RETENTION_DAYS`, `historyRetentionDaysFlow`, and `setHistoryRetentionDays()` in `app/src/main/java/com/example/data/repository/PreferencesRepository.kt`
- [X] T006 [P] Add string resources for retention periods, export actions, and pre-wipe warning in `app/src/main/res/values/strings.xml`

**Checkpoint**: Foundation ready - Database schema v2, paged queries, and retention settings in place.

---

## Phase 3: User Story 1 - Smooth Paginated Timeline and Device History (Priority: P1) 🎯 MVP

**Goal**: Deliver bounded incremental loading for both the global timeline and device-specific history lists, eliminating full-table memory consumption.

**Independent Test**: Load database with historical events. Verify that initial render only loads the first page, and subsequent pages load smoothly upon scrolling.

### Implementation for User Story 1

- [X] T007 [US1] Expose paged event flows (`paginatedTimelineEvents`, `paginatedDeviceEvents`, `loadMoreTimelineEvents`, `loadMoreDeviceEvents`) in `app/src/main/java/com/example/ui/viewmodel/DeviceViewModel.kt`
- [X] T008 [US1] Update `DeviceListScreen.kt` to observe `paginatedTimelineEvents` and implement infinite scroll pagination in the History tab in `app/src/main/java/com/example/ui/screens/DeviceListScreen.kt`
- [X] T009 [US1] Update `DeviceDetailScreen.kt` to observe `paginatedDeviceEvents` and implement infinite scroll pagination in `app/src/main/java/com/example/ui/screens/DeviceDetailScreen.kt`

**Checkpoint**: User Story 1 complete - Event lists load incrementally with bounded memory footprint.

---

## Phase 4: User Story 2 - Configurable Data Retention & Automated Cleanup (Priority: P1) 🎯 MVP

**Goal**: Allow users to configure retention duration (30, 90, 180 days, 1 year, or Unlimited) with background pruning of expired events.

**Independent Test**: Configure retention to 30 days. Trigger pruning with older and newer events. Verify older events are deleted while newer events and parent device records remain.

### Implementation for User Story 2

- [X] T010 [US2] Wire retention configuration StateFlow and `pruneExpiredEvents()` in `app/src/main/java/com/example/ui/viewmodel/DeviceViewModel.kt`
- [X] T011 [US2] Add automated retention pruning routine on service startup (`onStartCommand`) in `app/src/main/java/com/example/service/BluetoothWatcherService.kt`
- [X] T012 [US2] Add "Thời gian lưu lịch sử" configuration row and single-choice selection dialog in `app/src/main/java/com/example/ui/screens/SettingsScreen.kt`

**Checkpoint**: User Story 2 complete - Users can configure retention duration and stale events are pruned automatically.

---

## Phase 5: User Story 3 - Export History to Portable File (Priority: P1) 🎯 MVP

**Goal**: Provide local data export via Android Storage Access Framework (`ACTION_CREATE_DOCUMENT`) to JSON and CSV formats.

**Independent Test**: Trigger export in Settings, select file destination via document picker, verify generated file contains valid UTF-8 data matching Room records.

### Implementation for User Story 3

- [X] T013 [P] [US3] Create `ExportHelper.kt` with streaming JSON and CSV serializers in `app/src/main/java/com/example/util/ExportHelper.kt`
- [X] T014 [US3] Implement `exportHistoryToUri(uri: Uri, format: ExportFormat)` in `app/src/main/java/com/example/ui/viewmodel/DeviceViewModel.kt`
- [X] T015 [US3] Add "Xuất lịch sử ra file" row with `ActivityResultContracts.CreateDocument` SAF launcher in `app/src/main/java/com/example/ui/screens/SettingsScreen.kt`

**Checkpoint**: User Story 3 complete - Users can export full device and tracking history to local JSON/CSV files.

---

## Phase 6: User Story 4 - Pre-Wipe Backup Safety Prompt (Priority: P2)

**Goal**: Advise users to back up their data before wiping history, offering a direct export shortcut in the confirmation dialog.

**Independent Test**: Tap "Xoá toàn bộ lịch sử" in Settings. Verify dialog includes "Xuất dữ liệu trước" button which triggers export before wiping.

### Implementation for User Story 4

- [X] T016 [US4] Update `dialog_clear_all` in `SettingsScreen.kt` to advise backup first and provide "Xuất dữ liệu trước" shortcut button in `app/src/main/java/com/example/ui/screens/SettingsScreen.kt`

**Checkpoint**: User Story 4 complete - Users are protected against accidental unrecoverable data deletion.

---

## Phase 7: Testing, Verification & Polish

**Purpose**: Automated test suite for pruning, export, and migration, full build validation, and documentation

- [X] T017 [P] Update `DatabaseMigrationFrameworkTest.kt` to test Room migration from v1 to v2 with sample data in `app/src/test/java/com/example/DatabaseMigrationFrameworkTest.kt`
- [X] T018 [P] Create `DataRetentionAndExportTest.kt` verifying:
  1. Retention pruning deletes records older than cutoff and preserves newer records & devices.
  2. Export generates valid JSON/CSV with exact match of counts and values.
  3. Pagination queries return correct slice boundaries and ordering in `app/src/test/java/com/example/DataRetentionAndExportTest.kt`
- [X] T019 Run full test suite `./gradlew.bat testDebugUnitTest` and export Room schema JSON `app/schemas/com.example.data.AppDatabase/2.json`
- [X] T020 [P] Update `README.md` documenting the Data Retention policy, Storage Access Framework export, and pagination features in `README.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - starts immediately (`T001`).
- **Foundational (Phase 2)**: Depends on Setup (`T001`) - **BLOCKS** all user stories (`T002`-`T006`).
- **User Story 1 (Phase 3)**: Depends on Foundational (`T002`-`T006`). Paged queries & UI (`T007`-`T009`).
- **User Story 2 (Phase 4)**: Depends on Foundational (`T002`-`T006`). Retention setting & pruning (`T010`-`T012`).
- **User Story 3 (Phase 5)**: Depends on Foundational (`T002`-`T006`). Export serializers & SAF launcher (`T013`-`T015`).
- **User Story 4 (Phase 6)**: Depends on User Story 3 (`T013`-`T015`). Pre-wipe dialog integration (`T016`).
- **Testing & Polish (Phase 7)**: Depends on all user stories (`T017`-`T020`).

### Parallel Opportunities

- Within Phase 2: `T002`, `T005`, and `T006` can be edited in parallel.
- Within Phase 5: `T013` (`ExportHelper.kt`) can be developed independently of UI tasks.
- Once Phase 2 completes: User Story 1, User Story 2, and User Story 3 can proceed concurrently.
- Within Phase 7: `T017`, `T018`, and `T020` can run in parallel.
