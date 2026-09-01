# Technical Research: Android 14+ Foreground Service Types & Permission Lifecycle

**Feature**: Safe Permission Onboarding & Android 14+ Foreground Service Crash Prevention (`008-fix-onboarding-permission-crash`)  
**Status**: Completed  
**Date**: 2026-09-02  

---

## 1. Problem Statement & Crash Reproduction Analysis

### 1.1 The Crash Mechanism on Android 14 (API 34+)
Starting in Android 14 (API level 34, `UPSIDE_DOWN_CAKE`), Google implemented strict runtime permission enforcement for foreground services:
1. Every foreground service MUST declare at least one specific `android:foregroundServiceType` in `AndroidManifest.xml` (e.g. `connectedDevice`, `location`).
2. When calling `ServiceCompat.startForeground(service, id, notification, foregroundServiceType)`, Android OS validates that the calling application has been granted the runtime permissions required for each specified type:
   - `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE`: requires `android.permission.BLUETOOTH_CONNECT` (on API 31+), or `BLUETOOTH_ADVERTISE`, or `BLUETOOTH_SCAN`, or `UWB_RANGING`.
   - `FOREGROUND_SERVICE_TYPE_LOCATION`: requires `android.permission.ACCESS_FINE_LOCATION` or `android.permission.ACCESS_COARSE_LOCATION`.
3. If ANY required runtime permission for the specified type is not granted, Android OS immediately throws a `SecurityException` / `MissingForegroundServiceTypeException` inside `startForeground()`.

### 1.2 The Root Cause in BT-History
In BT-History:
1. `PermissionOnboardingScreen.kt`:
   The "Bỏ qua / Vào giao diện chính" button (`testTag("skip_onboarding_button")`) invokes the single callback parameter `onPermissionsGranted()`.
2. `MainActivity.kt`:
   `onboarding` composable sets:
   ```kotlin
   PermissionOnboardingScreen(
       onPermissionsGranted = {
           viewModel.completeOnboarding()
           navController.navigate("device_list") { popUpTo("onboarding") { inclusive = true } }
       }
   )
   ```
3. `DeviceViewModel.kt`:
   `completeOnboarding()` checks `if (isServiceEnabled.value)` (which defaults to `true`) and immediately calls `BluetoothWatcherService.startService(context)`.
4. `BluetoothWatcherService.kt`:
   `onStartCommand()` unconditionally calls:
   ```kotlin
   var foregroundServiceType = 0
   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
       foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
               ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
   }
   ServiceCompat.startForeground(this, NotificationHelper.SERVICE_NOTIFICATION_ID, notification, foregroundServiceType)
   ```
5. Result: Because the user clicked "Bỏ qua", neither `BLUETOOTH_CONNECT` nor `ACCESS_FINE_LOCATION` has been granted. The app immediately crashes with `SecurityException`.

### 1.3 The Secondary Crash Loop (Boot / Package Update)
In `PreferencesRepository.kt`:
- `isServiceEnabledFlow` defaults to `true`.
- If `completeOnboarding()` or skipping leaves `isServiceEnabled` as `true`, then when the phone boots (`ACTION_BOOT_COMPLETED`), `BootReceiver.kt` executes:
  ```kotlin
  val isServiceEnabled = app.preferencesRepository.isServiceEnabledFlow.first()
  if (isServiceEnabled) {
      BluetoothWatcherService.startService(context)
  }
  ```
  This crashes the app in the background repeatedly on every device restart, degrading device stability.

---

## 2. Architectural Solution

```
User Action: "Bỏ qua"
       │
       ▼
PermissionOnboardingScreen (invokes onSkip())
       │
       ▼
MainActivity (invokes viewModel.skipOnboarding())
       │
       ├─► PreferencesRepository.setOnboardingCompleted(true)
       └─► PreferencesRepository.setServiceEnabled(false) [NO service start!]
       │
       ▼
Navigate to DeviceListScreen
       │
       └─► Shows "Service Disabled - Missing Permissions" Banner + Action Button
```

### 2.1 Defense-in-Depth Protection Matrix

