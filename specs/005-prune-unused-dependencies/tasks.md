# Tasks: Prune Unused Template Dependencies

**Branch**: `005-prune-unused-dependencies`  
**Input**: Design artifacts from `specs/005-prune-unused-dependencies/`  

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Baseline verification

- [X] T001 Verify baseline build status with ./gradlew.bat testDebugUnitTest in root

---

## Phase 2: User Story 1 - Lean & Fast Build Configuration (Priority: P1) 🎯 MVP

**Goal**: Remove unused library artifacts, version catalog entries, plugins, and DSL configuration blocks.

**Independent Test**: Review `libs.versions.toml`, `build.gradle.kts`, and `app/build.gradle.kts` to confirm complete removal of dead entries.

### Implementation for User Story 1

- [X] T002 [US1] Prune unused versions, libraries, and plugins from gradle/libs.versions.toml
- [X] T003 [US1] Remove unused plugin aliases (secrets, google-services) from build.gradle.kts
- [X] T004 [US1] Remove unused plugin aliases, DSL configuration blocks (secrets, googleServices), and commented dependencies from app/build.gradle.kts

**Checkpoint**: User Story 1 complete - All unused template dependencies and plugins removed.

---

## Phase 3: User Story 2 - Minimal Annotation Processing & Clean Dependency Graph (Priority: P2)

**Goal**: Eliminate dead KSP annotation processor (`moshi.kotlin.codegen`) to optimize compilation.

**Independent Test**: Verify only Room compiler remains in KSP configuration in `app/build.gradle.kts`.

### Implementation for User Story 2

- [X] T005 [US2] Remove moshi.kotlin.codegen from KSP configurations in app/build.gradle.kts

**Checkpoint**: User Story 2 complete - KSP configuration is minimal and clean.

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Build verification and artifact validation

- [X] T006 Run ./gradlew.bat testDebugUnitTest and ./gradlew.bat assembleDebug to verify clean compilation
- [X] T007 [P] Update feature documentation and checklists in specs/005-prune-unused-dependencies/

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Completed.
- **User Story 1 (Phase 2)**: Completed. Catalog and build scripts pruned (`T002`, `T003`, `T004`).
- **User Story 2 (Phase 3)**: Completed. Moshi codegen removed from KSP (`T005`).
- **Polish (Phase 4)**: Completed. Tests and debug APK compilation validated (`T006`, `T007`).

### Implementation Strategy Verification
- **MVP**: Completed with User Story 1 & 2 (`T002` - `T005`).
- **All 7 Tasks Completed**: 100% finished and verified.
