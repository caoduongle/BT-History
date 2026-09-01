# Data & State Model: Onboarding Lifecycle and Service Permission States

**Feature**: Safe Permission Onboarding & Android 14+ Foreground Service Crash Prevention (`008-fix-onboarding-permission-crash`)  
**Date**: 2026-09-02  

---

## 1. Preferences DataStore Model

Preferences are stored in Android Jetpack Preferences DataStore (`bt_watcher_settings`):

| Preference Key | Type | Default | Mutated By | Purpose & Semantics |
| :--- | :--- | :--- | :--- | :--- |
| `onboarding_completed` | `Boolean` | `false` | `DeviceViewModel.completeOnboarding()`<br>`DeviceViewModel.skipOnboarding()` | Determines whether the user has passed onboarding. Once `true`, app routes to `device_list`. |
| `service_enabled` | `Boolean` | `true` | `DeviceViewModel.toggleService()`<br>`DeviceViewModel.skipOnboarding()`<br>`BluetoothWatcherService.onStartCommand()`<br>`BootReceiver.onReceive()` | Controls whether `BluetoothWatcherService` should actively monitor devices in the foreground. Automatically set to `false` when required permissions are missing. |
| `disconnect_alert` | `Boolean` | `true` | `DeviceViewModel.toggleDisconnectAlert()` | Enables high-priority drop notifications when an unexpected disconnection occurs. |

---

## 2. Onboarding Lifecycle & State Transitions

```
                    ┌────────────────────────┐
                    │ App Launch (Cold Boot) │
                    └───────────┬────────────┘
                                │
                  isOnboardingCompleted == false?
                                │
               ┌────────────────┴────────────────┐
               │ YES                             │ NO
               ▼                                 ▼
┌──────────────────────────────┐  ┌──────────────────────────────┐
│  PermissionOnboardingScreen  │  │       DeviceListScreen       │
└──────┬────────────────┬──────┘  └──────────────────────────────┘
       │                │
User clicks             User clicks
"Cấp quyền & Bắt đầu"   "Bỏ qua / Vào giao diện chính"
       │                │
       ▼                ▼
Permission Dialog    onSkip() triggered
       │                │
       ├─ Granted?      └────────────────────────┐
       │                                         │
       ├──► YES: onPermissionsGranted()          │
       │    ├─ setOnboardingCompleted(true)      │
       │    ├─ setServiceEnabled(true)           │
       │    └─ startService(context)             │
       │                                         │
       └──► NO: Stay or user skips               │
                                                 ▼
                                        skipOnboarding()
                                        ├─ setOnboardingCompleted(true)
                                        ├─ setServiceEnabled(false)
                                        └─ DO NOT startService()
                                                 │
                                                 ▼
                                        Navigate to DeviceListScreen
                                        (Banner shows service is OFF)
```

---

## 3. Foreground Service Permission Guard State Machine

```
              ┌────────────────────────────────────────┐
              │ BluetoothWatcherService.onStartCommand │
              └───────────────────┬────────────────────┘
                                  │
                 hasRequiredPermissionsForService()?
                                  │
               ┌──────────────────┴──────────────────┐
               │ TRUE                                │ FALSE
               ▼                                     ▼
┌──────────────────────────────┐      ┌──────────────────────────────┐
│ - Build ongoing notification │      │ - Log warning/error          │
│ - startForeground(...) with  │      │ - stopForeground(REMOVE)     │
│   CONNECTED_DEVICE|LOCATION  │      │ - stopSelf()                 │
│ - Register dynamic receiver  │      │ - setServiceEnabled(false)   │
│ - Return START_STICKY        │      │ - Return START_NOT_STICKY    │
└──────────────────────────────┘      └──────────────────────────────┘
```

---

## 4. BootReceiver Evaluation Matrix

When `ACTION_BOOT_COMPLETED` or `ACTION_MY_PACKAGE_REPLACED` is delivered:

| `isServiceEnabled` | `hasRequiredPermissionsForService` | Resulting Action |
| :--- | :--- | :--- |
| `true` | `true` | Call `BluetoothWatcherService.startService(context)`. Service starts normally. |
| `true` | `false` | **Do NOT start service.** Update `preferencesRepository.setServiceEnabled(false)` to prevent future crash loops. |
| `false` | *Any* | No action taken. Service remains stopped. |
