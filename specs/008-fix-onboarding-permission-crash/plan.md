# Implementation Plan: Safe Permission Onboarding & Android 14+ Foreground Service Crash Prevention

**Branch**: `008-fix-onboarding-permission-crash` | **Date**: 2026-09-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/008-fix-onboarding-permission-crash/spec.md`

---

## Summary

This plan resolves the critical crash on Android 14+ (API 34+) devices when users tap the "Bỏ qua / Vào giao diện chính" button during onboarding.
1. **Decouple Onboarding**: Refactor `PermissionOnboardingScreen` to provide distinct `onPermissionsGranted` and `onSkip` callbacks.
2. **Handle Skip State Safely**: Introduce `DeviceViewModel.skipOnboarding()`, which marks onboarding complete, guarantees `isServiceEnabled` is set to `false`, and does not launch `BluetoothWatcherService`.
3. **Permission Gatekeeper**: Implement `BluetoothHelper.hasRequiredPermissionsForService(context)` to verify `BLUETOOTH_CONNECT` and location permissions before service launch.
4. **Service Defense**: In `BluetoothWatcherService.onStartCommand()` and `startService()`, proactively verify permissions; if missing, stop the service cleanly (`stopSelf()`), update `PreferencesRepository.setServiceEnabled(false)`, and avoid illegal `startForeground()` calls.
5. **Boot Safety**: Harden `BootReceiver` so reboots do not initiate `BluetoothWatcherService` if permissions are missing, preventing crash loops.
6. **User Transparency**: Provide clear visual indicators (banner on `DeviceListScreen`, status card in `SettingsScreen`) when the service is inactive due to missing permissions, with a direct action to grant them.
7. **Automated Verification**: Implement Robolectric tests configured for Android 14 (API 34) reproducing the skip scenario and proving that no `SecurityException` is thrown.

---

## Technical Context

**Language/Version**: Kotlin 2.2.10, Java 17  
**Primary Dependencies**: Android Jetpack (Compose, Navigation, DataStore, Core-KTX), Google Play Services Location  
**Testing**: JUnit 4 (4.13.2), Robolectric (4.16.1), Kotlinx Coroutines Test  
**Target Platform**: Android (minSdk 24, compileSdk 36, targetSdk 36)  
**Security & Permissions**: Android 14 (API 34) Foreground Service Type Enforcement (`connectedDevice`, `location`)  
**Project Type**: Native Android Application (Jetpack Compose + Bento UI)  
**Constraints**: 0 crashes on API 34+; 100% test pass rate; no degradation of existing Bluetooth disconnect/connect tracking when permissions are granted.  

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Crash Prevention & Platform Compliance**: PASS. Android 14 foreground service type constraints are respected before calling `startForeground()`.
- **Principle of Least Privilege & User Choice**: PASS. Users who choose to skip permissions are allowed into the application without premature background service initialization.
- **Persistence Integrity**: PASS. `PreferencesRepository.isServiceEnabledFlow` reflects actual operational state, preventing boot-time crash loops.
- **Test Integrity**: PASS. Robolectric unit tests verify safety under API 34.

---

## Project Structure

### Documentation (this feature)

```text
specs/008-fix-onboarding-permission-crash/
├── spec.md                                     # Feature specification
├── plan.md                                     # Implementation plan (this file)
├── research.md                                 # Technical research & Android 14 FGS analysis
├── quickstart.md                               # Validation guide
├── contracts/
│   └── service-permission-contract.md          # Component & service contracts
└── checklists/
    └── requirements.md                         # Quality checklist
