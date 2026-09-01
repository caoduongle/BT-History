# Component Contract: Foreground Service Permission Gatekeeper & Onboarding Decoupling

**Feature**: Safe Permission Onboarding & Android 14+ Foreground Service Crash Prevention (`008-fix-onboarding-permission-crash`)  
**Status**: Completed  
**Date**: 2026-09-02  

---

## 1. UI Component Contract (`PermissionOnboardingScreen.kt`)

```kotlin
@Composable
fun PermissionOnboardingScreen(
    onPermissionsGranted: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
)
```

- `onPermissionsGranted`: Invoked exclusively when all required runtime permissions (`BLUETOOTH_CONNECT` and location) are actively granted by the user via the permission dialog launcher.
- `onSkip`: Invoked exclusively when the user taps the "Bỏ qua / Vào giao diện chính" button (`testTag("skip_onboarding_button")`).

---

## 2. Utility Contract (`BluetoothHelper.kt`)

```kotlin
object BluetoothHelper {
    /**
     * Checks whether all mandatory permissions required to operate BluetoothWatcherService
     * as an Android 14+ Foreground Service (FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE | FOREGROUND_SERVICE_TYPE_LOCATION)
     * are granted.
     *
     * @param context Application or Activity context.
     * @return true if both Bluetooth connect permission (API 31+) and location permission (fine or coarse) are granted; false otherwise.
     */
    fun hasRequiredPermissionsForService(context: Context): Boolean
}
```

---

## 3. Service Lifecycle Contract (`BluetoothWatcherService.kt`)

### Entry point `startService(context: Context)`
```kotlin
companion object {
    fun startService(context: Context) {
        if (!BluetoothHelper.hasRequiredPermissionsForService(context)) {
            Log.w(TAG, "Cannot start BluetoothWatcherService: Missing required permissions")
            return
        }
        val intent = Intent(context, BluetoothWatcherService::class.java).apply {
            action = ACTION_START_SERVICE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
```

### Lifecycle Callback `onStartCommand(intent: Intent?, flags: Int, startId: Int): Int`
```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val action = intent?.action
    if (action == ACTION_STOP_SERVICE) {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
    }

    if (!BluetoothHelper.hasRequiredPermissionsForService(this)) {
        Log.e(TAG, "BluetoothWatcherService started without required permissions. Stopping safely.")
        serviceScope.launch {
            (application as? BtWatcherApplication)?.preferencesRepository?.setServiceEnabled(false)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
    }

    // Proceed to create notification and call ServiceCompat.startForeground safely...
    return START_STICKY
}
```

---

## 4. ViewModel State Contract (`DeviceViewModel.kt`)

```kotlin
/**
 * Invoked when user successfully grants permissions on onboarding.
 * Marks onboarding as completed and starts BluetoothWatcherService.
 */
fun completeOnboarding()

/**
 * Invoked when user chooses to skip onboarding permissions.
 * Marks onboarding as completed, ensures service is disabled in preferences,
 * and suppresses BluetoothWatcherService startup.
 */
fun skipOnboarding()
```

---

## 5. BroadcastReceiver Contract (`BootReceiver.kt`)

```kotlin
override fun onReceive(context: Context, intent: Intent) {
    // ...
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val isServiceEnabled = app.preferencesRepository.isServiceEnabledFlow.first()
            if (isServiceEnabled) {
                if (BluetoothHelper.hasRequiredPermissionsForService(context)) {
                    BluetoothWatcherService.startService(context)
                } else {
                    app.preferencesRepository.setServiceEnabled(false)
                }
            }
        } finally {
            pendingResult.finish()
        }
    }
}
```
