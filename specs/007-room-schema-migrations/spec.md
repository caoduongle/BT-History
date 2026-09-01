# Feature Specification: Safe Room Schema Export & Migration Framework

**Feature Branch**: `007-room-schema-migrations`

**Created**: 2026-09-02

**Status**: Draft

**Input**: User description: "AppDatabase hiện dùng fallbackToDestructiveMigration(), nghĩa là mọi thay đổi schema tương lai sẽ xoá sạch dữ liệu lịch sử Bluetooth + vị trí của người dùng. Hãy: 1. Bật exportSchema = true và cấu hình Room lưu file schema JSON để làm cơ sở viết migration sau này. 2. Thay fallbackToDestructiveMigration() bằng cơ chế Migration thật (dù hiện tại version = 1 chưa có migration nào cần viết ngay, hãy chuẩn bị khung sẵn sàng để version 2 trở đi bắt buộc phải viết Migration thay vì fallback phá huỷ dữ liệu). 3. Ghi chú rõ trong code: mọi thay đổi entity từ nay về sau PHẢI đi kèm một Migration tương ứng, không được tăng version mà không có migration."

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Guard Historical Bluetooth & Location Data from Destruction (Priority: P1)

As an active user of BT Watcher who relies on historical Bluetooth connection/disconnection timelines and recorded GPS locations to track belongings, I want the application database to never wipe out my historical data during app updates or schema changes, so that my past location tracking history is permanently preserved.

**Why this priority**: Using `fallbackToDestructiveMigration()` is an anti-pattern for personal history and telemetry apps. Any modification to entity definitions (e.g., adding an event metadata field or index) in future updates would silently wipe the user's SQLite database on startup if a destructive fallback is enabled. Disabling destructive fallback ensures data durability.

**Independent Test**: Verify that `AppDatabase.kt` does not call `fallbackToDestructiveMigration()`, that database builds succeed with `addMigrations(...)`, and that tests verify database initialization and operations without destructive fallback.

**Acceptance Scenarios**:

1. **Given** `AppDatabase.kt`, **When** inspected, **Then** `fallbackToDestructiveMigration()` is completely removed from the database builder configuration.
2. **Given** the database builder in `AppDatabase.Companion.getDatabase()`, **When** instantiating the database, **Then** it registers a structured migration registry (`ALL_MIGRATIONS` array passed to `.addMigrations(*ALL_MIGRATIONS)`) ready to host future incremental migrations.
3. **Given** existing device records and event entities in SQLite, **When** the application restarts or updates, **Then** data is preserved intact without being dropped.

---

### User Story 2 - Enable Room Schema Export & Version-Controlled Schema Baseline (Priority: P2)

As an Android engineer developing future database iterations for BT Watcher, I want Room to automatically export the database schema JSON on build (`exportSchema = true`), so that the exact database schema for version 1 is version-controlled in the repository and serves as the baseline for writing future automated migrations and Room migration verification tests.

**Why this priority**: Incremental migrations in Room require a verified baseline schema JSON. Without exported schema JSON files, automated migration testing (`MigrationTestHelper`) and schema comparison across versions are impossible.

**Independent Test**: Run `./gradlew testDebugUnitTest` or `./gradlew assembleDebug` and verify that Room generates `app/schemas/com.example.data.AppDatabase/1.json` containing the exact table definitions for `devices` and `events`.

**Acceptance Scenarios**:

1. **Given** `@Database` annotation in `AppDatabase.kt`, **When** inspected, **Then** `exportSchema` is set to `true`.
2. **Given** `app/build.gradle.kts`, **When** KSP executes, **Then** the `room.schemaLocation` compiler argument points to a version-controlled directory (`$projectDir/schemas`).
3. **Given** a build execution (`./gradlew testDebugUnitTest`), **When** compilation completes, **Then** the valid schema JSON for version 1 is generated under `app/schemas/com.example.data.AppDatabase/1.json`.

---

### User Story 3 - Architectural Governance & Migration Contract (Priority: P3)

