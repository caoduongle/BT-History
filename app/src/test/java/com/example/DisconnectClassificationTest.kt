package com.example

import android.Manifest
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.dao.DeviceDao
import com.example.data.dao.EventDao
import com.example.data.entity.EventEntity
import com.example.receiver.BluetoothEventReceiver
import com.example.service.BluetoothWatcherService
import com.example.util.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DisconnectClassificationTest {

    private lateinit var app: BtWatcherApplication
    private lateinit var database: AppDatabase
    private lateinit var eventDao: EventDao
    private lateinit var deviceDao: DeviceDao
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext<BtWatcherApplication>()
        database = app.database
        eventDao = database.eventDao()
        deviceDao = database.deviceDao()
        notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Cấp quyền thông báo để NotificationHelper có thể hiển thị cảnh báo
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationHelper.createNotificationChannels(app)

        // Xoá DB và thông báo cũ, reset trạng thái adapter
        runBlocking {
            eventDao.deleteAll()
            deviceDao.deleteAll()
        }
        notificationManager.cancelAll()
        BluetoothEventReceiver.resetAdapterStateForTesting()
    }

    /**
     * Trường hợp (a): Người dùng tắt Bluetooth thủ công (STATE_TURNING_OFF hoặc STATE_OFF)
     * rồi ngắt kết nối thiết bị -> Coi là NGẮT CHỦ ĐỘNG, không cảnh báo.
     */
    @Test
    fun testManualBluetoothOff_classifiedAsIntentionalAndDoesNotAlert() {
        runBlocking {
            // Khởi động service để nhận broadcast
            val serviceController = Robolectric.buildService(BluetoothWatcherService::class.java).create()

            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val testDevice = bluetoothAdapter.getRemoteDevice("22:33:44:55:66:77")

            // 1. Giả lập hệ thống phát ACTION_STATE_CHANGED khi người dùng tắt Bluetooth
            val stateChangedIntent = Intent(BluetoothAdapter.ACTION_STATE_CHANGED).apply {
                putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_TURNING_OFF)
            }
            app.sendBroadcast(stateChangedIntent)
            shadowOf(Looper.getMainLooper()).idle()

            // Kiểm tra timestamp adapter off đã được ghi nhận ngay lập tức
            assertTrue("lastAdapterOffTimestamp phải được cập nhật > 0", BluetoothEventReceiver.lastAdapterOffTimestamp > 0L)
            assertTrue("isRecentAdapterOff phải trả về true", BluetoothEventReceiver.isRecentAdapterOff())

            // 2. Ngay sau đó (trong ngưỡng heuristic 4000ms), thiết bị bị ngắt kết nối
            val disconnectIntent = Intent(BluetoothDevice.ACTION_ACL_DISCONNECTED).apply {
                putExtra(BluetoothDevice.EXTRA_DEVICE, testDevice)
            }
            app.sendBroadcast(disconnectIntent)
            shadowOf(Looper.getMainLooper()).idle()

            // Chờ Room DB lưu sự kiện
            var events = emptyList<EventEntity>()
            val startTime = System.currentTimeMillis()
            while (events.isEmpty() && System.currentTimeMillis() - startTime < 5000) {
                shadowOf(Looper.getMainLooper()).idle()
                events = eventDao.getAllEventsFlow().first()
                if (events.isEmpty()) {
                    delay(50)
                }
            }

            assertEquals("Phải có đúng 1 EventEntity được lưu", 1, events.size)
            assertEquals("DISCONNECT", events[0].eventType)
            // Xác nhận isUnexpectedDisconnect = false (ngắt chủ động)
            assertFalse("isUnexpectedDisconnect phải là false khi người dùng chủ động tắt Bluetooth", events[0].isUnexpectedDisconnect)

            // Cho thêm thời gian để xác nhận KHÔNG có thông báo cảnh báo nào được gửi
            delay(200)
            shadowOf(Looper.getMainLooper()).idle()
            val shadowNm = shadowOf(notificationManager)
            val alertNotifications = shadowNm.allNotifications.filter {
                it.channelId == NotificationHelper.ALERT_CHANNEL_ID
            }
            assertEquals("Không được hiển thị thông báo cảnh báo khi ngắt chủ động", 0, alertNotifications.size)

            serviceController.destroy()
        }
    }

    /**
     * Trường hợp (b): Thiết bị rớt kết nối đột ngột trong khi Bluetooth vẫn bật
     * (không có ACTION_STATE_CHANGED báo tắt adapter trước đó) -> Coi là BẤT NGỜ, CÓ CẢNH BÁO.
     */
    @Test
    fun testSuddenDisconnect_classifiedAsUnexpectedAndTriggersAlert() {
        runBlocking {
            // Khởi động service
            val serviceController = Robolectric.buildService(BluetoothWatcherService::class.java).create()

            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val testDevice = bluetoothAdapter.getRemoteDevice("88:99:AA:BB:CC:DD")

            // Đảm bảo không có tín hiệu tắt adapter
            BluetoothEventReceiver.resetAdapterStateForTesting()
            assertFalse("isRecentAdapterOff phải là false khi Bluetooth vẫn bật", BluetoothEventReceiver.isRecentAdapterOff())

            // Phát sự kiện ngắt kết nối đột ngột
            val disconnectIntent = Intent(BluetoothDevice.ACTION_ACL_DISCONNECTED).apply {
                putExtra(BluetoothDevice.EXTRA_DEVICE, testDevice)
            }
            app.sendBroadcast(disconnectIntent)
            shadowOf(Looper.getMainLooper()).idle()

            // Chờ Room DB lưu sự kiện
            var events = emptyList<EventEntity>()
            val startTime = System.currentTimeMillis()
            while (events.isEmpty() && System.currentTimeMillis() - startTime < 5000) {
                shadowOf(Looper.getMainLooper()).idle()
                events = eventDao.getAllEventsFlow().first()
                if (events.isEmpty()) {
                    delay(50)
                }
            }

            assertEquals("Phải có đúng 1 EventEntity được lưu", 1, events.size)
            assertEquals("DISCONNECT", events[0].eventType)
            // Xác nhận isUnexpectedDisconnect = true (rớt kết nối đột ngột)
            assertTrue("isUnexpectedDisconnect phải là true khi thiết bị rớt kết nối đột ngột", events[0].isUnexpectedDisconnect)

            // Chờ thông báo cảnh báo ngắt kết nối được tạo và hiển thị
            val shadowNm = shadowOf(notificationManager)
            val notifStartTime = System.currentTimeMillis()
            while (shadowNm.allNotifications.none { it.channelId == NotificationHelper.ALERT_CHANNEL_ID }
                && System.currentTimeMillis() - notifStartTime < 5000
            ) {
                shadowOf(Looper.getMainLooper()).idle()
                delay(50)
            }

            val alertNotifications = shadowNm.allNotifications.filter {
                it.channelId == NotificationHelper.ALERT_CHANNEL_ID
            }
            assertEquals("Phải hiển thị đúng 1 thông báo cảnh báo khi ngắt đột ngột", 1, alertNotifications.size)

            serviceController.destroy()
        }
    }

    /**
     * Xác nhận khi Bluetooth được bật lại (STATE_ON), timestamp adapter tắt được reset về 0,
     * đảm bảo các sự kiện ngắt kết nối sau đó không bị nhầm là chủ động.
     */
    @Test
    fun testAdapterTurnedOn_resetsAdapterOffTimestamp() {
        runBlocking {
            val serviceController = Robolectric.buildService(BluetoothWatcherService::class.java).create()

            // Đặt giả lập adapter vừa tắt
            BluetoothEventReceiver.lastAdapterOffTimestamp = System.currentTimeMillis()
            assertTrue(BluetoothEventReceiver.isRecentAdapterOff())

            // Phát tín hiệu Bluetooth bật lại
            val stateOnIntent = Intent(BluetoothAdapter.ACTION_STATE_CHANGED).apply {
                putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON)
            }
            app.sendBroadcast(stateOnIntent)
            shadowOf(Looper.getMainLooper()).idle()

            // Xác nhận timestamp đã được reset về 0L
            assertEquals(0L, BluetoothEventReceiver.lastAdapterOffTimestamp)
            assertFalse(BluetoothEventReceiver.isRecentAdapterOff())

            serviceController.destroy()
        }
    }
}
