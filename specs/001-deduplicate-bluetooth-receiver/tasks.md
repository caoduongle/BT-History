# Tasks: Deduplicate Bluetooth Receiver

**Branch**: `001-deduplicate-bluetooth-receiver`  
**Input**: Design artifacts from `specs/001-deduplicate-bluetooth-receiver/`  

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and build environment verification

- [X] T001 Verify project build environment and wrapper execution with ./gradlew --version in root

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Validate baseline codebase and ensure no conflicting components

- [X] T002 Inspect existing BluetoothWatcherService and BluetoothEventReceiver registration in app/src/main/java/com/example/service/BluetoothWatcherService.kt

**Checkpoint**: Baseline verified - user story implementation can begin

---

## Phase 3: User Story 1 - Single Processing of Bluetooth Events (Priority: P1) 🎯 MVP

**Goal**: Eliminate duplicate event handling by removing the static manifest receiver declaration so that Bluetooth broadcasts are intercepted only once.

**Independent Test**: Build the app and confirm `AndroidManifest.xml` contains no `<receiver android:name=".receiver.BluetoothEventReceiver">` element.

### Implementation for User Story 1

- [X] T003 [US1] Remove static BluetoothEventReceiver declaration from app/src/main/AndroidManifest.xml

**Checkpoint**: User Story 1 complete - system broadcast duplicate dispatch via AndroidManifest is removed.

---

## Phase 4: User Story 2 - Safe Lifecycle Management for Background Monitoring (Priority: P2)

**Goal**: Ensure `BluetoothWatcherService` dynamically registers `BluetoothEventReceiver` on start, cleanly unregisters on destroy without resource or context leaks, and documents why dynamic registration is used over static manifest registration.

**Independent Test**: Verify service registration in `onCreate()`, safe unregistration in `onDestroy()`, and confirm architectural comments in `BluetoothWatcherService.kt`.

### Implementation for User Story 2

- [X] T004 [US2] Review and harden dynamic receiver registration and teardown in app/src/main/java/com/example/service/BluetoothWatcherService.kt
- [X] T005 [US2] Add architectural code comments in app/src/main/java/com/example/service/BluetoothWatcherService.kt explaining Android 8+ implicit broadcast rationale

**Checkpoint**: User Stories 1 and 2 complete - background monitoring is leak-free and compliant with Android 8+ background limits.

---

## Phase 5: User Story 3 - Automated Regression Safeguard (Priority: P3)

**Goal**: Implement an automated Robolectric test verifying that dispatching a single `ACTION_ACL_CONNECTED` broadcast creates exactly 1 `EventEntity` record in the database.

**Independent Test**: Execute `./gradlew testDebugUnitTest --tests "com.example.BluetoothDeduplicationTest"` and verify the test passes.

### Tests & Implementation for User Story 3

- [X] T006 [US3] Create Robolectric test in app/src/test/java/com/example/BluetoothDeduplicationTest.kt
- [X] T007 [US3] Execute test suite for BluetoothDeduplicationTest in app/src/test/java/com/example/BluetoothDeduplicationTest.kt

**Checkpoint**: User Story 3 complete - automated test confirms exactly 1 `EventEntity` is created per event.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Project validation and documentation updates

- [X] T008 Run full unit test suite via ./gradlew testDebugUnitTest in app/
- [X] T009 [P] Update feature documentation and checklists in specs/001-deduplicate-bluetooth-receiver/

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Completed.
- **Foundational (Phase 2)**: Completed.
- **User Story 1 (Phase 3)**: Completed. Removed duplicate manifest receiver.
- **User Story 2 (Phase 4)**: Completed. Hardened service lifecycle and added rationale documentation.
- **User Story 3 (Phase 5)**: Completed. Validated single-event persistence via automated test.
- **Polish (Phase 6)**: Completed. Full unit test suite passed.

### Implementation Strategy Verification
- **MVP**: Completed with User Story 1 (`T003`).
- **Incremental Delivery**: US1 (Deduplication) -> US2 (Lifecycle & Architecture Docs) -> US3 (Automated Regression Test Suite).
- **All 9 Tasks Completed**: 100% finished and verified.
