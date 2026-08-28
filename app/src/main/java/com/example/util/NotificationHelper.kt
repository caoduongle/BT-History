package com.example.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val SERVICE_CHANNEL_ID = "bt_watcher_service_channel"
    const val ALERT_CHANNEL_ID = "bt_watcher_alert_channel"

    const val SERVICE_NOTIFICATION_ID = 1001
    const val DISCONNECT_ALERT_NOTIFICATION_ID_BASE = 2000

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel for sticky foreground service
            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Dịch vụ giám sát BT Watcher",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Thông báo dịch vụ nền giám sát kết nối Bluetooth và vị trí GPS"
                setShowBadge(false)
            }

            // Channel for disconnect alerts
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Cảnh báo ngắt kết nối thiết bị",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Cảnh báo ngay khi thiết bị Bluetooth bị mất kết nối kèm vị trí GPS"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    fun buildServiceNotification(
        context: Context,
        connectedCount: Int = 0,
        lastEventText: String = "Đang sẵn sàng ghi nhận sự kiện"
    ): Notification {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (connectedCount > 0) {
            "Đang kết nối: $connectedCount thiết bị • $lastEventText"
        } else {
            "Đang giám sát nền • $lastEventText"
        }

        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle("BT Watcher đang hoạt động")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    fun showDisconnectAlert(
        context: Context,
        deviceId: Long,
        deviceName: String,
        latitude: Double?,
        longitude: Double?,
        address: String?
    ) {
        if (!BluetoothHelper.hasNotificationPermission(context)) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to open MainActivity
        val appIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("EXTRA_DEVICE_ID", deviceId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            deviceId.toInt(),
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val locationText = when {
            !address.isNullOrBlank() -> address
            latitude != null && longitude != null -> TimeFormatter.formatCoordinates(latitude, longitude)
            else -> "Chưa xác định tọa độ GPS"
        }

        val builder = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setContentTitle("⚠️ Thiết bị vừa ngắt kết nối!")
            .setContentText("$deviceName đã ngắt kết nối tại: $locationText")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Thiết bị \"$deviceName\" đã bị ngắt kết nối.\n📍 Vị trí ghi nhận: $locationText\n⏰ Thời gian: ${TimeFormatter.formatFullDateTime(System.currentTimeMillis())}")
            )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(appPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Add "Xem trên bản đồ" Action button if coordinates exist
        if (latitude != null && longitude != null) {
            val mapUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode("Vị trí ngắt kết nối $deviceName")})")
            val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val mapPendingIntent = PendingIntent.getActivity(
                context,
                (deviceId + 5000).toInt(),
                mapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_dialog_map,
                "🗺️ Xem vị trí trên bản đồ",
                mapPendingIntent
            )
        }

        notificationManager.notify((DISCONNECT_ALERT_NOTIFICATION_ID_BASE + deviceId).toInt(), builder.build())
    }
}
