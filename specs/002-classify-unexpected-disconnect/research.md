# Technical Research: Classify Unexpected Disconnect

**Feature**: Classify Unexpected Disconnect (`002-classify-unexpected-disconnect`)  
**Status**: Completed  
**Date**: 2026-09-02  

---

## 1. Problem Definition & Context

In `BluetoothEventReceiver.kt`, `isUnexpectedDisconnect` was previously assigned `true` unconditionally whenever:
1. `BluetoothDevice.ACTION_ACL_DISCONNECTED` was received.
2. An A2DP or Headset profile connection transitioned to `BluetoothProfile.STATE_DISCONNECTED` from `STATE_CONNECTED`.

### The Problem
When a user manually turns off Bluetooth via Quick Settings or System Settings, or turns on Airplane Mode:
- The Android Bluetooth stack tears down active connections, firing `ACTION_ACL_DISCONNECTED`.
- The application treated this intentional action as an unexpected disconnect, immediately acquiring GPS location and firing a heads-up warning notification: *"Thiết bị bị ngắt kết nối đột ngột"*.
- This resulted in frequent false alarms, annoying users and eroding confidence in genuine disconnect warnings.

---

## 2. Research Decisions

### Decision 1: Heuristic Classification Based on `BluetoothAdapter.ACTION_STATE_CHANGED`
- **Decision**: Monitor `BluetoothAdapter.ACTION_STATE_CHANGED` in `BluetoothEventReceiver`. When state changes to `STATE_TURNING_OFF` or `STATE_OFF`, record a timestamp (`lastAdapterOffTimestamp`). When any disconnection occurs, check if `(currentTimestamp - lastAdapterOffTimestamp) <= ADAPTER_OFF_HEURISTIC_WINDOW_MS`:
  - If **within window**: Classify as **intentional disconnection** (`isUnexpectedDisconnect = false`). Do **not** show the disconnect alert notification.
  - If **outside window** (or no adapter turn-off occurred): Classify as **unexpected disconnection** (`isUnexpectedDisconnect = true`). Show the disconnect alert notification if enabled.
- **Rationale**:
  - Android OS dispatches `BluetoothAdapter.ACTION_STATE_CHANGED` with `STATE_TURNING_OFF` immediately when the user taps the Bluetooth tile or toggles Bluetooth off in settings.
  - Peripherals are subsequently disconnected as the radio shuts down.
  - Correlating these two broadcast events within a temporal window provides an accurate, non-invasive indicator of user intent without requiring private APIs or root permissions.
- **Alternatives Considered**:
  - *Checking `BluetoothAdapter.getDefaultAdapter().isEnabled` during disconnect*: Unreliable because during the transition phase (`STATE_TURNING_OFF`), `isEnabled` may still return `true` or state transitions may be raced by asynchronous broadcast queues.
  - *Requiring user to toggle tracking off before Bluetooth*: Poor user experience; users expect to toggle Bluetooth freely from the quick settings bar.

### Decision 2: In-Memory Volatile State Storage
- **Decision**: Store `lastAdapterOffTimestamp` as a `@Volatile` in-memory timestamp in `BluetoothEventReceiver.Companion` (with a helper method or reset capability for testing).
- **Rationale**:
  - Zero disk I/O overhead during broadcast handling.
  - `BluetoothWatcherService` maintains the foreground process while active, ensuring in-memory state persistence during active tracking.
  - Thread-safe access across concurrent broadcast dispatches.
  - Resetting timestamp to `0L` when `STATE_ON` or `STATE_TURNING_ON` is detected prevents stale timestamps from suppressing legitimate future disconnects.
- **Alternatives Considered**:
  - *DataStore / SharedPreferences*: Unnecessary disk writes for ephemeral temporal states spanning 3–5 seconds.

### Decision 3: Heuristic Window Duration & Constant Definition
- **Decision**: Define `const val ADAPTER_OFF_HEURISTIC_WINDOW_MS = 4000L` (4 seconds).
- **Rationale**:
  - Android device hardware and OEM skins (OneUI, MIUI, Pixel UI) typically complete radio teardown and broadcast dispatch within 200ms to 2500ms.
  - A 4000ms window provides sufficient headroom for OS broadcast queue latency while keeping the window short enough that a genuine unexpected disconnect minutes or hours later cannot be falsely suppressed.
- **Heuristic Trade-offs & Limitations**:
  - *Edge Case 1*: If a peripheral drops connection 1 second before the user happens to manually turn off Bluetooth, the disconnect might be marked intentional. This is an acceptable trade-off because the user was actively turning off Bluetooth anyway.
  - *Edge Case 2*: If device is under extreme OS throttling causing broadcast delivery delays > 4000ms, an intentional disconnect might be flagged unexpected. 4000ms is well above normal OEM thresholds to minimize this risk.

---

## 3. Implementation Blueprint

```kotlin
companion object {
    /**
     * Thời gian tối đa (ms) giữa sự kiện adapter bắt đầu tắt và sự kiện ngắt kết nối thiết bị
     * để coi là ngắt kết nối chủ động (không bắn thông báo cảnh báo).
     */
    const val ADAPTER_OFF_HEURISTIC_WINDOW_MS = 4000L

    @Volatile
    var lastAdapterOffTimestamp: Long = 0L

    fun isRecentAdapterOff(currentTime: Long = System.currentTimeMillis()): Boolean {
        val lastOff = lastAdapterOffTimestamp
        return lastOff > 0L && (currentTime - lastOff) in 0L..ADAPTER_OFF_HEURISTIC_WINDOW_MS
    }
}
```

When handling actions:
1. `BluetoothAdapter.ACTION_STATE_CHANGED`:
   - If `state == BluetoothAdapter.STATE_TURNING_OFF || state == BluetoothAdapter.STATE_OFF`: update `lastAdapterOffTimestamp = System.currentTimeMillis()`.
   - If `state == BluetoothAdapter.STATE_ON || state == BluetoothAdapter.STATE_TURNING_ON`: reset `lastAdapterOffTimestamp = 0L`.
2. Disconnect actions (`ACL_DISCONNECTED` or profile `STATE_DISCONNECTED`):
   - `isUnexpectedDisconnect = !isRecentAdapterOff(timestamp)`
   - Only trigger `NotificationHelper.showDisconnectAlert(...)` if `isUnexpectedDisconnect == true`.
