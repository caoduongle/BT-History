# Tasks: Gate Simulation Controls & Developer Mode Protection

**Branch**: `009-gate-simulate-feature`  
**Input**: Design artifacts from `specs/009-gate-simulate-feature/`  

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify baseline build and test execution environment before making code changes

- [x] T001 Verify baseline unit test status with `./gradlew.bat testDebugUnitTest` in root

---

## Phase 2: Foundational (Data Layer & ViewModel Infrastructure)

**Purpose**: DataStore persistence and state flows that gate simulation across all screens

**⚠️ CRITICAL**: All UI screens and tests depend on this centralized state flow

- [x] T002 Add `KEY_DEVELOPER_MODE_ENABLED`, `isDeveloperModeEnabledFlow`, and `setDeveloperModeEnabled(enabled: Boolean)` in `app/src/main/java/com/example/data/repository/PreferencesRepository.kt`
- [x] T003 Expose `isDeveloperModeEnabled: StateFlow<Boolean>` and `isSimulationAvailable: StateFlow<Boolean>` (`BuildConfig.DEBUG || isDeveloperModeEnabled`) in `app/src/main/java/com/example/ui/viewmodel/DeviceViewModel.kt`
- [x] T004 [P] Add string resources for Developer Mode countdown and activation Toasts and version display in `app/src/main/res/values/strings.xml`

**Checkpoint**: Foundation ready - ViewModel exposes reactive `isSimulationAvailable` state.

---

## Phase 3: User Story 1 - Production Gating for End Users (Priority: P1) 🎯 MVP

**Goal**: Ensure production release builds hide all simulation controls (FAB, empty-state button, detail menu items) by default.

**Independent Test**: Set `isSimulationAvailable = false` in tests; verify `simulate_fab`, `btn_add_sample_data`, and detail simulation menu items are completely absent from the UI.

### Implementation for User Story 1

- [x] T005 [US1] Gate "Mô phỏng" FAB (`testTag("simulate_fab")`) and "Thêm dữ liệu mẫu để thử nghiệm" button behind `isSimulationAvailable` in `app/src/main/java/com/example/ui/screens/DeviceListScreen.kt`
- [x] T006 [US1] Gate "Mô phỏng Kết nối" and "Mô phỏng Ngắt kết nối" overflow menu actions behind `isSimulationAvailable` in `app/src/main/java/com/example/ui/screens/DeviceDetailScreen.kt`

**Checkpoint**: User Story 1 complete - simulation entry points are completely hidden when simulation is not available.

---

## Phase 4: User Story 2 - Hidden Developer Mode Unlock in Settings (Priority: P1) 🎯 MVP

**Goal**: Provide internal QA and developers with the 7-tap easter egg on the App Version row in `SettingsScreen` to enable Developer Mode.

**Independent Test**: Navigate to `SettingsScreen`, tap the App Version row 7 times, verify countdown toasts appear on taps 4-6, activation toast appears on tap 7, and `isDeveloperModeEnabled` becomes `true`.

### Implementation for User Story 2

- [x] T007 [US2] Add App Version row at bottom of `SettingsScreen.kt` with 7-tap counter logic, countdown Toasts, activation Toast, and `viewModel.setDeveloperModeEnabled(true)` in `app/src/main/java/com/example/ui/screens/SettingsScreen.kt`

**Checkpoint**: User Story 2 complete - internal testers can seamlessly unlock simulation controls on release builds.

---

## Phase 5: User Story 3 & 4 - Automated Testing & Documentation (Priority: P1)

**Goal**: Create automated Robolectric tests verifying UI gating and update `README.md` to document the 7-tap unlock flow.

**Independent Test**: Run `./gradlew.bat testDebugUnitTest` and verify `SimulationGatingTest` passes; inspect `README.md`.

### Implementation for User Story 3 & 4

- [x] T008 [US4] Create Robolectric test `SimulationGatingTest.kt` verifying hidden simulation controls when disabled, visible when enabled, and preference unlock flow in `app/src/test/java/com/example/SimulationGatingTest.kt`
- [x] T009 [US4] Update `README.md` to document the 7-tap Developer Mode activation procedure for testing on Android Emulators in `README.md`

**Checkpoint**: User Story 4 complete - automated regression protection and clear documentation in place.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Full regression testing and final documentation validation

- [x] T010 Run full unit test suite with `./gradlew.bat testDebugUnitTest` across all SDK configurations
- [x] T011 [P] Update feature documentation and checklists in `specs/009-gate-simulate-feature/`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately (`T001`).
- **Foundational (Phase 2)**: Depends on Setup (`T001`) - **BLOCKS** all user stories (`T002`, `T003`, `T004`).
- **User Story 1 (Phase 3)**: Depends on Foundational (`T002`-`T004`). Gating UI controls (`T005`, `T006`).
- **User Story 2 (Phase 4)**: Depends on Foundational (`T002`-`T004`). Settings unlock gesture (`T007`).
- **User Story 3 & 4 (Phase 5)**: Depends on User Story 1 & 2. Automated tests & docs (`T008`, `T009`).
- **Polish (Phase 6)**: Depends on all user stories being complete (`T010`, `T011`).

### Parallel Opportunities

- Within Phase 2: `T004` (strings) can be edited in parallel with `T002` and `T003`.
- Within Phase 3: `T005` (`DeviceListScreen.kt`) and `T006` (`DeviceDetailScreen.kt`) can be developed in parallel.
- Within Phase 5: `T009` (README) can be prepared in parallel with `T008` (Test code).
- Within Phase 6: `T011` (Docs) can be prepared in parallel with `T010` (Build validation).
