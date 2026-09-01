# Research: Room Schema Export & Safe Migration Architecture

**Feature**: `007-room-schema-migrations`  
**Date**: 2026-09-02  

## Decision 1: Room Schema Export via KSP Configuration

- **Decision**: Configure KSP in `app/build.gradle.kts` using `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` and include the schema directory in Android source sets for test assets:
  ```kotlin
  ksp {
      arg("room.schemaLocation", "$projectDir/schemas")
  }
  android {
      sourceSets {
          getByName("androidTest").assets.srcDirs("$projectDir/schemas")
          getByName("test").assets.srcDirs("$projectDir/schemas")
      }
  }
  ```
- **Rationale**:
  - `ksp { arg("room.schemaLocation", ...) }` is the official, standard mechanism recommended by Google for KSP-based Room compilation.
  - Specifying `$projectDir/schemas` creates schema files inside `app/schemas/` (e.g. `app/schemas/com.example.data.AppDatabase/1.json`), making them easy to commit and version-control in Git.
  - Adding the schemas directory to test sourceSets allows unit and integration test runners (like Robolectric or Android instrumentation) to access schema JSONs when migration test helpers are used.
- **Alternatives Considered**:
  - *Room Gradle Plugin (`androidx.room` plugin)*: Requires adding an additional plugin classpath in `libs.versions.toml` and `build.gradle.kts`, adding unnecessary build complexity when standard KSP argument already works natively.
  - *Leaving `exportSchema = false`*: Room compiler outputs warnings on build, and no baseline JSON is generated, preventing automated migration verification in future releases.

---

## Decision 2: Elimination of `fallbackToDestructiveMigration()` & Safe Migration Framework

- **Decision**: Remove `.fallbackToDestructiveMigration()` completely from `AppDatabase.Companion.getDatabase()` and replace it with:
  ```kotlin
  /**
   * Registry of all incremental database migrations.
   *
   * CRITICAL DATA SAFETY CONTRACT:
   * 1. Destructive migration (fallbackToDestructiveMigration) is deliberately DISABLED.
   *    Never re-enable it, as doing so will wipe user connection history and GPS records.
   * 2. Any change to entity schemas (DeviceEntity, EventEntity, or new tables) MUST:
   *    - Increment `version` in @Database(version = N, ...)
   *    - Define an explicit `Migration(from = N-1, to = N)`
   *    - Append the migration to `ALL_MIGRATIONS`
   *    - Export and commit the new schema JSON (app/schemas/com.example.data.AppDatabase/N.json)
   */
  val ALL_MIGRATIONS: Array<Migration> = arrayOf(
      // Migrations will be registered here as new schema versions are created:
      // MIGRATION_1_2,
      // MIGRATION_2_3,
  )
  ```
  And in the database builder:
  ```kotlin
  Room.databaseBuilder(
      context.applicationContext,
      AppDatabase::class.java,
      "bt_watcher_database"
  )
      .addMigrations(*ALL_MIGRATIONS)
      .build()
  ```
- **Rationale**:
  - For personal tracking and telemetry applications, database records (past known Bluetooth locations) are the core value to the user. Losing this data upon updating the app destroys user trust.
  - Disabling destructive fallback enforces an absolute compilation and runtime safety net: if a developer increments `version` without defining a migration, Room throws an immediate `IllegalStateException: A migration from X to Y wasn't found` during unit testing, catching the bug before deployment.
- **Alternatives Considered**:
  - *`fallbackToDestructiveMigrationOnDowngrade()`*: Acceptable only for downgrades, but not needed since production apps do not support automated downgrades.
  - *Keep `fallbackToDestructiveMigration()` with comments*: Human errors still happen; developers could accidentally change an entity and wipe databases in production. Explicitly removing the call enforces safety at the compiler/framework level.

---

## Decision 3: Version 1 Baseline Preservation in Git

- **Decision**: Ensure that git tracks `app/schemas/com.example.data.AppDatabase/1.json`.
- **Rationale**:
  - The `.gitignore` file must not exclude `schemas/`. (We previously inspected `.gitignore` and confirmed `schemas/` is NOT ignored).
  - Committing `1.json` serves as the cryptographically verified contract of the database schema at version 1.
- **Alternatives Considered**:
  - *Ignoring schemas in Git*: Defeats the entire purpose of `exportSchema = true` because Room migration tests in CI or on other developer machines would not have access to past schema versions.
