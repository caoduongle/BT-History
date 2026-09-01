# Quickstart Validation Guide: Classify Unexpected Disconnect

**Feature**: Classify Unexpected Disconnect (`002-classify-unexpected-disconnect`)  
**Status**: Ready for Validation  
**Date**: 2026-09-02  

---

## 1. Automated Test Execution

Run the dedicated classification test suite via Gradle:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.DisconnectClassificationTest"
```

### Expected Test Results

1. `testManualBluetoothOff_doesNotTriggerDisconnectAlert`:
   - Simulates `BluetoothAdapter.ACTION_STATE_CHANGED` with `STATE_TURNING_OFF`.
   - Dispatches `BluetoothDevice.ACTION_ACL_DISCONNECTED`.
   - Asserts:
     - `EventEntity.isUnexpectedDisconnect == false`
     - Notification manager has 0 disconnect alert notifications posted.
2. `testSuddenDisconnect_triggersDisconnectAlert`:
   - Ensures no prior adapter-off broadcast.
   - Dispatches `BluetoothDevice.ACTION_ACL_DISCONNECTED`.
   - Asserts:
     - `EventEntity.isUnexpectedDisconnect == true`
     - Notification manager has 1 disconnect alert notification posted (when alert setting is enabled).

---

## 2. Manual On-Device Verification

### Scenario A: Intentional Disconnect (User turns off Bluetooth)
1. Ensure tracking service is running and a Bluetooth device is connected.
2. Swipe down quick settings and turn off Bluetooth.
3. Open BT-History app.
4. **Verify**:
   - Disconnection is recorded in timeline with `isUnexpectedDisconnect = false`.
   - **NO** heads-up notification alert appears.

### Scenario B: Unexpected Disconnect (Lost connection / out of range)
1. Ensure tracking service is running and a Bluetooth device is connected.
2. Turn off the peripheral device directly (e.g. press power button on headphones or walk out of range) while keeping phone Bluetooth turned ON.
3. **Verify**:
   - Disconnection is recorded in timeline with `isUnexpectedDisconnect = true`.
   - A disconnect warning notification appears with last known location.
