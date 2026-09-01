# Tasks: Safe Room Schema Export & Migration Framework

**Branch**: `007-room-schema-migrations`  
**Input**: Design artifacts from `specs/007-room-schema-migrations/`  

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Baseline verification

- [x] T001 Verify baseline build status with ./gradlew.bat testDebugUnitTest in root

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Configure Gradle KSP to export Room schemas to version control

- [x] T002 Configure Room KSP compiler argument room.schemaLocation and test sourceSets in app/build.gradle.kts

**Checkpoint**: Foundation ready - KSP is configured to export schemas upon database compilation.

---

## Phase 3: User Story 1 - Safe Migration Framework & Elimination of Destructive Fallback (Priority: P1) 🎯 MVP

**Goal**: Protect historical Bluetooth connection records and GPS locations by completely removing destructive migration fallback and wiring an extensible migration array.

**Independent Test**: Verify `fallbackToDestructiveMigration()` is absent from `AppDatabase.kt`, the database builder registers `.addMigrations(*ALL_MIGRATIONS)`, and unit tests confirm database creation.

### Implementation for User Story 1

- [x] T003 [US1] Remove fallbackToDestructiveMigration() and define ALL_MIGRATIONS array in app/src/main/java/com/example/data/AppDatabase.kt
- [x] T004 [US1] Register .addMigrations(*ALL_MIGRATIONS) in databaseBuilder in app/src/main/java/com/example/data/AppDatabase.kt
- [x] T005 [P] [US1] Create unit test in app/src/test/java/com/example/DatabaseMigrationFrameworkTest.kt verifying database initialization without destructive fallback

**Checkpoint**: User Story 1 complete - user data can never be wiped by destructive fallback.

---

## Phase 4: User Story 2 - Room Schema Export & Baseline Version 1 (Priority: P2)

**Goal**: Enable Room schema export to generate and version-control the canonical schema JSON baseline for version 1.

**Independent Test**: Build the project and confirm `app/schemas/com.example.data.AppDatabase/1.json` is generated with valid definitions for `devices` and `events`.

### Implementation for User Story 2

- [x] T006 [US2] Set exportSchema = true in @Database annotation in app/src/main/java/com/example/data/AppDatabase.kt
- [x] T007 [US2] Run ./gradlew.bat compileDebugKotlin to generate canonical schema JSON at app/schemas/com.example.data.AppDatabase/1.json
- [x] T008 [P] [US2] Verify 1.json schema content accurately reflects devices and events tables, indices, and foreign keys

**Checkpoint**: User Story 2 complete - schema baseline v1 is version-controlled in Git.

---

## Phase 5: User Story 3 - Architectural Governance & Migration Contract (Priority: P3)

**Goal**: Document strict migration requirements in code comments so future developers never bump version without an explicit Migration.

**Independent Test**: Inspect `AppDatabase.kt` and confirm prominent architectural guidelines exist.

### Implementation for User Story 3

- [x] T009 [US3] Add prominent architectural KDoc and migration contract comments in app/src/main/java/com/example/data/AppDatabase.kt

**Checkpoint**: User Story 3 complete - codebase governance is codified.

---

## Phase 6: Polish & Build Verification

**Purpose**: Full regression testing and APK verification

- [x] T010 Run ./gradlew.bat testDebugUnitTest and ./gradlew.bat assembleDebug to verify 100% test pass rate and clean APK packaging
- [x] T011 [P] Update feature documentation and checklists in specs/007-room-schema-migrations/

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Baseline verification (`T001`).
- **Foundational (Phase 2)**: KSP schema location configuration (`T002`). Blocks compilation tasks.
- **User Story 1 (Phase 3)**: Remove destructive fallback and wire migration registry (`T003`, `T004`, `T005`).
- **User Story 2 (Phase 4)**: Enable schema export and generate 1.json baseline (`T006`, `T007`, `T008`).
- **User Story 3 (Phase 5)**: Codify developer governance in code comments (`T009`).
- **Polish (Phase 6)**: Regression test suite and build verification (`T010`, `T011`).

---

## Implementation Strategy

### MVP First (Phases 1-3)
1. Configure KSP argument.
2. Remove destructive migration from `AppDatabase.kt` and wire `ALL_MIGRATIONS`.
3. Verify with unit tests.

### Incremental Delivery
1. Foundational KSP setup ready.
2. User Story 1 protects user data immediately.
3. User Story 2 exports `1.json` for future migration tests.
4. User Story 3 ensures future development adherence.
5. All existing Robolectric & unit tests pass without regressions.
