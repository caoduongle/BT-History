# Feature Specification: Data Retention, History Pagination & Export

**Feature Branch**: `010-data-retention-and-export`  
**Created**: 2026-09-02  
**Status**: Draft  
**Input**: User description: "EventDao.getAllEventsFlow() nạp toàn bộ bảng `events` vào bộ nhớ mỗi lần thay đổi, không phân trang — vì BluetoothWatcherService chạy nền liên tục và ghi sự kiện suốt vòng đời sử dụng app (hàng tháng/năm), bảng này sẽ phình to không giới hạn. Đồng thời, DeviceViewModel.clearAllHistory() xoá vĩnh viễn toàn bộ lịch sử mà không có tuỳ chọn sao lưu/export trước đó. Hãy: 1. Thêm phân trang cho danh sách sự kiện hiển thị ở DeviceDetailScreen (lịch sử sự kiện của 1 thiết bị) và timeline tổng ở DeviceListScreen — dùng Paging 3 (androidx.paging) tích hợp với Room, hoặc tối thiểu là query có LIMIT/OFFSET thay cho getAllEventsFlow() tải hết bảng. 2. Thêm cấu hình \"giữ lịch sử N ngày gần nhất\" trong SettingsScreen (mặc định ví dụ 180 ngày, có thể chọn \"Không giới hạn\"), lưu qua PreferencesRepository, và một cơ chế dọn dữ liệu cũ hơn ngưỡng này (chạy khi service khởi động, hoặc qua WorkManager định kỳ mỗi ngày). 3. Thêm nút \"Xuất lịch sử ra file\" trong SettingsScreen, dùng Storage Access Framework (Intent.ACTION_CREATE_DOCUMENT) để xuất toàn bộ devices+events ra CSV hoặc JSON, và gợi ý người dùng bấm nút này trước khi xác nhận \"Xoá toàn bộ lịch sử\" trong dialog_clear_all. 4. Nếu cần đổi schema Room để hỗ trợ việc dọn dữ liệu hiệu quả (ví dụ thêm index), phải tuân thủ đúng quy trình migration bắt buộc đã ghi trong docstring của AppDatabase.kt (tăng version, viết Migration, thêm vào ALL_MIGRATIONS, export schema JSON mới, commit cùng lúc). 5. Viết test: dọn dữ liệu xoá đúng các dòng quá hạn và giữ nguyên dòng còn hạn; export ra đúng số dòng và định dạng hợp lệ. 6. Tạo specs/010-data-retention-and-export."

---

## Problem & Background Summary

1. **Unbounded Memory & Query Explosion**:
   - `EventDao.getAllEventsFlow()` and `getEventsByDeviceIdFlow()` query and load all event records from SQLite into Android heap memory as complete in-memory lists every time a new event is recorded.
   - Because `BluetoothWatcherService` runs continuously in the background and logs connect/disconnect events over weeks, months, or years, the `events` table grows indefinitely, creating severe risk of OutOfMemory (OOM) crashes, UI frame drops, and battery drain.
2. **Permanent Unrecoverable Data Loss**:
   - `DeviceViewModel.clearAllHistory()` permanently deletes all stored devices and events without offering users a way to backup or export their historical logs beforehand.
3. **Absence of Lifecycle Data Retention Policies**:
   - Users currently have no mechanism to configure automatic pruning of stale historical events (e.g. keeping only the last 30, 90, 180, or 365 days).
4. **Lack of Portable Data Export**:
   - Users cannot extract their timeline or device telemetry to portable open formats (JSON or CSV) for archival, spreadsheet analysis, or personal backup.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Smooth Paginated Timeline and Device History (Priority: P1) 🎯 MVP

As an active user with thousands of logged Bluetooth events, I want the event timeline on the main screen and the device history list on the detail screen to load incrementally in pages, so that the app opens instantly, scrolls smoothly without stutter, and never runs out of device memory.

**Why this priority**: Unbounded full-table loading is a critical performance bottleneck that directly causes UI stutter and OOM app crashes as history accumulates.

**Independent Test**: Populate the database with 5,000+ historical events. Open `DeviceListScreen` and `DeviceDetailScreen`. Verify that initial load completes within 500ms, only an initial batch of items is loaded into memory, and additional records load smoothly as the user scrolls toward the bottom.

**Acceptance Scenarios**:

