package com.example.service

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothA2dp
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.BtWatcherApplication
import com.example.receiver.BluetoothEventReceiver
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BluetoothWatcherService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var dynamicReceiver: BluetoothEventReceiver? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        registerBluetoothReceiver()
        observeConnectedDevices()
    }

    private fun registerBluetoothReceiver() {
        if (dynamicReceiver == null) {
            dynamicReceiver = BluetoothEventReceiver()
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(dynamicReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(dynamicReceiver, filter)
            }
        }
    }

    private fun observeConnectedDevices() {
        serviceScope.launch {
            val repository = (application as BtWatcherApplication).repository
            repository.connectedCountFlow.collect { count ->
                updateNotification(count)
            }
        }
    }

    private fun updateNotification(connectedCount: Int) {
        val notification = NotificationHelper.buildServiceNotification(
            this,
            connectedCount = connectedCount,
            lastEventText = if (connectedCount > 0) "Đang theo dõi $connectedCount thiết bị" else "Đang lắng nghe kết nối"
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NotificationHelper.SERVICE_NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SERVICE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = NotificationHelper.buildServiceNotification(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var foregroundServiceType = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            }
            ServiceCompat.startForeground(
                this,
                NotificationHelper.SERVICE_NOTIFICATION_ID,
                notification,
                foregroundServiceType
            )
        } else {
            startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        dynamicReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                // Ignore
            }
            dynamicReceiver = null
        }
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_SERVICE = "com.example.btwatcher.action.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.btwatcher.action.STOP_SERVICE"

        fun startService(context: Context) {
            val intent = Intent(context, BluetoothWatcherService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, BluetoothWatcherService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
