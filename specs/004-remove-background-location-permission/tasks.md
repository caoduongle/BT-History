# Tasks: Remove Redundant Background Location Permission

**Branch**: `004-remove-background-location-permission`  
**Input**: Design artifacts from `specs/004-remove-background-location-permission/`  

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Baseline verification

- [X] T001 Verify baseline test execution environment with ./gradlew.bat testDebugUnitTest in root

---

## Phase 2: User Story 1 - Frictionless Onboarding & Play Store Compliance (Priority: P1) 🎯 MVP

**Goal**: Remove `ACCESS_BACKGROUND_LOCATION` from `AndroidManifest.xml` and confirm onboarding UI only requests foreground location.

**Independent Test**: Inspect `app/src/main/AndroidManifest.xml` to verify `ACCESS_BACKGROUND_LOCATION` is completely removed.

### Implementation for User Story 1

- [X] T002 [US1] Remove ACCESS_BACKGROUND_LOCATION from app/src/main/AndroidManifest.xml and add architectural rationale comments
- [X] T003 [US1] Audit PermissionOnboardingScreen.kt and SettingsScreen.kt to ensure only foreground location permissions are requested

**Checkpoint**: User Story 1 complete - Manifest is compliant and onboarding remains frictionless.

---

## Phase 3: User Story 2 - Architectural Transparency & Policy Documentation (Priority: P2)

**Goal**: Add detailed documentation in `README.md` explaining why `ACCESS_BACKGROUND_LOCATION` is not needed and how Google Play Location Policy is fulfilled.

**Independent Test**: Review `README.md` to confirm the location permission architecture section.

### Implementation for User Story 2

- [X] T004 [US2] Add Location Architecture & Google Play Policy Compliance section to README.md

**Checkpoint**: User Story 2 complete - Architectural choices and policy rationale are thoroughly documented.

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Regression testing and artifact validation

- [X] T005 Run static manifest audit and full test suite via ./gradlew.bat testDebugUnitTest in app/
- [X] T006 [P] Update feature documentation and checklists in specs/004-remove-background-location-permission/

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Completed.
- **User Story 1 (Phase 2)**: Completed. Manifest cleaned up and UI audited (`T002`, `T003`).
- **User Story 2 (Phase 3)**: Completed. Comprehensive documentation added to `README.md` (`T004`).
- **Polish (Phase 4)**: Completed. Static manifest check and full unit test suite passed (`T005`, `T006`).

### Implementation Strategy Verification
- **MVP**: Completed with User Story 1 (`T002`, `T003`).
- **All 6 Tasks Completed**: 100% finished and verified.
