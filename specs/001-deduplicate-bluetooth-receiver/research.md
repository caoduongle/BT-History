# Technical Research: Deduplicate Bluetooth Receiver

**Feature**: Deduplicate Bluetooth Receiver (`001-deduplicate-bluetooth-receiver`)  
**Status**: Completed  
**Date**: 2026-09-02  

---

## 1. Background & Problem Analysis

In the BT-History Android application, Bluetooth event monitoring was experiencing duplicate processing:
- When a Bluetooth peripheral connected or disconnected, two database event rows were created.
- High-accuracy GPS location resolution via `LocationHelper` was invoked twice consecutively.
- If disconnect notifications were enabled, two duplicate heads-up notifications were dispatched to the notification tray.

### Root Cause
The root cause was dual registration of `BluetoothEventReceiver`:
1. **Static Manifest Registration**: Declared in `app/src/main/AndroidManifest.xml` under `<receiver android:name=".receiver.BluetoothEventReceiver" android:exported="true">` with intent filters for `ACL_CONNECTED`, `ACL_DISCONNECTED`, etc.
2. **Dynamic Context Registration**: Registered programmatically in `BluetoothWatcherService.registerBluetoothReceiver()` via `registerReceiver(dynamicReceiver, filter, Context.RECEIVER_EXPORTED)`.

When the foreground service is active, both registrations receive the system broadcast, triggering two separate instances of `BluetoothEventReceiver.onReceive()`.

---

## 2. Research Decisions

### Decision 1: Remove Static Manifest Receiver and Rely Exclusively on Dynamic Registration
- **Decision**: Completely delete `<receiver android:name=".receiver.BluetoothEventReceiver">` from `app/src/main/AndroidManifest.xml`. Retain and rely exclusively on dynamic registration inside `BluetoothWatcherService`.
- **Rationale**:
  - **Android 8.0+ (API 26) Background Execution Limits**: Android severely restricts implicit broadcast receivers declared in `AndroidManifest.xml`. Bluetooth actions such as `BluetoothDevice.ACTION_ACL_CONNECTED` and `BluetoothDevice.ACTION_ACL_DISCONNECTED` are implicit system broadcasts and are not on the whitelist of exempt implicit broadcasts. Declaring them in the manifest either fails to wake up background apps or causes unexpected behavior.
  - **Foreground Service Alignment**: BT-History operates a continuous foreground service (`BluetoothWatcherService`) with `foregroundServiceType="connectedDevice|location"`. While this foreground service runs, the application process remains alive and prioritized. A dynamic receiver registered by this service reliably intercepts all Bluetooth events from the OS.
  - **User Intent & Battery Control**: Dynamic registration allows event listening to be turned on/off along with the service lifecycle. If the user stops tracking, no events are caught or processed, conserving battery.
- **Alternatives Considered**:
  - *Manifest-only registration*: Incompatible with Android 8+ background limits; unable to reliably monitor Bluetooth in the background without an active process/service.
  - *Application-level deduplication window (debouncing in DB/repository)*: Adding a 1-second debounce or timestamp-based deduplication in Room would mask the symptom rather than solving the root cause. It would still waste system resources, wake lock time, and battery on duplicate location and receiver invocations.

### Decision 2: Strict Service Lifecycle Pairing (Registration and Teardown)
- **Decision**: Register the receiver in `BluetoothWatcherService.onCreate()` and unregister in `BluetoothWatcherService.onDestroy()`.
- **Rationale**:
  - `onCreate()` is executed exactly once when the service instance is created, before any commands (`onStartCommand()`) are handled. This ensures the receiver is active as soon as the service exists.
  - `onStartCommand()` may be triggered multiple times across the service lifecycle (e.g. repeated user actions, system restart with `START_STICKY`). Registering in `onStartCommand()` would require defensive checks against double registration.
  - In `onDestroy()`, calling `unregisterReceiver()` inside a `try-catch(e: Exception)` block and setting `dynamicReceiver = null` guarantees that:
    1. The receiver reference is released and no `IntentReceiverLeaked` warning is produced by the Android framework.
    2. If unregistration is attempted when already unregistered, `IllegalArgumentException` is safely caught.
    3. The service's CoroutineScope (`serviceJob.cancel()`) is canceled properly.
- **Alternatives Considered**:
  - *Registering in Application.onCreate()*: Process-level receiver would remain active even if the user deliberately stops the tracking service from settings or notification.

### Decision 3: Export Flag Compliance for Android 14+ (API 34+)
- **Decision**: Maintain `Context.RECEIVER_EXPORTED` on API 33+ (Tiramisu and above):
  ```kotlin
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      registerReceiver(dynamicReceiver, filter, Context.RECEIVER_EXPORTED)
  } else {
      registerReceiver(dynamicReceiver, filter)
  }
  ```
- **Rationale**: Android 14 (API 34) mandates that every dynamically registered receiver specify either `RECEIVER_EXPORTED` or `RECEIVER_NOT_EXPORTED`. Since Bluetooth broadcasts (`ACL_CONNECTED`, `ACL_DISCONNECTED`) originate from the system Bluetooth process (`com.android.bluetooth`), which is an external UID, the receiver must be marked `RECEIVER_EXPORTED` to receive them.
- **Alternatives Considered**:
  - *`RECEIVER_NOT_EXPORTED`*: Would cause Android 14+ to drop all system Bluetooth broadcasts, completely breaking device tracking.

### Decision 4: Automated Verification Strategy
- **Decision**: Use a Robolectric test to simulate an `ACL_CONNECTED` broadcast intent and assert that exactly one `EventEntity` is inserted into the Room database.
- **Rationale**:
  - Robolectric runs fast on the JVM without an emulator or physical device, making it ideal for continuous integration.
  - Robolectric provides full Android component lifecycles, shadows for `ApplicationProvider`, and supports in-memory Room database instances.
  - Dispatching an `Intent(BluetoothDevice.ACTION_ACL_CONNECTED)` via `context.sendBroadcast(intent)` will execute `BluetoothEventReceiver.onReceive()`, run the coroutine on `Dispatchers.IO`, and verify the end-to-end flow.
- **Alternatives Considered**:
  - *MockK/Mockito Unit Test*: Only tests isolated method calls without verifying IntentFilter parsing, BroadcastReceiver behavior, or real database insertion.
  - *Physical Device Instrumented Test*: Slow, requires pairing a physical Bluetooth peripheral during test execution.

---

## 3. Best Practices & Android Platform Considerations

1. **Android 8.0+ Implicit Broadcast Ban**:
   - `android.bluetooth.device.action.ACL_CONNECTED` and `ACL_DISCONNECTED` are implicit broadcasts.
   - Per Google's official Android documentation, apps targeting API 26+ cannot use manifest receivers for implicit broadcasts unless explicitly whitelisted.
   - Dynamic registration within a running Service or Activity is the recommended architecture.

2. **Code Commenting**:
   - Add concise architectural rationale in `BluetoothWatcherService.kt` and `AndroidManifest.xml` explaining why dynamic registration is employed over static manifest declaration.
