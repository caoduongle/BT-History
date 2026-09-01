# Implementation Plan: Deduplicate Bluetooth Receiver

**Branch**: `001-deduplicate-bluetooth-receiver` | **Date**: 2026-09-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/001-deduplicate-bluetooth-receiver/spec.md`

## Summary

In the BT-History Android application, `BluetoothEventReceiver` was concurrently declared statically in `app/src/main/AndroidManifest.xml` and registered dynamically inside `BluetoothWatcherService.registerBluetoothReceiver()`. This dual registration caused every system Bluetooth broadcast to be delivered twice, resulting in duplicate database event entries, redundant GPS location queries, and duplicate disconnect alert notifications.

The technical solution consists of:
1. Removing the static `<receiver>` declaration from `AndroidManifest.xml` to eliminate redundant broadcast dispatch.
2. Relying entirely on dynamic registration in `BluetoothWatcherService`, pairing registration in `onCreate()` and unregistration in `onDestroy()`.
3. Adding explicit code comments detailing why dynamic registration is required (Android 8.0+ / API 26+ implicit broadcast background execution restrictions).
4. Creating an automated Robolectric test verifying that an `ACTION_ACL_CONNECTED` broadcast produces exactly 1 `EventEntity` record in the database.

## Technical Context

**Language/Version**: Kotlin 2.2.10, Java 11 / Android SDK (compileSdk 36, minSdk 24, targetSdk 36)

**Primary Dependencies**: Android Jetpack (Room 2.7.0, Coroutines 1.10.2, Core-KTX 1.18.0, Lifecycle 2.8.7, ServiceCompat), Google Play Services Location 21.3.0

**Storage**: SQLite via Android Jetpack Room (`AppDatabase`, `EventEntity`, `DeviceEntity`, `EventDao`, `DeviceDao`)

**Testing**: JUnit 4 (4.13.2), Robolectric (4.16.1), `kotlinx-coroutines-test` (1.10.2), AndroidX Test Core/JUnit

**Target Platform**: Android 7.0 (API 24) through Android 15 (API 35/36)

**Project Type**: Native Android Mobile Application

**Performance Goals**: Instantaneous broadcast processing (<50ms before background delegation), 0 duplicate database records, 0 duplicate notifications, 0 location fetch redundancy

**Constraints**: Android 8.0+ (API 26) ban on manifest-declared implicit broadcast receivers; Android 14+ (API 34) mandatory `Context.RECEIVER_EXPORTED` for system-wide broadcasts

**Scale/Scope**: Modify `AndroidManifest.xml` (remove static receiver), modify `BluetoothWatcherService.kt` (code comments + verify lifecycle), add new Robolectric test `BluetoothDeduplicationTest.kt`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Test-First / Verification**: Verified. Robolectric automated unit test is planned to enforce the single-event invariant before feature sign-off.
- **Simplicity / YAGNI**: Verified. Direct removal of redundant static manifest declaration addresses the root cause without introducing complex debounce logic or unnecessary cache layers.
- **Observability**: Verified. Structured lifecycle logs and foreground service notifications clearly communicate active state and device count.
- **No Gate Violations**: All criteria satisfied.

## Project Structure

### Documentation (this feature)

```text
specs/001-deduplicate-bluetooth-receiver/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan (this file)
├── research.md          # Phase 0 research & architectural decisions
├── data-model.md        # Phase 1 data model & schema invariants
├── quickstart.md        # Phase 1 validation guide
├── contracts/           # Phase 1 component contracts
│   └── bluetooth-receiver-contract.md
└── checklists/
    └── requirements.md  # Specification quality checklist
```

### Source Code (repository root)

```text
app/
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml                                  # [MODIFY] Remove static <receiver>
│   │   └── java/com/example/
│   │       ├── receiver/
│   │       │   └── BluetoothEventReceiver.kt                    # [REFERENCE] Target broadcast receiver
│   │       ├── service/
│   │       │   └── BluetoothWatcherService.kt                   # [MODIFY] Document dynamic vs static rationale
│   │       └── data/
│   │           ├── AppDatabase.kt                               # [REFERENCE] Room database
│   │           ├── dao/EventDao.kt                              # [REFERENCE] Event DAO for assertions
│   │           └── entity/EventEntity.kt                        # [REFERENCE] Event entity
│   └── test/
│       └── java/com/example/
│           ├── BluetoothDeduplicationTest.kt                    # [NEW] Robolectric single-event test
│           └── ExampleRobolectricTest.kt                        # [REFERENCE] Existing Robolectric sample
└── build.gradle.kts                                             # [REFERENCE] Build configuration
```

**Structure Decision**: Standard Android single-module (`app/`) architecture. Changes are isolated to manifest, service documentation, and unit test suite.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
| :--- | :--- | :--- |
| None | N/A | Direct fix; zero unnecessary complexity introduced |
