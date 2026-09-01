# Tasks: Safe Permission Onboarding & Android 14+ Foreground Service Crash Prevention

**Branch**: `008-fix-onboarding-permission-crash`  
**Input**: Design artifacts from `specs/008-fix-onboarding-permission-crash/`  

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify baseline build and test execution environment before making code changes

- [x] T001 Verify baseline unit test status with `./gradlew.bat testDebugUnitTest` in root

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core permission logic that MUST be complete before ANY user story or service code can execute

**⚠️ CRITICAL**: All subsequent user stories depend on this centralized gatekeeper

- [x] T002 Implement composite permission gatekeeper `BluetoothHelper.hasRequiredPermissionsForService(context: Context): Boolean` in `app/src/main/java/com/example/util/BluetoothHelper.kt`

**Checkpoint**: Foundation ready - all components can now deterministically evaluate if the service can legally execute under Android 14+ foreground service constraints.

---

## Phase 3: User Story 1 - Decouple Onboarding Skip from Permission-Grant Flow (Priority: P1) 🎯 MVP

**Goal**: Ensure clicking "Bỏ qua / Vào giao diện chính" routes the user directly to the device list without initiating background Bluetooth/location tracking services when permissions are ungranted.

**Independent Test**: Launch the onboarding screen with permissions ungranted, click "Bỏ qua", and verify navigation completes to `device_list`, `isOnboardingCompleted == true`, `isServiceEnabled == false`, and zero service start intents are dispatched.

### Implementation for User Story 1

- [x] T003 [US1] Add distinct `onSkip: () -> Unit` parameter to `PermissionOnboardingScreen` and bind `"skip_onboarding_button"` in `app/src/main/java/com/example/ui/screens/PermissionOnboardingScreen.kt`
- [x] T004 [P] [US1] Implement `skipOnboarding()` in `app/src/main/java/com/example/ui/viewmodel/DeviceViewModel.kt` to mark onboarding completed and set `isServiceEnabled = false` when permissions are missing
- [x] T005 [US1] Wire `onSkip` callback to invoke `viewModel.skipOnboarding()` and navigate to `device_list` in `app/src/main/java/com/example/MainActivity.kt`

**Checkpoint**: User Story 1 complete - user onboarding skip path is completely decoupled from service activation, eliminating the immediate UI crash upon skipping.

---

## Phase 4: User Story 2 - Comprehensive Foreground Service Permission Gatekeeper (Priority: P1) 🎯 MVP

**Goal**: Prevent `BluetoothWatcherService` from ever calling `startForeground` without required permissions on Android 14+ (API 34+), shut down safely without throwing `SecurityException`, and eliminate boot-time crash loops.

**Independent Test**: Call `BluetoothWatcherService.startService(context)` and trigger `onStartCommand()` when permissions are revoked; verify service stops itself safely (`stopSelf()`), removes foreground notification, resets `isServiceEnabled` to `false`, and throws zero exceptions.

### Implementation for User Story 2

- [x] T006 [US2] Guard `BluetoothWatcherService.startService(context)` to abort before dispatching start intent if `hasRequiredPermissionsForService` returns false in `app/src/main/java/com/example/service/BluetoothWatcherService.kt`
- [x] T007 [US2] Guard `BluetoothWatcherService.onStartCommand()` to call `stopForeground(STOP_FOREGROUND_REMOVE)`, `stopSelf()`, and update `preferencesRepository.setServiceEnabled(false)` if permissions are missing in `app/src/main/java/com/example/service/BluetoothWatcherService.kt`
- [x] T008 [P] [US2] Guard `BootReceiver.onReceive()` to check `hasRequiredPermissionsForService(context)` before starting service and reset preference if missing in `app/src/main/java/com/example/receiver/BootReceiver.kt`

**Checkpoint**: User Story 2 complete - service startup is defensively guarded at all entry points against Android 14 `SecurityException` and reboot crash-loops.

---

## Phase 5: User Story 3 - In-App Visibility & Recovery for Disabled Service (Priority: P2)

**Goal**: Provide clear visual feedback and a 1-tap recovery shortcut on `DeviceListScreen` and `SettingsScreen` when the background monitoring service is inactive due to missing permissions.

**Independent Test**: Skip onboarding, view `DeviceListScreen`, verify the warning banner appears indicating background tracking is paused due to permissions, and tap the action button to launch the permission request flow.

### Implementation for User Story 3

