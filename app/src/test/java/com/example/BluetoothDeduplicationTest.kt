package com.example

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.dao.DeviceDao
import com.example.data.dao.EventDao
import com.example.data.entity.EventEntity
import com.example.receiver.BluetoothEventReceiver
import com.example.service.BluetoothWatcherService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BluetoothDeduplicationTest {

    private lateinit var app: BtWatcherApplication
    private lateinit var database: AppDatabase
    private lateinit var eventDao: EventDao
    private lateinit var deviceDao: DeviceDao

    @Before
    fun setUp() {
        runBlocking {
            app = ApplicationProvider.getApplicationContext<BtWatcherApplication>()
            database = app.database
            eventDao = database.eventDao()
            deviceDao = database.deviceDao()
            eventDao.deleteAll()
            deviceDao.deleteAll()
        }
    }

    /**
     * Xác nhận khi service đang chạy và nhận một sự kiện ACL_CONNECTED qua broadcast intent,
     * chỉ có đúng 1 EventEntity được lưu vào cơ sở dữ liệu (không phải 2).
     */
    @Test
    fun testAclConnected_createsExactlyOneEventEntityInDatabase() {
        runBlocking {
            // Khởi động service - service sẽ đăng ký động duy nhất một BluetoothEventReceiver trong onCreate()
            val serviceController = Robolectric.buildService(BluetoothWatcherService::class.java).create()

            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val testDevice = bluetoothAdapter.getRemoteDevice("11:22:33:44:55:66")

            val intent = Intent(BluetoothDevice.ACTION_ACL_CONNECTED).apply {
                putExtra(BluetoothDevice.EXTRA_DEVICE, testDevice)
            }

            // Phát broadcast sự kiện kết nối Bluetooth
            app.sendBroadcast(intent)

            // Cho phép Robolectric Main Looper xử lý hàng đợi broadcast
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

            // Chờ coroutine ghi DB trên Dispatchers.IO hoàn tất
            var events = emptyList<EventEntity>()
            val startTime = System.currentTimeMillis()
            while (events.isEmpty() && System.currentTimeMillis() - startTime < 5000) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                events = eventDao.getAllEventsFlow().first()
                if (events.isEmpty()) {
                    delay(50)
                }
            }

            // Kiểm tra đúng 1 EventEntity được tạo trong DB, không bị trùng lặp
            assertEquals("Một sự kiện ACL_CONNECTED chỉ được tạo đúng 1 EventEntity trong DB", 1, events.size)
            assertEquals("CONNECT", events[0].eventType)

            // Kiểm tra DeviceEntity tương ứng cũng được tạo/cập nhật chính xác
            val savedDevice = deviceDao.getDeviceByMac("11:22:33:44:55:66")
            assertNotNull(savedDevice)
            assertEquals(true, savedDevice?.isConnected)

            // Huỷ service đúng cách và đảm bảo không leak
            serviceController.destroy()
        }
    }

    /**
     * Xác nhận việc gọi trực tiếp onReceive của BluetoothEventReceiver với sự kiện ACL_CONNECTED
     * tạo đúng 1 EventEntity trong DB.
     */
    @Test
    fun testDirectReceiverAclConnected_createsExactlyOneEventEntity() {
        runBlocking {
            val receiver = BluetoothEventReceiver()
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val testDevice = bluetoothAdapter.getRemoteDevice("AA:BB:CC:DD:EE:FF")

            val intent = Intent(BluetoothDevice.ACTION_ACL_CONNECTED).apply {
                putExtra(BluetoothDevice.EXTRA_DEVICE, testDevice)
            }

            receiver.onReceive(app, intent)

            var events = emptyList<EventEntity>()
            val startTime = System.currentTimeMillis()
            while (events.isEmpty() && System.currentTimeMillis() - startTime < 5000) {
                events = eventDao.getAllEventsFlow().first()
                if (events.isEmpty()) {
                    delay(50)
                }
            }

            assertEquals("onReceive trực tiếp tạo đúng 1 EventEntity", 1, events.size)
            assertEquals("CONNECT", events[0].eventType)
        }
    }

    /**
     * Xác nhận lifecycle của BluetoothWatcherService đăng ký receiver trong onCreate
     * và huỷ đăng ký an toàn trong onDestroy mà không ném lỗi hoặc leak receiver.
     */
    @Test
    fun testServiceLifecycle_registersAndUnregistersSafely() {
        val serviceController = Robolectric.buildService(BluetoothWatcherService::class.java)
        serviceController.create()
        // onDestroy huỷ đăng ký an toàn, không ném ReceiverNotRegisteredException
        serviceController.destroy()
    }
}
