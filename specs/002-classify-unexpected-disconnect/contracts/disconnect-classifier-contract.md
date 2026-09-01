# Component Contract: Disconnect Classification & Adapter State

**Feature**: Classify Unexpected Disconnect (`002-classify-unexpected-disconnect`)  
**Status**: Completed  
**Date**: 2026-09-02  

---

## 1. Public / Companion Contract: `BluetoothEventReceiver`

```kotlin
class BluetoothEventReceiver : BroadcastReceiver() {
    companion object {
        /**
         * Heuristic window (in milliseconds) within which a device disconnection
         * following an adapter state change to TURNING_OFF or OFF is treated as intentional.
         */
        const val ADAPTER_OFF_HEURISTIC_WINDOW_MS = 4000L

        @Volatile
        var lastAdapterOffTimestamp: Long = 0L

        /**
         * Returns true if BluetoothAdapter turned off or began turning off within the heuristic window.
         */
        fun isRecentAdapterOff(currentTime: Long = System.currentTimeMillis()): Boolean

        /**
         * Reset timestamp for testing isolation.
         */
        fun resetAdapterStateForTesting()
    }
}
```

---

## 2. Broadcast Action Contracts Handled

### 2.1 `BluetoothAdapter.ACTION_STATE_CHANGED`
- **Intent Action**: `android.bluetooth.adapter.action.STATE_CHANGED`
- **Extra**: `BluetoothAdapter.EXTRA_STATE` (`Int`)
- **Handling**:
  - `BluetoothAdapter.STATE_TURNING_OFF` (13) or `BluetoothAdapter.STATE_OFF` (10):
    Sets `lastAdapterOffTimestamp = System.currentTimeMillis()`.
  - `BluetoothAdapter.STATE_ON` (12) or `BluetoothAdapter.STATE_TURNING_ON` (11):
    Resets `lastAdapterOffTimestamp = 0L`.

### 2.2 `BluetoothDevice.ACTION_ACL_DISCONNECTED`
- **Intent Action**: `android.bluetooth.device.action.ACL_DISCONNECTED`
- **Extra**: `BluetoothDevice.EXTRA_DEVICE` (`BluetoothDevice`)
- **Handling**:
  - Evaluates `isUnexpectedDisconnect = !isRecentAdapterOff(timestamp)`.
  - Dispatches `repository.recordBluetoothEvent(..., isUnexpectedDisconnect = isUnexpectedDisconnect)`.
  - Evaluates notification: alerts only if `isUnexpectedDisconnect == true`.

### 2.3 Profile `CONNECTION_STATE_CHANGED` (A2DP / Headset)
- **Extras**: `BluetoothProfile.EXTRA_STATE`, `BluetoothProfile.EXTRA_PREVIOUS_STATE`
- **Handling**:
  - When transitioning from `STATE_CONNECTED` to `STATE_DISCONNECTED`:
  - Evaluates `isUnexpectedDisconnect = !isRecentAdapterOff(timestamp)`.
  - Dispatches `repository.recordBluetoothEvent(..., isUnexpectedDisconnect = isUnexpectedDisconnect)`.
  - Evaluates notification: alerts only if `isUnexpectedDisconnect == true`.