1. **Given** a database containing thousands of recorded events, **When** the user opens the global timeline or a specific device's history, **Then** the UI displays the initial page of the most recent events without loading the entire table into memory.
2. **Given** the user is scrolling down the event history list, **When** approaching the end of the loaded items, **Then** the next batch of older events is fetched seamlessly and appended to the display.
3. **Given** a new Bluetooth connection/disconnection event occurs in real-time while viewing the timeline, **When** recorded, **Then** the new event appears reactively at the top of the list without triggering a full-table reload.

---

### User Story 2 - Configurable Data Retention & Automated Cleanup (Priority: P1) 🎯 MVP

As a privacy-conscious user or a user with limited device storage, I want to configure a retention period (e.g., 30 days, 90 days, 180 days, 365 days, or Unlimited) in Settings, so that historical events older than my selected threshold are automatically purged in the background without manual intervention.

**Why this priority**: Automated pruning prevents database bloating over prolonged usage (months/years) and respects user privacy preferences.

**Independent Test**: Set retention threshold to 30 days in `SettingsScreen`. Insert events older than 35 days alongside events recorded 5 days ago. Trigger cleanup (or restart service/worker). Verify that events older than 30 days are purged while recent events and parent devices remain intact.

**Acceptance Scenarios**:

1. **Given** the user visits `SettingsScreen`, **When** inspecting the "Lưu trữ & Dữ liệu" section, **Then** an option "Thời gian lưu lịch sử" is displayed showing current retention selection (defaulting to 180 days).
2. **Given** the user changes the retention policy (e.g. from 180 days to 90 days), **When** confirmed, **Then** the preference is saved to persistent storage and an immediate or scheduled purge executes to remove events older than 90 days.
3. **Given** the retention policy is set to "Không giới hạn" (Unlimited / 0 days), **When** the cleanup routine runs, **Then** no historical events are deleted.
4. **Given** the background service starts or a daily background job triggers, **When** evaluated, **Then** obsolete events past the threshold timestamp are deleted while parent device metadata is preserved.

---

### User Story 3 - Export History to Portable File (Priority: P1) 🎯 MVP

As an advanced user or someone preparing to switch devices, I want to export all my logged devices and event history to a JSON or CSV file using the Android Storage Access Framework, so that I can save my data locally, analyze it in external tools, or archive it securely.

**Why this priority**: Empowers users with ownership of their personal tracking data and provides a safe backup path before clearing history.

**Independent Test**: Navigate to `SettingsScreen`, click "Xuất lịch sử ra file", select a destination folder via Android document picker (`ACTION_CREATE_DOCUMENT`), and verify the exported file contains valid structured device and event records matching the database.

**Acceptance Scenarios**:

1. **Given** `SettingsScreen`, **When** the user clicks "Xuất lịch sử ra file", **Then** the Android system document creation picker opens with a suggested filename containing the current date (e.g. `bt_watcher_history_YYYYMMDD.json` or `.csv`).
2. **Given** the user confirms a save location, **When** export processing completes, **Then** a confirmation notification/toast appears with the total count of exported events and devices, and the file contains valid UTF-8 data.
3. **Given** the database is empty when export is triggered, **When** initiated, **Then** the user is informed with a friendly message that there is no data to export.

---

### User Story 4 - Pre-Wipe Backup Safety Prompt (Priority: P2)

As a user about to clear all history, I want the confirmation dialog to remind me to export my data first and offer a direct export shortcut, so that I don't accidentally wipe irreplaceable location and connection logs permanently.

**Why this priority**: Protects users from accidental permanent data destruction.

**Independent Test**: Open `SettingsScreen`, tap "Xoá toàn bộ lịch sử". Verify the dialog highlights that data deletion is irreversible, offers an "Xuất dữ liệu trước" action, and only proceeds to wipe when explicitly confirmed.

**Acceptance Scenarios**:

1. **Given** the user clicks "Xoá toàn bộ lịch sử", **When** the confirmation dialog appears, **Then** it prominently displays a warning that history cannot be restored and includes an option to export first.
2. **Given** the user taps "Xuất dữ liệu trước" from the dialog, **When** tapped, **Then** the export flow is launched before any deletion takes place.
3. **Given** the user proceeds to confirm wipe, **When** completed, **Then** the database is cleared and all screens refresh to an empty state.

---

## Functional Requirements

