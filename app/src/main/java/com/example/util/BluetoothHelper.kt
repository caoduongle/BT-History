package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object BluetoothHelper {

    const val DEFAULT_DEVICE_NAME = "Thiết bị Bluetooth"
    const val UNKNOWN_DEVICE_NAME = "Thiết bị không rõ"

    fun hasBluetoothConnectPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun getSafeDeviceName(context: Context, device: BluetoothDevice?): String {
        if (device == null) return context.getString(com.example.R.string.device_default_name)
        return try {
            if (hasBluetoothConnectPermission(context)) {
                device.name ?: device.alias ?: context.getString(com.example.R.string.device_name_with_mac_suffix, device.address.takeLast(5))
            } else {
                context.getString(com.example.R.string.device_name_with_mac_suffix, device.address.takeLast(5))
            }
        } catch (e: Exception) {
            context.getString(com.example.R.string.device_default_name)
        }
    }

    @SuppressLint("MissingPermission")
    fun determineDeviceType(context: Context, device: BluetoothDevice?): String {
        if (device == null) return "OTHER"
        return try {
            val btClass = device.bluetoothClass ?: return "OTHER"
            when (btClass.majorDeviceClass) {
                BluetoothClass.Device.Major.AUDIO_VIDEO -> {
                    when (btClass.deviceClass) {
                        BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES,
                        BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET -> "HEADSET"
                        BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER -> "SPEAKER"
                        BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO -> "CAR"
                        else -> "HEADSET"
                    }
                }
                BluetoothClass.Device.Major.WEARABLE -> "WATCH"
                BluetoothClass.Device.Major.PHONE -> "PHONE"
                BluetoothClass.Device.Major.COMPUTER -> "COMPUTER"
                else -> {
                    val name = getSafeDeviceName(context, device).lowercase()
                    when {
                        name.contains("airpod") || name.contains("buds") || name.contains("headset") ||
                                name.contains("tai nghe") || name.contains("wh-") || name.contains("wf-") -> "HEADSET"
                        name.contains("speaker") || name.contains("loa") || name.contains("flip") ||
                                name.contains("charge") || name.contains("boom") -> "SPEAKER"
                        name.contains("watch") || name.contains("band") || name.contains("đồng hồ") ||
                                name.contains("fitbit") || name.contains("garmin") -> "WATCH"
                        name.contains("car") || name.contains("oto") || name.contains("ô tô") ||
                                name.contains("auto") -> "CAR"
                        else -> "OTHER"
                    }
                }
            }
        } catch (e: Exception) {
            "OTHER"
        }
    }
}
