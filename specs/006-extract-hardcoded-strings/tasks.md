# Tasks: Extract Hardcoded Strings to Resource Catalog

**Branch**: `006-extract-hardcoded-strings`  
**Input**: Design artifacts from `specs/006-extract-hardcoded-strings/`  

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Baseline verification

- [x] T001 Verify baseline build status with ./gradlew.bat testDebugUnitTest in root

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Define the master string catalog in `strings.xml`

- [x] T002 Define comprehensive string resources with semantic keys in app/src/main/res/values/strings.xml

**Checkpoint**: Foundation ready - all required string IDs exist and can be referenced in code.

---

## Phase 3: User Story 1 - Centralized Strings in Utilities & ViewModel (Priority: P1) 🎯 MVP

**Goal**: Update core helpers, services, and ViewModel enums to use `R.string.*` resources.

**Independent Test**: Verify notification and helper strings compile cleanly and reference `R.string.*`.

### Implementation for User Story 1

- [x] T003 [US1] Update NotificationHelper.kt, LocationHelper.kt, BluetoothHelper.kt, and TimeFormatter.kt to use context.getString()
- [x] T004 [US1] Update TimeFilter enum in DeviceViewModel.kt to declare @StringRes val titleRes: Int
- [x] T005 [US1] Align fallback device names in DeviceRepository.kt

**Checkpoint**: Core services, notifications, and ViewModel enums are fully localized.

---

## Phase 4: User Story 2 - Centralized Strings in Jetpack Compose UI (Priority: P2)

**Goal**: Replace all hardcoded string literals in Compose UI screens and components with `stringResource()`.

**Independent Test**: Inspect Compose files and confirm `stringResource(R.string.*)` is used for all text elements.

### Implementation for User Story 2

- [x] T006 [US2] Update UI components in app/src/main/java/com/example/ui/components/ (DeviceCard, TimelineItem, WarningBanner)
- [x] T007 [US2] Update UI screens in app/src/main/java/com/example/ui/screens/ (DeviceListScreen, DeviceDetailScreen, SettingsScreen, PermissionOnboardingScreen)

**Checkpoint**: 100% of Compose UI is driven by centralized string resources.

---

## Phase 5: Polish & Build Verification

**Purpose**: Regression testing and build validation

- [x] T008 Run ./gradlew.bat testDebugUnitTest and ./gradlew.bat assembleDebug to verify full functional and visual parity
- [x] T009 [P] Update feature documentation and checklists in specs/006-extract-hardcoded-strings/

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Baseline verification (`T001`).
- **Foundational (Phase 2)**: Create master string catalog in `strings.xml` (`T002`). Blocks all subsequent phases.
- **User Story 1 (Phase 3)**: Utilities, helpers, and ViewModel (`T003`, `T004`, `T005`).
- **User Story 2 (Phase 4)**: Compose UI screens and widgets (`T006`, `T007`).
- **Polish (Phase 5)**: Tests and APK compilation (`T008`, `T009`).

---

## Implementation Strategy

### MVP First (Phases 2 & 3)
1. Add strings to `strings.xml`.
2. Update backend utilities and notifications (`NotificationHelper`, `LocationHelper`, `BluetoothHelper`, `DeviceViewModel`).

### Incremental Delivery
1. Foundational catalog established.
2. System & background services localized.
3. UI screens & components localized.
4. Full regression and unit tests pass.