```

### Source Code Changes

```text
app/src/
├── main/java/com/example/
│   ├── MainActivity.kt                         # Handle onSkip -> viewModel.skipOnboarding()
│   ├── receiver/
│   │   └── BootReceiver.kt                     # Check hasRequiredPermissionsForService before starting service
│   ├── service/
│   │   └── BluetoothWatcherService.kt          # Guard onStartCommand & startService with permission check
│   ├── ui/
│   │   ├── screens/
│   │   │   ├── DeviceListScreen.kt             # Add banner when service is disabled due to missing permissions
│   │   │   ├── PermissionOnboardingScreen.kt   # Separate onPermissionsGranted & onSkip callbacks
│   │   │   └── SettingsScreen.kt               # Show status & permission shortcut when service disabled
│   │   └── viewmodel/
│   │       └── DeviceViewModel.kt              # Add skipOnboarding() & permission-aware service toggling
│   └── util/
│       └── BluetoothHelper.kt                  # Add hasRequiredPermissionsForService(context)
└── test/java/com/example/
    └── OnboardingPermissionSkipTest.kt         # Robolectric test on API 34 verifying Skip flow & safe service shutdown
```

---

## Implementation Phases

### Phase 1: Core Permission Gatekeeper (`BluetoothHelper`)
- Add `BluetoothHelper.hasRequiredPermissionsForService(context: Context): Boolean`.
- Return `true` if `hasBluetoothConnectPermission(context)` AND `hasLocationPermission(context)` are both `true`.

### Phase 2: Service & Boot Safety Hardening
- In `BluetoothWatcherService.kt`:
  - In `startService(context)`: Check `BluetoothHelper.hasRequiredPermissionsForService(context)`; abort if `false`.
  - In `onStartCommand()`: Check `BluetoothHelper.hasRequiredPermissionsForService(this)`. If `false`, remove any foreground notification, call `stopSelf()`, launch coroutine to set `preferencesRepository.setServiceEnabled(false)`, and return `START_NOT_STICKY`.
- In `BootReceiver.kt`:
  - Check `BluetoothHelper.hasRequiredPermissionsForService(context)` before calling `BluetoothWatcherService.startService()`. If permissions are missing, set `preferencesRepository.setServiceEnabled(false)`.

### Phase 3: Decouple Onboarding Screen & ViewModel
- In `PermissionOnboardingScreen.kt`:
  - Add parameter `onSkip: () -> Unit`.
  - Wire `OutlinedButton` (testTag `"skip_onboarding_button"`) to invoke `onSkip`.
- In `DeviceViewModel.kt`:
  - Add `fun skipOnboarding()`. Marks `isOnboardingCompleted = true` and ensures `isServiceEnabled = false` when permissions are missing.
- In `MainActivity.kt`:
  - Pass `onSkip = { viewModel.skipOnboarding(); navController.navigate("device_list") { popUpTo("onboarding") { inclusive = true } } }`.

### Phase 4: UI Transparency & Shortcut Recovery
- In `DeviceListScreen.kt`:
  - Check if `!isServiceEnabled` due to missing permissions.
  - Display a prominent Bento-styled warning card/banner alerting the user that background tracking is inactive, with a "Cấp quyền" button linking to the permission launcher or settings.
- In `SettingsScreen.kt`:
  - If permissions are missing, show an explanatory status alert under the Foreground Service switch.

### Phase 5: Automated Robolectric Unit Test Suite
- Create `app/src/test/java/com/example/OnboardingPermissionSkipTest.kt`:
  - Configure `@RunWith(RobolectricTestRunner::class)` and `@Config(sdk = [34])`.
  - Test 1: Verify `skipOnboarding()` sets `isOnboardingCompleted = true` and `isServiceEnabled = false`.
  - Test 2: Verify `BluetoothWatcherService.onStartCommand()` without permissions calls `stopSelf()` without calling `startForeground` or throwing `SecurityException`.
  - Test 3: Verify `BluetoothHelper.hasRequiredPermissionsForService()` accurately reflects runtime permission state.
- Run `./gradlew.bat testDebugUnitTest` to ensure all tests pass.

---

## Complexity Tracking

| Issue | Resolution |
| :--- | :--- |
| Android 14 `SecurityException` on `startForeground` | Prevented via defensive pre-check in `startService()` and inside `onStartCommand()` before invoking `ServiceCompat.startForeground`. |
| Reboot crash-loop via `BootReceiver` | Solved by actively resetting `isServiceEnabled` to `false` and validating permissions inside `BootReceiver`. |
| User stuck in onboarding if they skip | Solved by setting `isOnboardingCompleted = true` while keeping service stopped until permissions are granted. |
