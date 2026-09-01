# Feature Specification: Transactional Device Event Upsert

**Feature Branch**: `003-transactional-device-upsert`

**Created**: 2026-09-02

**Status**: Draft

**Input**: User description: "DeviceRepository.recordBluetoothEvent() đọc thiết bị theo MAC address rồi mới quyết định insert/update, không nằm trong transaction, có thể race nếu 2 coroutine cùng gọi song song (ví dụ nếu vẫn còn bug đăng ký receiver kép, hoặc nhiều sự kiện dồn dập). Hãy: 1. Bọc toàn bộ logic đọc-rồi-ghi trong recordBluetoothEvent bằng @androidx.room.Transaction (thêm hàm transaction phù hợp ở DeviceDao/EventDao hoặc dùng RoomDatabase.withTransaction). 2. Đảm bảo unique index trên mac_address vẫn được tôn trọng ngay cả khi có insert đồng thời (xử lý conflict resolution OnConflictStrategy phù hợp thay vì để exception rơi ra ngoài, hoặc dùng upsert pattern an toàn). 3. Viết test giả lập gọi recordBluetoothEvent đồng thời (coroutine) cho cùng một MAC address và xác nhận chỉ có đúng 1 DeviceEntity được tạo ra."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Atomic & Race-Free Bluetooth Event Recording (Priority: P1)

As an app user who pairs and connects Bluetooth accessories that may emit rapid burst events (such as ACL connect, A2DP profile connect, and Headset profile connect within milliseconds), I want the application to process these events reliably without database corruption, crashes, or duplicate device profiles, so that my device list always displays clean, single entries for each physical peripheral.

**Why this priority**: A read-then-write pattern outside a database transaction is vulnerable to race conditions when two or more coroutines execute concurrently. If two threads check for an unrecorded MAC address at the same moment, both see no existing device and attempt to insert new records, risking foreign key violation cascades, crashes, or inconsistent state.

**Independent Test**: Simulate concurrent `recordBluetoothEvent()` invocations across multiple parallel coroutines for an unrecorded MAC address; verify that all events are recorded successfully and only a single `DeviceEntity` exists in the database.

**Acceptance Scenarios**:

1. **Given** a new Bluetooth peripheral (MAC address not yet in the database), **When** two or more coroutines concurrently execute `recordBluetoothEvent()` for this device, **Then** the database executes the read-and-write operations atomically in a transaction, resulting in exactly one `DeviceEntity` created, with all generated `EventEntity` records properly linked to that device's ID.
2. **Given** an existing Bluetooth peripheral already in the database, **When** rapid concurrent events arrive for that device, **Then** the device state (connection status, last event timestamp, last location) is updated atomically without race condition anomalies or deadlocks.

---

### User Story 2 - Resilient Conflict Handling for Unique MAC Addresses (Priority: P2)

As a developer maintaining the application, I want the database insertion logic to handle unique index collisions on `mac_address` gracefully, so that unexpected concurrent inserts never throw unhandled `SQLiteConstraintException` errors.

**Why this priority**: Defensive programming requires that even if a transaction boundary is somehow stressed by external SQLite locks or timing anomalies, the database conflict resolution strategy cleanly falls back to updating the existing record rather than crashing the background service.

**Independent Test**: Attempt simultaneous insert operations violating the unique `mac_address` constraint; verify that conflict resolution handles the collision safely and returns a valid device ID without crashing.

**Acceptance Scenarios**:

1. **Given** an insert attempt on `DeviceEntity` with a duplicate MAC address, **When** a collision occurs on the unique index, **Then** the conflict strategy safely resolves the collision (e.g., via safe upsert / `OnConflictStrategy.IGNORE` with fallback re-query) without bubbling an exception out to the caller.

---

### User Story 3 - Concurrency Regression Test Suite (Priority: P3)

