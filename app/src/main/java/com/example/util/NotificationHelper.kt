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
                context.getString(R.string.notif_channel_service_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_service_desc)
                setShowBadge(false)
            }

            // Channel for disconnect alerts
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                context.getString(R.string.notif_channel_alert_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_alert_desc)
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
        lastEventText: String? = null
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

        val resolvedLastEvent = lastEventText ?: context.getString(R.string.notif_service_default_last_event)
        val statusText = if (connectedCount > 0) {
            context.getString(R.string.notif_service_status_connected, connectedCount, resolvedLastEvent)
        } else {
            context.getString(R.string.notif_service_status_monitoring, resolvedLastEvent)
        }

        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notif_service_active_title))
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
            else -> context.getString(R.string.location_unknown_coordinates)
        }

        val builder = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notif_alert_title))
            .setContentText(context.getString(R.string.notif_alert_content, deviceName, locationText))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.notif_alert_big_text, deviceName, locationText, TimeFormatter.formatFullDateTime(System.currentTimeMillis())))
            )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(appPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Add "Xem trên bản đồ" Action button if coordinates exist
        if (latitude != null && longitude != null) {
            val mapLabel = context.getString(R.string.notif_map_marker_label, deviceName)
            val mapUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(mapLabel)})")
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
                context.getString(R.string.notif_action_view_on_map),
                mapPendingIntent
            )
        }

        notificationManager.notify((DISCONNECT_ALERT_NOTIFICATION_ID_BASE + deviceId).toInt(), builder.build())
    }
}
