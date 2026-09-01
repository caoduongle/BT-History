# Implementation Plan: Transactional Device Event Upsert

**Branch**: `003-transactional-device-upsert` | **Date**: 2026-09-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/003-transactional-device-upsert/spec.md`

## Summary

`DeviceRepository.recordBluetoothEvent()` previously performed an un-transactional read-then-write cycle: querying `getDeviceByMac`, conditionally inserting or updating the device, and inserting an event. Under concurrent background execution (e.g. bursty profile events), parallel coroutines racing on the same MAC address could both find no existing device, causing concurrent inserts, duplicate replacement via `OnConflictStrategy.REPLACE` (which wipes out foreign keys), or race condition collisions.

The implementation plan:
1. Updates `DeviceDao` to use `OnConflictStrategy.IGNORE` on insert instead of `REPLACE`, preventing destructive row deletion and foreign key cascade issues.
2. Injects `database: AppDatabase` into `DeviceRepository` and wraps `recordBluetoothEvent` in `database.withTransaction { ... }`, ensuring atomic serialization of read-then-write logic.
3. Implements a safe upsert fallback: if an insert returns `-1L` due to a concurrent unique index conflict on `mac_address`, re-query and update the existing device cleanly.
4. Updates `BtWatcherApplication` to provide `database` to `DeviceRepository`.
5. Creates a dedicated Robolectric concurrency test suite in `ConcurrentEventRecordingTest.kt` verifying that 10 concurrent coroutines targeting the same MAC address create exactly 1 `DeviceEntity`.

## Technical Context

**Language/Version**: Kotlin 2.2.10, Java 11 / Android SDK (compileSdk 36, minSdk 24, targetSdk 36)

**Primary Dependencies**: Android Jetpack (Room 2.7.0, Coroutines 1.10.2, Core-KTX 1.18.0)

**Storage**: SQLite via Room (`DeviceEntity`, `EventEntity`, `AppDatabase`)

**Testing**: JUnit 4 (4.13.2), Robolectric (4.16.1), `kotlinx-coroutines-test` (1.10.2)

**Target Platform**: Android 7.0 (API 24) through Android 15 (API 35/36)

**Performance Goals**: Zero SQLite deadlocks, serialized atomic writes completing within a few milliseconds.

**Constraints**: Backwards compatible Room schema; no table restructuring or migration required.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Test-First / Verification**: Verified. Concurrency test suite `ConcurrentEventRecordingTest.kt` exercises simultaneous writes on `Dispatchers.IO`.
- **Simplicity / YAGNI**: Verified. Uses Room's standard `withTransaction` and `OnConflictStrategy.IGNORE` without adding external synchronization locks.
- **Observability**: Clear return contract `Pair(deviceId, eventId)`.
- **No Gate Violations**: All criteria satisfied.

## Project Structure

### Documentation (this feature)

```text
specs/003-transactional-device-upsert/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan (this file)
├── research.md          # Phase 0 research & architectural decisions
├── data-model.md        # Phase 1 data model & sequence diagram
├── quickstart.md        # Phase 1 validation guide
├── contracts/           # Phase 1 component contracts
│   └── transactional-repository-contract.md
└── checklists/
    └── requirements.md  # Specification quality checklist
```

### Source Code (repository root)

```text
app/
├── src/
│   ├── main/
│   │   └── java/com/example/
│   │       ├── BtWatcherApplication.kt                          # [MODIFY] Pass database to DeviceRepository
│   │       ├── data/
│   │       │   ├── dao/
│   │       │   │   └── DeviceDao.kt                             # [MODIFY] Replace REPLACE with IGNORE
│   │       │   └── repository/
│   │       │       └── DeviceRepository.kt                      # [MODIFY] Wrap in withTransaction + safe upsert
│   └── test/
│       └── java/com/example/
│           ├── ConcurrentEventRecordingTest.kt                  # [NEW] Concurrency test suite
│           ├── DisconnectClassificationTest.kt                  # [REFERENCE] Existing test suite
│           └── BluetoothDeduplicationTest.kt                    # [REFERENCE] Existing test suite
└── build.gradle.kts
```

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
| :--- | :--- | :--- |
| None | N/A | Room `withTransaction` is the standard, built-in solution for multi-DAO atomic operations |
