# Implementation Plan: Safe Room Schema Export & Migration Framework

**Branch**: `007-room-schema-migrations` | **Date**: 2026-09-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/007-room-schema-migrations/spec.md`

## Summary

This plan transitions `AppDatabase` from dangerous destructive migration fallbacks to a resilient, production-ready schema evolution architecture. We will:
1. Enable `exportSchema = true` in `AppDatabase.kt` and configure Room's KSP processor in `app/build.gradle.kts` to output database schema definitions to `app/schemas/`.
2. Remove `fallbackToDestructiveMigration()`, eliminating the risk of accidental user data wipes.
3. Introduce an extensible migration registry (`ALL_MIGRATIONS` array) and bind it via `Room.databaseBuilder(...).addMigrations(*ALL_MIGRATIONS)`.
4. Add clear architectural documentation and code guidelines mandating that all future entity changes include explicit `Migration` implementations.
5. Generate and version-control `app/schemas/com.example.data.AppDatabase/1.json` in Git.

---

## Technical Context

**Language/Version**: Kotlin 2.1+, Java 17  
**Primary Dependencies**: AndroidX Room (Runtime, KTX, KSP Compiler), Room Migration / SQLite  
**Storage**: SQLite via Room Database (`bt_watcher_database`)  
**Testing**: JUnit 4, Robolectric 4.14+, Coroutines Test  
**Target Platform**: Android (minSdk 24, targetSdk 36)  
**Project Type**: Native Android Mobile Application  
**Performance Goals**: Zero overhead during normal operations; atomic, non-blocking startup  
**Constraints**: Zero data loss; 100% test compatibility; clean Git-tracked schema artifacts  
**Scale/Scope**: 1 database (`AppDatabase`), 2 entities (`DeviceEntity`, `EventEntity`), 1 build script (`app/build.gradle.kts`)  

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Data Durability**: PASS. Eliminates `fallbackToDestructiveMigration()`, protecting all user historical locations and device logs.
- **Traceability & Version Control**: PASS. Schema JSON baseline is preserved under `app/schemas/`.
- **Test Integrity**: PASS. All unit and Robolectric tests must pass with 100% success without regression.

---

## Project Structure

### Documentation (this feature)

```text
specs/007-room-schema-migrations/
├── spec.md                              # Feature specification
├── plan.md                              # Implementation plan (this file)
├── research.md                          # Phase 0 research & architectural decisions
├── data-model.md                        # Phase 1 data model & schema v1 details
├── quickstart.md                        # Phase 1 validation guide
├── contracts/
│   └── database-migration-contract.md   # Developer migration contract
└── checklists/
    └── requirements.md                  # Quality checklist
```

### Source Code Changes

```text
app/
├── build.gradle.kts                     # Configure KSP room.schemaLocation and test sourceSets
├── schemas/
│   └── com.example.data.AppDatabase/
│       └── 1.json                       # Generated Room schema baseline JSON
└── src/
    ├── main/java/com/example/data/
    │   └── AppDatabase.kt               # Enable exportSchema, remove fallbackToDestructiveMigration, register ALL_MIGRATIONS
    └── test/java/com/example/
        └── DatabaseMigrationFrameworkTest.kt # Unit tests verifying non-destructive DB configuration
```

---

## Implementation Phases

### Phase 1: Build & KSP Configuration
- In `app/build.gradle.kts`, configure `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`.
- In `android.sourceSets`, register `$projectDir/schemas` for test assets.

### Phase 2: Database Code Refactoring
- In `AppDatabase.kt`:
  - Change `@Database(..., exportSchema = true)`.
  - Remove `.fallbackToDestructiveMigration()`.
  - Define `val ALL_MIGRATIONS: Array<Migration> = arrayOf(...)` with detailed architectural instructions.
  - Call `.addMigrations(*ALL_MIGRATIONS)` in database builder.

### Phase 3: Schema Generation & Testing
- Run `./gradlew.bat testDebugUnitTest` to trigger KSP schema export.
- Verify generation of `app/schemas/com.example.data.AppDatabase/1.json`.
- Add unit test verifying that database builds without destructive fallback.
- Run `./gradlew.bat assembleDebug` to verify APK build.
