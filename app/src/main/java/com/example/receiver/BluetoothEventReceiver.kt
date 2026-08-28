package com.example.receiver

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.BtWatcherApplication
import com.example.util.BluetoothHelper
import com.example.util.LocationHelper
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BluetoothEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleBluetoothAction(context, intent, action)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleBluetoothAction(context: Context, intent: Intent, action: String) {
        val app = context.applicationContext as? BtWatcherApplication ?: return
        val repository = app.repository
        val preferencesRepository = app.preferencesRepository

        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

        var eventType: String? = null
        var isUnexpectedDisconnect = false

        when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                eventType = "CONNECT"
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                eventType = "DISCONNECT"
                isUnexpectedDisconnect = true
            }
            "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED",
            "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED" -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                val prevState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1)
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    eventType = "CONNECT"
                } else if (state == BluetoothProfile.STATE_DISCONNECTED && prevState == BluetoothProfile.STATE_CONNECTED) {
                    eventType = "DISCONNECT"
                    isUnexpectedDisconnect = true
                }
            }
        }

        if (device != null && eventType != null) {
            val macAddress = device.address ?: return
            val deviceName = BluetoothHelper.getSafeDeviceName(context, device)
            val deviceType = BluetoothHelper.determineDeviceType(context, device)
            val timestamp = System.currentTimeMillis()

            // Fetch high-accuracy location at the moment event triggered
            val locationResult = LocationHelper.getCurrentLocation(context)

            val (deviceId, _) = repository.recordBluetoothEvent(
                name = deviceName,
                macAddress = macAddress,
                deviceType = deviceType,
                eventType = eventType,
                timestamp = timestamp,
                latitude = locationResult.latitude,
                longitude = locationResult.longitude,
                accuracy = locationResult.accuracy,
                locationAddress = locationResult.address,
                isUnexpectedDisconnect = isUnexpectedDisconnect
            )

            // Check if disconnect alert is enabled in settings
            if (eventType == "DISCONNECT" && isUnexpectedDisconnect) {
                val isAlertEnabled = preferencesRepository.isDisconnectAlertEnabledFlow.first()
                if (isAlertEnabled) {
                    NotificationHelper.showDisconnectAlert(
                        context = context,
                        deviceId = deviceId,
                        deviceName = deviceName,
                        latitude = locationResult.latitude,
                        longitude = locationResult.longitude,
                        address = locationResult.address
                    )
                }
            }
        }
    }
}