| Component | Responsibility Before Change | Responsibility After Change |
| :--- | :--- | :--- |
| `PermissionOnboardingScreen` | Shared `onPermissionsGranted` for both Grant and Skip | Distinct `onPermissionsGranted` and `onSkip` callbacks |
| `DeviceViewModel` | Only `completeOnboarding()` which always started service if enabled | Added `skipOnboarding()`: sets `onboarding_completed=true` and disables service |
| `BluetoothHelper` | Individual permission checks only | New `hasRequiredPermissionsForService(context)` composite gatekeeper |
| `BluetoothWatcherService.startService` | Dispatched intent without permission check | Checks `hasRequiredPermissionsForService`; aborts cleanly if missing |
| `BluetoothWatcherService.onStartCommand` | Called `startForeground` unconditionally | Checks `hasRequiredPermissionsForService`; if missing, calls `stopForeground`, `stopSelf()`, sets `service_enabled=false`, returns `START_NOT_STICKY` |
| `BootReceiver` | Started service if `isServiceEnabled` | Checks `hasRequiredPermissionsForService` before starting; resets pref to `false` if missing |
| `DeviceListScreen` & `SettingsScreen` | Indicated running/stopped without permission awareness | Prominent warning banner and status alerting user that service is OFF due to permissions, with 1-tap grant shortcut |

---

## 3. Platform Compatibility Matrix

| Android Version | Bluetooth Connect Permission | Location Permission | FGS Type Enforcement |
| :--- | :--- | :--- | :--- |
| **Android 14+ (API 34+)** | `BLUETOOTH_CONNECT` (Runtime) | `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` (Runtime) | **Strict**: Throws `SecurityException` at `startForeground()` if either permission is missing |
| **Android 12-13 (API 31-33)** | `BLUETOOTH_CONNECT` (Runtime) | `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` (Runtime) | Location FGS enforced; connectedDevice recommended |
| **Android 7.0-11 (API 24-30)** | Install-time manifest permission | `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` (Runtime) | Standard FGS without type-specific enforcement |

By enforcing `BluetoothHelper.hasRequiredPermissionsForService(context)` across all Android versions, the application guarantees rock-solid behavior regardless of OS API level.

---

## 4. Architectural Decisions, Rationale & Alternatives Considered

### Decision 1: Dedicated `onSkip` Callback in `PermissionOnboardingScreen`
- **Decision**: Add a distinct `onSkip: () -> Unit` parameter alongside `onPermissionsGranted: () -> Unit`.
- **Rationale**: Clean separation of intent. Skipping onboarding means the user opted out of tracking for now; granting permissions means the user explicitly opted in. Mixing both in one callback led to the original crash bug.
- **Alternatives Considered**: Passing a Boolean parameter `onComplete: (granted: Boolean) -> Unit`. Rejected because idiomatic Compose screen contracts use discrete lambda actions (`onPermissionsGranted`, `onSkip`).

### Decision 2: Centralized Composite Permission Gatekeeper in `BluetoothHelper`
- **Decision**: Add `hasRequiredPermissionsForService(context: Context): Boolean` that verifies both Bluetooth connect and location permissions.
- **Rationale**: Eliminates duplication of permission checks across `BluetoothWatcherService.startService()`, `BluetoothWatcherService.onStartCommand()`, `BootReceiver`, and `DeviceViewModel`.
- **Alternatives Considered**: Checking permissions independently at each call site. Rejected due to drift and maintenance risk as Android permission policies evolve.

### Decision 3: Service Auto-Shutdown (`stopSelf()`) + Disabling Preference vs Catching `SecurityException`
- **Decision**: Validate permissions before calling `ServiceCompat.startForeground()`. If missing, immediately call `stopForeground(STOP_FOREGROUND_REMOVE)`, `stopSelf()`, set `PreferencesRepository.setServiceEnabled(false)`, and return `START_NOT_STICKY`.
- **Rationale**: Catching `SecurityException` around `startForeground()` is invalid under Android OS guidelines—an ANR will occur within 5 seconds if `startForegroundService()` is called and `startForeground()` is not completed. The service must never be started in the first place, or must call `stopSelf()` immediately.
- **Alternatives Considered**: Wrapping `startForeground()` in a `try-catch` block. Rejected because Android platform kills the app process with an ANR if `startForeground` is caught without promoting to foreground.

### Decision 4: In-App UI Alert Banner on `DeviceListScreen` and `SettingsScreen`
- **Decision**: Display a high-visibility warning banner when `!isServiceEnabled` or permissions are missing, offering a direct 1-tap "Cấp quyền" shortcut.
- **Rationale**: Users who skip permissions must be informed that automatic tracking is paused, and provided a frictionless recovery path without having to find the app in Android OS Settings.
- **Alternatives Considered**: Silent failure or only showing status in Settings. Rejected because users would assume the app is malfunctioning when device events aren't tracked.