- **FR-001**: The system MUST implement paginated data retrieval for device-specific event logs in `DeviceDetailScreen` and global event logs in `DeviceListScreen`, loading records in bounded batches (e.g. 20-50 items per page) instead of reading the entire table into memory.
- **FR-002**: The pagination mechanism MUST preserve chronological ordering (`timestamp DESC`) and support reactive real-time updates when new events are inserted by the background service.
- **FR-003**: `PreferencesRepository` MUST define and persist a data retention setting (e.g., `KEY_HISTORY_RETENTION_DAYS`) with a default value of 180 days, supporting options: 30 days, 90 days, 180 days, 365 days, and Unlimited (0 / disabled).
- **FR-004**: `SettingsScreen` MUST expose a user-friendly selection control (dropdown or dialog selection) displaying human-readable labels for retention options ("30 ngày", "90 ngày", "180 ngày (Mặc định)", "1 năm", "Không giới hạn").
- **FR-005**: The system MUST provide an automated retention pruning routine that deletes events with `timestamp < (currentTimeMillis - retentionDays * 86,400,000L)` whenever retention is not set to Unlimited.
- **FR-006**: The retention pruning routine MUST execute automatically upon service startup and/or on a periodic daily schedule, without blocking UI operations or holding main thread execution.
- **FR-007**: Pruning obsolete event rows MUST NOT delete the associated `DeviceEntity` records, allowing users to retain known device names and paired MAC addresses even if older connection logs have aged out.
- **FR-008**: `SettingsScreen` MUST provide a "Xuất lịch sử ra file" action invoking Android Storage Access Framework (`Intent.ACTION_CREATE_DOCUMENT`) allowing the user to select the save destination.
- **FR-009**: The export engine MUST generate structured, UTF-8 encoded files in JSON or CSV format containing all paired devices and associated event history (timestamps, event types, coordinates, addresses, unexpected disconnect flags).
- **FR-010**: The "Xoá toàn bộ lịch sử" confirmation dialog MUST advise the user to export their data before wiping and provide a one-click transition to export.
- **FR-011**: If any database schema modifications or compound indices are introduced (e.g. `(device_id, timestamp)` index on `events`), the implementation MUST strictly follow the `AppDatabase.kt` contract: increment `version`, write an explicit `Migration`, add to `ALL_MIGRATIONS`, export schema JSON, and test migration fidelity.
- **FR-012**: Comprehensive automated unit and Robolectric tests MUST verify:
  1. Retention pruning correctly deletes only records older than the specified cutoff timestamp.
  2. Retention pruning leaves records newer than the cutoff timestamp and parent device records intact.
  3. Export generation outputs valid format, correct line/object counts, and escaped fields.
  4. Pagination queries return correct slice boundaries and ordering.

---

## Success Criteria

- **SC-001**: Memory usage for event history screens remains stable and bounded ($< 50\text{ MB}$ heap footprint) regardless of whether the database holds 100 or 100,000 events.
- **SC-002**: Initial timeline and device detail screen rendering completes in $< 300\text{ ms}$ on standard test hardware/emulators with 10,000 events stored.
- **SC-003**: 100% of events exceeding the configured retention threshold are purged during cleanup, with 0% data loss for events within the retention window.
- **SC-004**: 100% of exported files conform strictly to standard JSON or CSV RFC 4180 format and can be parsed by external tools without syntax errors.
- **SC-005**: Zero regressions across all existing test suites (`testDebugUnitTest` passes 100%).

---

## Key Entities & Data Schema

### 1. Data Retention Preference
- **Storage**: AndroidX DataStore (`preferencesDataStore`)
- **Key**: `history_retention_days` (Int)
- **Values**:
  - `30`: Keep last 30 days
  - `90`: Keep last 90 days
  - `180`: Keep last 180 days (Default)
  - `365`: Keep last 365 days
  - `0`: Unlimited (never auto-delete)

### 2. Export Data Structure
- **Device Record**: `id`, `name`, `mac_address`, `device_type`, `is_connected`, `last_seen_timestamp`, `last_latitude`, `last_longitude`, `last_location_address`.
- **Event Record**: `id`, `device_id`, `event_type`, `timestamp`, `timestamp_formatted`, `latitude`, `longitude`, `accuracy`, `location_address`, `is_unexpected_disconnect`.

---

## Assumptions & Dependencies

- **Storage Access Framework**: Export leverages standard Android SAF (`ActivityResultContracts.CreateDocument` or `Intent.ACTION_CREATE_DOCUMENT`), requiring no runtime storage permissions on modern Android (API 24-35+).
- **Pagination Strategy**: Uses AndroidX Paging 3 with Room Paging (`PagingSource`) or Room `LIMIT :limit OFFSET :offset` / keyset paging for bounded UI consumption.
- **Room Migration Integrity**: Existing database version is 1. If an index or schema change is required, database version increments to 2 with explicit `MIGRATION_1_2` committed alongside schema JSON.