As a contributor or maintainer of the BT-History codebase, I want clear, explicit architectural guidelines documented in the database source code and project documentation, stating that any future change to entity schemas MUST be accompanied by an explicit `Migration(from, to)` definition and that incrementing database version without migration is strictly prohibited.

**Why this priority**: When destructive migration is removed, bumping the database version without providing a migration path causes an `IllegalStateException` on startup. Clear code contracts ensure all future developers follow safe migration practices.

**Independent Test**: Review `AppDatabase.kt` and ensure explicit KDoc/comments define the migration governance policy and explain how to add new migrations.

**Acceptance Scenarios**:

1. **Given** `AppDatabase.kt`, **When** reviewed by a developer, **Then** a prominent architectural comment document specifies:
   - Destructive migration is strictly forbidden to protect user data.
   - Any modification to entity fields or indexes requires incrementing `version` AND registering an explicit `Migration(from, to)` in `ALL_MIGRATIONS`.
   - Schema JSON updates must be committed to git alongside code changes.

---

### Edge Cases

- **Existing Version 1 Databases**: For all existing users on database version 1, removing `fallbackToDestructiveMigration()` has zero impact on runtime behavior because the database version remains 1 and no schema migration is executed.
- **Unregistered Version Bump**: If a developer increments `version = 2` without providing a `Migration(1, 2)`, Room will throw an `IllegalStateException` during testing, immediately catching the oversight before reaching production.
- **CI / Build Schema Directory**: The schema directory (`app/schemas`) must be tracked in git so that fresh checkouts or CI environments can validate schemas without build-order dependencies.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST set `exportSchema = true` in the `@Database` annotation of `AppDatabase.kt`.
- **FR-002**: The build configuration (`app/build.gradle.kts`) MUST configure the KSP compiler argument `room.schemaLocation` pointing to the `app/schemas` directory.
- **FR-003**: The system MUST remove `fallbackToDestructiveMigration()` from `AppDatabase.Companion.getDatabase()`.
- **FR-004**: The system MUST introduce an extensible migration registry (e.g. `ALL_MIGRATIONS: Array<Migration>`) and register it with `Room.databaseBuilder(...).addMigrations(*ALL_MIGRATIONS)`.
- **FR-005**: The system MUST compile the project and generate the version 1 baseline schema file `app/schemas/com.example.data.AppDatabase/1.json`.
- **FR-006**: The system MUST document the database migration contract in `AppDatabase.kt`, mandating that all future schema alterations accompany a corresponding `Migration` implementation.
- **FR-007**: All existing unit tests and Robolectric integration tests MUST continue to pass with 100% success.

---

### Key Entities *(if data involved)*

- **`DeviceEntity`**: Represents paired/discovered Bluetooth devices (table `devices`). Schema v1 definition in Room.
- **`EventEntity`**: Represents connection/disconnection events with timestamps and GPS coordinates (table `events`). Schema v1 definition in Room.
- **Schema JSON Artifact**: `app/schemas/com.example.data.AppDatabase/1.json`, documenting SQLite tables, columns, primary keys, indices, and foreign keys for version 1.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **Zero Data Loss Vulnerability**: Destructive migration is eliminated from database initialization code.
- **100% Schema Export Coverage**: Running `./gradlew.bat testDebugUnitTest` generates a valid, syntactically correct `1.json` schema artifact under `app/schemas/com.example.data.AppDatabase/`.
- **100% Test Suite Pass Rate**: All unit tests (`./gradlew.bat testDebugUnitTest`) pass without regressions.
- **Clean Compilation**: `./gradlew.bat assembleDebug` builds successfully with zero schema-related warnings or errors.

---

## Assumptions & Dependencies

- **Database Version**: The current database version is 1; no backward migrations are required right now since version 1 is the initial public schema.
- **Room Library**: Uses `androidx.room:room-runtime`, `androidx.room:room-ktx`, and `androidx.room:room-compiler` already installed via KSP in `libs.versions.toml`.
- **Version Control**: `app/schemas/` will be committed to Git as the authoritative source of truth for Room schema verification.
