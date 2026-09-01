# Tasks: Classify Unexpected Disconnect

**Branch**: `002-classify-unexpected-disconnect`  
**Input**: Design artifacts from `specs/002-classify-unexpected-disconnect/`  

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify test execution environment

- [X] T001 Verify test execution environment with ./gradlew.bat testDebugUnitTest in root

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Define heuristic constants and adapter state tracking variables

- [X] T002 Define companion constant ADAPTER_OFF_HEURISTIC_WINDOW_MS and lastAdapterOffTimestamp in app/src/main/java/com/example/receiver/BluetoothEventReceiver.kt

**Checkpoint**: Foundation ready - receiver is prepared for intent handling and heuristic logic.

---

## Phase 3: User Story 1 - Eliminate False Alarms on Intentional Disablement (Priority: P1) 🎯 MVP

**Goal**: Accurately classify disconnects preceded by adapter turn-off as intentional (`isUnexpectedDisconnect = false`), suppressing false alert notifications.

**Independent Test**: Connect a peripheral, dispatch adapter turn-off broadcast followed by disconnect, and verify `isUnexpectedDisconnect` is `false` with 0 notifications.

### Implementation for User Story 1

- [X] T003 [US1] Intercept BluetoothAdapter.ACTION_STATE_CHANGED and update lastAdapterOffTimestamp in app/src/main/java/com/example/receiver/BluetoothEventReceiver.kt
- [X] T004 [US1] Calculate isUnexpectedDisconnect using isRecentAdapterOff heuristic in app/src/main/java/com/example/receiver/BluetoothEventReceiver.kt

**Checkpoint**: User Story 1 complete - intentional disconnects are recognized and false alarms are suppressed.

---

## Phase 4: User Story 2 - Maintainable Heuristic Threshold & Architectural Transparency (Priority: P2)

**Goal**: Document the heuristic algorithm, trade-offs, OS scheduling edge cases, and rationale in code comments.

**Independent Test**: Verify code comments in `BluetoothEventReceiver.kt` clearly articulate the heuristic window and trade-offs.

### Implementation for User Story 2

- [X] T005 [US2] Add architectural code comments explaining heuristic trade-offs and limits in app/src/main/java/com/example/receiver/BluetoothEventReceiver.kt

**Checkpoint**: User Stories 1 and 2 complete - heuristic is fully documented and maintainable.

---

## Phase 5: User Story 3 - Automated Regression Safeguard for Disconnect Scenarios (Priority: P3)

**Goal**: Create automated tests for both intentional adapter-off disconnects and sudden connection drops while Bluetooth is on.

**Independent Test**: Execute `.\gradlew.bat testDebugUnitTest --tests "com.example.DisconnectClassificationTest"` and verify all tests pass.

### Tests & Implementation for User Story 3

- [X] T006 [US3] Create DisconnectClassificationTest covering intentional and accidental disconnects in app/src/test/java/com/example/DisconnectClassificationTest.kt
- [X] T007 [US3] Execute test suite for DisconnectClassificationTest in app/src/test/java/com/example/DisconnectClassificationTest.kt

**Checkpoint**: User Story 3 complete - both disconnect flows are verified by automated tests.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Full regression suite validation and documentation update

- [X] T008 Run full unit test suite via ./gradlew.bat testDebugUnitTest in app/
- [X] T009 [P] Update feature documentation and checklists in specs/002-classify-unexpected-disconnect/

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Completed.
- **Foundational (Phase 2)**: Completed.
- **User Story 1 (Phase 3)**: Completed. Core heuristic classification implemented.
- **User Story 2 (Phase 4)**: Completed. Architectural code comments added.
- **User Story 3 (Phase 5)**: Completed. Disconnect classification unit tests implemented and passing.
- **Polish (Phase 6)**: Completed. Entire test suite passed.

### Implementation Strategy Verification
- **MVP**: Completed with User Story 1 (`T003`, `T004`).
- **All 9 Tasks Completed**: 100% finished and verified.