As an engineer on the project, I want automated concurrency tests that simulate parallel coroutine execution and verify data integrity, protecting against future regressions in the repository or DAO layers.

**Why this priority**: Concurrency bugs are notoriously difficult to reproduce in production. Having an automated test verifying parallel writes provides permanent regression protection.

**Independent Test**: Run the automated test suite executing parallel coroutines calling `recordBluetoothEvent` for the same MAC address and assert that `deviceDao.getAllDevicesFlow().first().size == 1`.

**Acceptance Scenarios**:

1. **Given** a clean database, **When** 5 or more coroutines call `recordBluetoothEvent()` simultaneously with the same MAC address on `Dispatchers.IO`, **Then** the test completes with zero exceptions, exactly 1 `DeviceEntity` exists in the database, and all events are persisted with matching `deviceId`.

---

### Edge Cases

- **Bursty Profile Connections**: When a headset connects, Android often emits `ACTION_ACL_CONNECTED`, A2DP `STATE_CONNECTED`, and Headset `STATE_CONNECTED` almost simultaneously across background threads. All three must resolve to the same device ID without conflict.
- **Concurrent Connect & Disconnect**: If a connect event and disconnect event are processed concurrently for the same device, the transaction isolation ensures serial execution without orphaned event records.
- **Foreign Key Cascade Safety**: Because `events` has a foreign key to `devices.id` with `onDelete = ForeignKey.CASCADE`, `OnConflictStrategy.REPLACE` on `devices` must NOT be used on unique index collisions, as `REPLACE` in SQLite deletes the existing row and triggers unwanted cascade deletions or foreign key constraint failures.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST wrap the read-then-write logic of `DeviceRepository.recordBluetoothEvent()` inside an atomic Room transaction (using `@Transaction` on the DAO or `RoomDatabase.withTransaction`).
- **FR-002**: The system MUST ensure that concurrent calls to `recordBluetoothEvent()` for the same MAC address produce exactly one `DeviceEntity` in the database.
- **FR-003**: The system MUST avoid `OnConflictStrategy.REPLACE` on `DeviceEntity` where foreign key relationships would be corrupted by row deletion and recreation.
- **FR-004**: The system MUST handle any unique index collision on `DeviceEntity.mac_address` gracefully without throwing uncaught `SQLiteConstraintException` errors.
- **FR-005**: All `EventEntity` records created during concurrent calls MUST reference the valid, existing `deviceId` of the single persisted `DeviceEntity`.
- **FR-006**: The system MUST provide an automated concurrency test verifying that concurrent `recordBluetoothEvent()` calls across parallel coroutines result in strictly 1 `DeviceEntity`.

### Key Entities

- **DeviceEntity**: Represents a Bluetooth peripheral. Key constraint: unique index on `mac_address`. Primary key: auto-generated `id: Long`.
- **EventEntity**: Represents a historical connection event. Key constraint: foreign key referencing `devices.id` with `CASCADE`.
- **DeviceRepository**: Repository orchestrating atomic device upsert and event insertion.
- **AppDatabase**: Room database managing transaction boundaries and SQLite connection pooling.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% deduplication of device entities: exactly 1 `DeviceEntity` created per unique MAC address under 100% of concurrent race condition simulations.
- **SC-002**: Zero `SQLiteConstraintException` crashes during concurrent event recording.
- **SC-003**: 100% foreign key consistency: 0 orphaned `EventEntity` records or foreign key constraint failures.
- **SC-004**: 100% pass rate on automated concurrency regression tests.

## Assumptions

- Room's built-in transaction support (`@Transaction` in DAO or `withTransaction` extension on `RoomDatabase`) provides ACID transaction guarantees over the underlying SQLite database.
- SQLite write transactions in Room serialize concurrent writes, ensuring that only one transaction can perform the read-then-write check at any given moment.
- The existing Room database schema (`devices` and `events` tables) remains backwards-compatible; no database version bump or migration is required.
