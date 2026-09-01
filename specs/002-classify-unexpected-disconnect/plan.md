# Implementation Plan: Classify Unexpected Disconnect

**Branch**: `002-classify-unexpected-disconnect` | **Date**: 2026-09-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/002-classify-unexpected-disconnect/spec.md`

## Summary

In `BluetoothEventReceiver.handleBluetoothAction()`, `isUnexpectedDisconnect` was previously hard-coded to `true` for all disconnection events (`ACTION_ACL_DISCONNECTED` and profile `STATE_DISCONNECTED`). This caused false alarms whenever the user turned off phone Bluetooth manually or entered Airplane Mode.

The technical implementation introduces:
1. Interception of `BluetoothAdapter.ACTION_STATE_CHANGED` in `BluetoothEventReceiver`, recording `lastAdapterOffTimestamp` when transitioning to `STATE_TURNING_OFF` or `STATE_OFF`, and resetting when turning back ON.
2. An adjustable heuristic constant `ADAPTER_OFF_HEURISTIC_WINDOW_MS = 4000L` with comprehensive documentation of assumptions, edge cases, and trade-offs.
3. Classification of disconnects within the heuristic window as intentional (`isUnexpectedDisconnect = false`), suppressing disconnect alert notifications while preserving normal history logging.
4. Retention of `isUnexpectedDisconnect = true` (and alert notifications) when disconnections occur without prior adapter turn-off.
5. Unit/Robolectric tests in `DisconnectClassificationTest.kt` verifying both intentional and accidental disconnect flows.

## Technical Context

**Language/Version**: Kotlin 2.2.10, Java 11 / Android SDK (compileSdk 36, minSdk 24, targetSdk 36)

**Primary Dependencies**: Android Jetpack (Room 2.7.0, Coroutines 1.10.2, Core-KTX 1.18.0), Google Play Services Location 21.3.0

**Storage**: SQLite via Room (`EventEntity`, `EventDao`)

**Testing**: JUnit 4 (4.13.2), Robolectric (4.16.1), `kotlinx-coroutines-test` (1.10.2), AndroidX Test Core

**Target Platform**: Android 7.0 (API 24) through Android 15 (API 35/36)

**Project Type**: Native Android Mobile Application

**Performance Goals**: Sub-millisecond evaluation (<1ms) in broadcast receiver using in-memory volatile timestamps; zero disk I/O for classification.

**Constraints**: Must not delay or block broadcast receiver thread; must handle OEM-specific broadcast ordering differences.

**Scale/Scope**: Modify `BluetoothEventReceiver.kt`, add new test suite `DisconnectClassificationTest.kt`.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Test-First / Verification**: Verified. Test suite `DisconnectClassificationTest.kt` covers both positive (intentional) and negative (accidental) disconnect paths.
- **Simplicity / YAGNI**: Verified. Uses a simple, robust in-memory timestamp heuristic without introducing complex state machine libraries or extra persistent tables.
- **Observability**: Verified. Architectural trade-offs and heuristic limitations are documented in code comments and contracts.
- **No Gate Violations**: All criteria satisfied.

## Project Structure

### Documentation (this feature)

```text
specs/002-classify-unexpected-disconnect/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan (this file)
├── research.md          # Phase 0 research & architectural decisions
├── data-model.md        # Phase 1 data model & state transition flow
├── quickstart.md        # Phase 1 validation guide
├── contracts/           # Phase 1 component contracts
│   └── disconnect-classifier-contract.md
└── checklists/
    └── requirements.md  # Specification quality checklist
```

### Source Code (repository root)

```text
app/
├── src/
│   ├── main/
│   │   └── java/com/example/
│   │       ├── receiver/
│   │       │   └── BluetoothEventReceiver.kt                    # [MODIFY] Implement adapter state tracking & heuristic
│   │       └── service/
│   │           └── BluetoothWatcherService.kt                   # [REFERENCE] Already registers ACTION_STATE_CHANGED
│   └── test/
│       └── java/com/example/
│           ├── DisconnectClassificationTest.kt                  # [NEW] Intentional vs accidental disconnect tests
│           └── BluetoothDeduplicationTest.kt                    # [REFERENCE] Existing deduplication test suite
└── build.gradle.kts                                             # [REFERENCE] Build configuration
```

**Structure Decision**: Direct modification to `BluetoothEventReceiver.kt` and addition of unit test `DisconnectClassificationTest.kt`.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
| :--- | :--- | :--- |
| None | N/A | Heuristic based on adapter state is the most lightweight and accurate solution available on standard Android |
