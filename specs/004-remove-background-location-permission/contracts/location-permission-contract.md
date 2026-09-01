# Component Contract: Location & Foreground Service Permissions

**Feature**: Remove Redundant Background Location Permission (`004-remove-background-location-permission`)  
**Status**: Completed  
**Date**: 2026-09-02  

---

## 1. Manifest Contract (`AndroidManifest.xml`)

### Declared Location Permissions:
```xml
<!-- Standard Foreground Location Permissions (Granted at runtime: "While using the app") -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Foreground Service Type Permission for Location (API 34+) -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />

<!-- NOTE: android.permission.ACCESS_BACKGROUND_LOCATION is INTENTIONALLY EXCLUDED. -->
```

### Service Declaration Contract:
```xml
<service
    android:name=".service.BluetoothWatcherService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="connectedDevice|location" />
```

---

## 2. Runtime UI Permission Request Contract

### `PermissionOnboardingScreen.kt` & `SettingsScreen.kt`
- Must only request:
  - `Manifest.permission.ACCESS_FINE_LOCATION`
  - `Manifest.permission.ACCESS_COARSE_LOCATION`
  - `Manifest.permission.BLUETOOTH_CONNECT` (API 31+)
  - `Manifest.permission.BLUETOOTH_SCAN` (API 31+)
  - `Manifest.permission.POST_NOTIFICATIONS` (API 33+)
- **Must NEVER request**: `Manifest.permission.ACCESS_BACKGROUND_LOCATION`.
