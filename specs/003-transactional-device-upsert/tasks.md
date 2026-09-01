# Tasks: Transactional Device Event Upsert

**Branch**: `003-transactional-device-upsert`  
**Input**: Design artifacts from `specs/003-transactional-device-upsert/`  

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify build and test execution environment

- [X] T001 Verify test execution environment with ./gradlew.bat testDebugUnitTest in root

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Update DAO conflict strategy to prevent destructive cascade deletions

- [X] T002 Replace OnConflictStrategy.REPLACE with OnConflictStrategy.IGNORE in app/src/main/java/com/example/data/dao/DeviceDao.kt

**Checkpoint**: Foundation ready - Device insertion cannot accidentally delete rows on MAC conflict.

---

## Phase 3: User Story 1 - Atomic & Race-Free Event Recording (Priority: P1) 🎯 MVP

**Goal**: Guarantee thread-safe read-then-write execution in `DeviceRepository.recordBluetoothEvent()` using `RoomDatabase.withTransaction`.

**Independent Test**: Execute parallel calls to `recordBluetoothEvent` and verify atomic execution.

### Implementation for User Story 1

- [X] T003 [US1] Inject AppDatabase into DeviceRepository and wrap recordBluetoothEvent in withTransaction in app/src/main/java/com/example/data/repository/DeviceRepository.kt
- [X] T004 [US1] Implement safe upsert fallback (re-query on conflict) in app/src/main/java/com/example/data/repository/DeviceRepository.kt
- [X] T005 [US1] Update repository initialization in app/src/main/java/com/example/BtWatcherApplication.kt

**Checkpoint**: User Story 1 complete - all read-and-write cycles are serialized inside an ACID transaction.

---

## Phase 4: User Story 2 - Resilient Conflict Handling (Priority: P2)

**Goal**: Verify that unique MAC collisions never throw unhandled `SQLiteConstraintException` errors.

**Independent Test**: Verify defensive re-query handling and non-destructive upsert in `DeviceRepository.kt`.

### Implementation for User Story 2

- [X] T006 [US2] Review and harden conflict error handling and edge case recovery in app/src/main/java/com/example/data/repository/DeviceRepository.kt

**Checkpoint**: User Story 2 complete - database constraint collisions gracefully resolve to update.

---

## Phase 5: User Story 3 - Concurrency Regression Test Suite (Priority: P3)

**Goal**: Automated test simulating 10 parallel coroutines calling `recordBluetoothEvent` for the same MAC address, verifying strictly 1 `DeviceEntity` is created.

**Independent Test**: Execute `.\gradlew.bat testDebugUnitTest --tests "com.example.ConcurrentEventRecordingTest"` and verify all tests pass.

### Tests & Implementation for User Story 3

- [X] T007 [US3] Create ConcurrentEventRecordingTest in app/src/test/java/com/example/ConcurrentEventRecordingTest.kt
- [X] T008 [US3] Execute test suite for ConcurrentEventRecordingTest in app/src/test/java/com/example/ConcurrentEventRecordingTest.kt

**Checkpoint**: User Story 3 complete - concurrency safety is confirmed by automated tests.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Full regression suite validation and documentation update

- [X] T009 Run full unit test suite via ./gradlew.bat testDebugUnitTest in app/
- [X] T010 [P] Update feature documentation and checklists in specs/003-transactional-device-upsert/

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Completed.
- **Foundational (Phase 2)**: Completed. Updated `DeviceDao` conflict strategy (`T002`).
- **User Story 1 (Phase 3)**: Completed. Implemented `withTransaction` and safe upsert (`T003`, `T004`, `T005`).
- **User Story 2 (Phase 4)**: Completed. Conflict handling hardened (`T006`).
- **User Story 3 (Phase 5)**: Completed. Multi-coroutine tests passing (`T007`, `T008`).
- **Polish (Phase 6)**: Completed. Full unit test suite passing (`T009`, `T010`).

### Implementation Strategy Verification
- **MVP**: Completed with User Story 1 (`T003` - `T005`).
- **All 10 Tasks Completed**: 100% finished and verified.
