# Quickstart Validation Guide: Deduplicate Bluetooth Receiver

**Feature**: Deduplicate Bluetooth Receiver (`001-deduplicate-bluetooth-receiver`)  
**Status**: Ready for Validation  
**Date**: 2026-09-02  

---

## 1. Prerequisites

- Android SDK installed with `compileSdk 36`
- JDK 17 or 21 configured
- Gradle build tool available (e.g. `./gradlew` or local Gradle 8.14+/9.1+)

---

## 2. Automated Test Validation (Robolectric)

### Scenario A: Single Event Record Assertion on `ACL_CONNECTED`

This automated test validates that dispatching a single `ACTION_ACL_CONNECTED` broadcast produces exactly 1 `EventEntity` record in the database.

**Execution Command**:
```powershell
./gradlew testDebugUnitTest --tests "com.example.BluetoothDeduplicationTest"
```

**Expected Outcome**:
- `BluetoothEventReceiver` receives the broadcast.
- The repository executes `recordBluetoothEvent(...)`.
- `EventDao.getAllEventsFlow()` or `eventDao.getEventsForDevice(...)` contains exactly 1 row (assert `assertEquals(1, events.size)`).
- Test passes with 0 failures and 0 errors.

---

## 3. Manual / Device Verification Scenarios

### Scenario B: Connect & Disconnect Verification on Physical Device or Emulator

1. **Setup**:
   - Install and launch the application:
     ```powershell
     ./gradlew installDebug
     ```
   - Grant necessary Bluetooth and Location permissions during onboarding.
   - Start the background monitoring service from the app interface.
   - Verify foreground notification appears: *"Đang lắng nghe kết nối"*.

2. **Step 1: Peripheral Connection**:
   - Turn on and connect a paired Bluetooth device (e.g., headphones, watch, or car audio).
   - Open BT-History app to the timeline screen.
   - **Verification**:
     - Exactly **one** event entry appears: `[CONNECT] <Device Name>` with timestamp and location address.
     - Device count in foreground notification increases by 1.

3. **Step 2: Unexpected Peripheral Disconnection**:
   - Turn off the connected Bluetooth device or walk out of range.
   - **Verification**:
     - Exactly **one** event entry appears: `[DISCONNECT] <Device Name>`.
     - Exactly **one** disconnect alert notification is displayed (if disconnect alerts are turned ON in Settings).

4. **Step 3: Service Stop and Cleanup**:
   - Stop the monitoring service from the Settings or main screen.
   - Connect or disconnect the device again.
   - **Verification**:
     - No new events are recorded while the service is stopped.
     - No memory leaks or receiver crashes (`ReceiverNotRegisteredException`) occur.
