# Technical Research: Foreground Service Location vs Background Location

**Feature**: Remove Redundant Background Location Permission (`004-remove-background-location-permission`)  
**Status**: Completed  
**Date**: 2026-09-02  

---

## 1. Problem Statement

`app/src/main/AndroidManifest.xml` previously included:
```xml
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

### The Questions to Answer
1. Does the app actually need `ACCESS_BACKGROUND_LOCATION`?
2. How does Android define location access within a Foreground Service (`foregroundServiceType="location"`)?
3. What are the Google Play Store policy ramifications of keeping vs. removing this permission?

---

## 2. Android OS Location Model (API 29+)

### 2.1 The Two Types of Location Access in Android
| Concept | Scope | Required Permissions | User Prompt |
| :--- | :--- | :--- | :--- |
| **Foreground Location** | When the app is in the foreground (Activity visible) **OR** when a Foreground Service with `foregroundServiceType="location"` is active and showing an ongoing notification. | `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` + `FOREGROUND_SERVICE_LOCATION` | *"While using the app"* (One-click approval) |
| **Background Location** | When location is accessed **WITHOUT** any visible UI and **WITHOUT** an active foreground service (e.g. headless WorkManager, AlarmManager, geofencing). | `ACCESS_BACKGROUND_LOCATION` | *"Allow all the time"* (Requires navigating into Android System Settings) |

### 2.2 Execution Path in BT-History
In BT-History:
1. `BluetoothWatcherService` is started as a **Foreground Service** using `startForegroundService()` and calls `startForeground()` with a persistent notification.
2. `AndroidManifest.xml` explicitly declares:
   ```xml
   <service
       android:name=".service.BluetoothWatcherService"
       android:foregroundServiceType="connectedDevice|location" />
   ```
3. Location is queried **only** in `LocationHelper.getCurrentLocation(context)` when a Bluetooth broadcast is delivered to `BluetoothEventReceiver`.
4. Because `BluetoothWatcherService` is actively running with `foregroundServiceType="location"`, Android treats the broadcast receiver's location query as **Foreground Location Access**.
5. There are **zero** background threads, WorkManager workers, or alarms querying location when the service is stopped.

**Conclusion**: `ACCESS_BACKGROUND_LOCATION` is **100% redundant and unnecessary**.

---

## 3. Google Play Store Policy Implications

### 3.1 Risks of Retaining `ACCESS_BACKGROUND_LOCATION`
- **Mandatory Policy Declaration**: Google Play Console mandates that any app requesting `ACCESS_BACKGROUND_LOCATION` submit an extensive justification form and an in-app video recording demonstrating why background location is an indispensable core feature.
- **Strict Scrutiny**: Google Play Review Guidelines state:
  > *"If location access is only needed when the user is actively using the app or while an ongoing notification is displayed, you must use a foreground service instead of requesting background location."*
- **Rejection / Removal Risk**: If an app declares `ACCESS_BACKGROUND_LOCATION` while its functionality is satisfied by a Foreground Service, Google Play reviewers reject the submission or issue policy violation warnings.
- **Two-Step Permission Friction**: Android 11+ prohibits requesting background location in the same dialog as foreground location. It requires a custom in-app educational disclosure followed by launching system settings.

### 3.2 Benefits of Removing `ACCESS_BACKGROUND_LOCATION`
1. **Zero Policy Burden**: No Google Play background location declaration form or review video required.
2. **Smooth User Experience**: Users grant *"While using the app"* directly within the initial onboarding flow without leaving the app.
3. **Enhanced User Trust**: Users are not suspicious of an invasive *"Allow all the time"* tracking request.
4. **Clean Manifest**: Zero dead permissions.

---

## 4. Implementation Decisions

1. **Remove from Manifest**: Delete `<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />` from `app/src/main/AndroidManifest.xml`.
2. **Retain Required Foreground Permissions**:
   - `ACCESS_FINE_LOCATION`
   - `ACCESS_COARSE_LOCATION`
   - `FOREGROUND_SERVICE`
   - `FOREGROUND_SERVICE_LOCATION`
   - `android:foregroundServiceType="connectedDevice|location"` on `BluetoothWatcherService`.
3. **Verify Onboarding & Settings**: Ensure `PermissionOnboardingScreen.kt` and `SettingsScreen.kt` remain clean (they already only request `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`).
4. **Documentation**: Add architectural comments in `AndroidManifest.xml` and document the location permission rationale in `README.md`.