- [x] T009 [US3] Add Bento-styled permission alert banner and grant shortcut to `app/src/main/java/com/example/ui/screens/DeviceListScreen.kt`
- [x] T010 [P] [US3] Add permission status indicator and permission explanation under the Foreground Service switch in `app/src/main/java/com/example/ui/screens/SettingsScreen.kt`

**Checkpoint**: User Story 3 complete - users have clear visibility into service status and a frictionless recovery path to enable tracking when desired.

---

## Phase 6: User Story 4 - Automated Robolectric Regression Suite on API 34 (Priority: P1)

**Goal**: Create automated Robolectric unit tests running against Android 14 (API 34) verifying that skipping onboarding never crashes and service halts cleanly without calling `startForeground`.

**Independent Test**: Run `./gradlew.bat testDebugUnitTest` and verify `OnboardingPermissionSkipTest` passes with 100% success.

### Implementation for User Story 4

- [x] T011 [US4] Create Robolectric test `OnboardingPermissionSkipTest.kt` targeting SDK 34 verifying Skip flow, missing-permission service exit, and preference consistency in `app/src/test/java/com/example/OnboardingPermissionSkipTest.kt`

**Checkpoint**: User Story 4 complete - automated regression test confirms crash fix under Android 14.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Full regression testing and documentation validation

- [x] T012 Run full unit test suite with `./gradlew.bat testDebugUnitTest` across all SDK configurations
- [x] T013 [P] Update feature documentation and checklists in `specs/008-fix-onboarding-permission-crash/`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately (`T001`).
- **Foundational (Phase 2)**: Depends on Setup (`T001`) - **BLOCKS** all user stories (`T002`).
- **User Story 1 (Phase 3)**: Depends on Foundational (`T002`). Decouples UI Skip flow (`T003`, `T004`, `T005`).
- **User Story 2 (Phase 4)**: Depends on Foundational (`T002`). Guards service and boot receiver (`T006`, `T007`, `T008`).
- **User Story 3 (Phase 5)**: Depends on User Story 1 (`T003`-`T005`). Adds UI banners and status (`T009`, `T010`).
- **User Story 4 (Phase 6)**: Depends on User Story 1 (`T003`-`T005`) and User Story 2 (`T006`-`T008`). Robolectric test verification (`T011`).
- **Polish (Phase 7)**: Depends on all user stories being complete (`T012`, `T013`).

### Parallel Opportunities

- Within Phase 3 (US1): `T004` (`DeviceViewModel.kt`) can be developed in parallel with `T003` (`PermissionOnboardingScreen.kt`).
- Within Phase 4 (US2): `T008` (`BootReceiver.kt`) can be developed in parallel with `T006` and `T007` (`BluetoothWatcherService.kt`).
- Within Phase 5 (US3): `T010` (`SettingsScreen.kt`) can be developed in parallel with `T009` (`DeviceListScreen.kt`).
- Within Phase 7: `T013` (Docs) can be prepared in parallel with `T012` (Build validation).

---

## Parallel Example: User Story 1 & User Story 2

```bash
# Developer A working on UI decoupling (User Story 1):
Task: "Add distinct onSkip: () -> Unit parameter to PermissionOnboardingScreen in app/src/main/java/com/example/ui/screens/PermissionOnboardingScreen.kt"
Task: "Implement skipOnboarding() in app/src/main/java/com/example/ui/viewmodel/DeviceViewModel.kt"

# Developer B working on Service Defense (User Story 2):
Task: "Guard BluetoothWatcherService.startService(context) in app/src/main/java/com/example/service/BluetoothWatcherService.kt"
Task: "Guard BootReceiver.onReceive() in app/src/main/java/com/example/receiver/BootReceiver.kt"
```

---

## Implementation Strategy

### MVP First (Phases 1-4: US1 & US2)
1. Complete Setup (`T001`) and Foundational Permission Gatekeeper (`T002`).
2. Implement User Story 1: Decouple `PermissionOnboardingScreen` and add `DeviceViewModel.skipOnboarding()` (`T003`, `T004`, `T005`).
3. Implement User Story 2: Guard `BluetoothWatcherService` and `BootReceiver` against unauthorized execution (`T006`, `T007`, `T008`).
4. **Validation Checkpoint**: User can click "Bỏ qua" on onboarding without triggering service start or throwing `SecurityException`.

### Incremental Delivery (Phases 5-7: US3, US4 & Polish)
1. Add User Story 3: In-App UI alerts and Settings recovery shortcuts (`T009`, `T010`).
2. Add User Story 4: Robolectric test suite on API 34 verifying the fix (`T011`).
3. Run full test suite regression and finalize documentation (`T012`, `T013`).
