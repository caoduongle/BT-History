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

        // Xử lý sự kiện thay đổi trạng thái BluetoothAdapter đồng bộ ngay khi nhận broadcast
        if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            if (state == BluetoothAdapter.STATE_TURNING_OFF || state == BluetoothAdapter.STATE_OFF) {
                lastAdapterOffTimestamp = System.currentTimeMillis()
            } else if (state == BluetoothAdapter.STATE_ON || state == BluetoothAdapter.STATE_TURNING_ON) {
                lastAdapterOffTimestamp = 0L
            }
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleBluetoothAction(context, intent, action)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult?.finish()
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
                isUnexpectedDisconnect = !isRecentAdapterOff()
            }
            "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED",
            "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED" -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                val prevState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1)
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    eventType = "CONNECT"
                } else if (state == BluetoothProfile.STATE_DISCONNECTED && prevState == BluetoothProfile.STATE_CONNECTED) {
                    eventType = "DISCONNECT"
                    isUnexpectedDisconnect = !isRecentAdapterOff()
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

    companion object {
        /**
         * Ngưỡng thời gian (milliseconds) giữa thời điểm BluetoothAdapter bắt đầu tắt
         * (STATE_TURNING_OFF hoặc STATE_OFF) và thời điểm nhận sự kiện ngắt kết nối thiết bị.
         *
         * Heuristic:
         * 1. Khi người dùng chủ động tắt Bluetooth trên điện thoại (qua Quick Settings hoặc Cài đặt),
         *    Android sẽ phát ACTION_STATE_CHANGED (STATE_TURNING_OFF/STATE_OFF) ngay trước khi
         *    các kết nối Bluetooth ngoại vi bị ngắt.
         * 2. Nếu sự kiện ngắt kết nối (ACL_DISCONNECTED hoặc profile DISCONNECTED) diễn ra trong
         *    ngưỡng thời gian này (mặc định 4000ms), sự kiện được coi là NGẮT KẾT NỐI CHỦ ĐỘNG
         *    (isUnexpectedDisconnect = false) và không hiển thị thông báo cảnh báo để tránh false alarms.
         * 3. Nếu không có tín hiệu tắt adapter trong khoảng thời gian này (Bluetooth vẫn đang bật bình thường),
         *    sự kiện được coi là rớt kết nối bất thường (isUnexpectedDisconnect = true) và cảnh báo tới người dùng.
         *
         * Giới hạn và đánh đổi (Trade-offs):
         * - Heuristic này có thể chưa hoàn hảo 100%: nếu thiết bị ngoại vi rớt kết nối chỉ 1-2 giây ngay trước
         *   khi người dùng tắt Bluetooth, nó có thể bị coi là chủ động. Tuy nhiên đây là trường hợp cực kỳ hiếm,
         *   và khi người dùng đang chủ động tắt Bluetooth thì việc bỏ qua cảnh báo là phù hợp.
         * - Ngưỡng 4000ms là khoảng thời gian cân bằng tối ưu: đủ dài để bao trọn độ trễ hàng đợi IPC/broadcast
         *   trên các OEM Android khác nhau khi tắt radio, nhưng đủ ngắn để không bỏ sót các sự kiện ngắt kết nối
         *   thực sự xảy ra sau đó.
         */
        const val ADAPTER_OFF_HEURISTIC_WINDOW_MS = 4000L

        @Volatile
        var lastAdapterOffTimestamp: Long = 0L
            internal set

        /**
         * Kiểm tra xem BluetoothAdapter có vừa mới chuyển sang trạng thái tắt gần đây (trong vòng heuristic window) hay không.
         */
        fun isRecentAdapterOff(currentTime: Long = System.currentTimeMillis()): Boolean {
            val lastOff = lastAdapterOffTimestamp
            return lastOff > 0L && (currentTime - lastOff) in 0L..ADAPTER_OFF_HEURISTIC_WINDOW_MS
        }

        /**
         * Đặt lại trạng thái adapter để đảm bảo tính độc lập giữa các ca test.
         */
        fun resetAdapterStateForTesting() {
            lastAdapterOffTimestamp = 0L
        }
    }
}
