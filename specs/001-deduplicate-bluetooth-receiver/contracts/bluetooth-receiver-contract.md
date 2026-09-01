# Component Contract: Bluetooth Broadcast & Service Lifecycle

**Feature**: Deduplicate Bluetooth Receiver (`001-deduplicate-bluetooth-receiver`)  
**Status**: Completed  
**Date**: 2026-09-02  

---

## 1. Broadcast Receiver Contract: `BluetoothEventReceiver`

### 1.1 Intent Filters Handled

The dynamic receiver is bound to an `IntentFilter` configured with the following system actions:

| Action | Intent Constant | Expected Extras | Trigger / Meaning |
| :--- | :--- | :--- | :--- |
| `android.bluetooth.device.action.ACL_CONNECTED` | `BluetoothDevice.ACTION_ACL_CONNECTED` | `BluetoothDevice.EXTRA_DEVICE` (`BluetoothDevice`) | Peripheral connected at ACL level |
| `android.bluetooth.device.action.ACL_DISCONNECTED` | `BluetoothDevice.ACTION_ACL_DISCONNECTED` | `BluetoothDevice.EXTRA_DEVICE` (`BluetoothDevice`) | Peripheral disconnected at ACL level |
| `android.bluetooth.device.action.BOND_STATE_CHANGED` | `BluetoothDevice.ACTION_BOND_STATE_CHANGED` | `BluetoothDevice.EXTRA_DEVICE`, `EXTRA_BOND_STATE` | Peripheral pairing state altered |
| `android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED` | `BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED` | `BluetoothProfile.EXTRA_STATE`, `EXTRA_PREVIOUS_STATE`, `EXTRA_DEVICE` | Audio A2DP profile connect/disconnect |
| `android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED` | `BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED` | `BluetoothProfile.EXTRA_STATE`, `EXTRA_PREVIOUS_STATE`, `EXTRA_DEVICE` | Handsfree/Headset profile connect/disconnect |
| `android.bluetooth.adapter.action.STATE_CHANGED` | `BluetoothAdapter.ACTION_STATE_CHANGED` | `BluetoothAdapter.EXTRA_STATE` | Phone's Bluetooth adapter turned ON/OFF |

### 1.2 Registration & Export Contract

- **Android 14+ (API 34+) Compliance**:
  - Must be registered with `Context.RECEIVER_EXPORTED` because system intents originate from `com.android.bluetooth` (external UID).
- **Registration Location**:
  - Dynamic registration inside `BluetoothWatcherService.onCreate()`.
  - Statically removed from `AndroidManifest.xml`.
- **Execution Model**:
  - `onReceive()` calls `goAsync()`.
  - Asynchronous execution runs on `Dispatchers.IO`.
  - Releases execution slot via `pendingResult.finish()` in `finally`.

---

## 2. Service Lifecycle Contract: `BluetoothWatcherService`

### 2.1 Lifecycle State Transitions

| Lifecycle Event | Action Taken | Guarantee |
| :--- | :--- | :--- |
| `onCreate()` | Initializes notification channels, registers `BluetoothEventReceiver`, observes connected device count | Receiver active before any intents can be routed |
| `onStartCommand()` | Promotes to foreground service (`ServiceCompat.startForeground`) with `connectedDevice \| location` types | Process protected from OS background killer |
| `onDestroy()` | Unregisters `BluetoothEventReceiver`, sets `dynamicReceiver = null`, cancels `serviceJob` | No `IntentReceiverLeaked`, no crashes on stop/restart |

### 2.2 Foreground Service Declaration (`AndroidManifest.xml`)

```xml
<service
    android:name=".service.BluetoothWatcherService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="connectedDevice|location" />
```
*(Remains active, unexposed to external apps).*
