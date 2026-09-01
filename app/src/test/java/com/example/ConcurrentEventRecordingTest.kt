package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.dao.DeviceDao
import com.example.data.dao.EventDao
import com.example.data.entity.DeviceEntity
import com.example.data.repository.DeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConcurrentEventRecordingTest {

    private lateinit var app: BtWatcherApplication
    private lateinit var database: AppDatabase
    private lateinit var deviceDao: DeviceDao
    private lateinit var eventDao: EventDao
    private lateinit var repository: DeviceRepository

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext<BtWatcherApplication>()
        database = app.database
        deviceDao = database.deviceDao()
        eventDao = database.eventDao()
        repository = app.repository

        runBlocking {
            eventDao.deleteAll()
            deviceDao.deleteAll()
        }
    }

    /**
     * Yêu cầu 3: Giả lập gọi recordBluetoothEvent đồng thời (nhiều coroutine) cho cùng một MAC address
     * và xác nhận chỉ có đúng 1 DeviceEntity được tạo ra.
     */
    @Test
    fun testConcurrentRecordBluetoothEvent_createsExactlyOneDeviceEntity() {
        runBlocking {
            val targetMac = "11:22:33:44:55:66"
            val coroutineCount = 10

            // Khởi chạy 10 coroutine chạy đua trên Dispatchers.IO cùng gọi recordBluetoothEvent
            val deferreds = (1..coroutineCount).map { index ->
                async(Dispatchers.IO) {
                    repository.recordBluetoothEvent(
                        name = "Headphones $index",
                        macAddress = targetMac,
                        deviceType = "AUDIO",
                        eventType = if (index % 2 == 0) "CONNECT" else "DISCONNECT",
                        timestamp = System.currentTimeMillis() + index,
                        latitude = 10.762622 + (index * 0.0001),
                        longitude = 106.660172 + (index * 0.0001),
                        accuracy = 10.0f,
                        locationAddress = "Địa chỉ $index",
                        isUnexpectedDisconnect = false
                    )
                }
            }

            // Chờ toàn bộ các coroutine hoàn thành
            val results = deferreds.awaitAll()

            // 1. Xác nhận trong cơ sở dữ liệu chỉ có DUY NHẤT 1 DeviceEntity
            val devices = deviceDao.getAllDevicesFlow().first()
            assertEquals("Chỉ được tạo đúng 1 DeviceEntity duy nhất cho cùng MAC address", 1, devices.size)
            assertEquals(targetMac, devices[0].macAddress)

            val singleDeviceId = devices[0].id

            // 2. Xác nhận tất cả các coroutine đều trả về cùng một deviceId hợp lệ
            val uniqueReturnedDeviceIds = results.map { it.first }.toSet()
            assertEquals("Tất cả coroutine phải trả về cùng 1 deviceId", 1, uniqueReturnedDeviceIds.size)
            assertEquals(singleDeviceId, uniqueReturnedDeviceIds.first())

            // 3. Xác nhận toàn bộ 10 sự kiện đều được lưu và liên kết với đúng deviceId đó (Foreign Key Integrity)
            val events = eventDao.getAllEventsFlow().first()
            assertEquals("Toàn bộ 10 sự kiện phải được lưu thành công", coroutineCount, events.size)
            assertTrue("Tất cả sự kiện phải liên kết tới deviceId duy nhất", events.all { it.deviceId == singleDeviceId })
        }
    }

    /**
     * Xác nhận khi thiết bị đã tồn tại trong DB, các lệnh gọi đồng thời tiếp theo
     * cập nhật trạng thái thiết bị một cách nguyên tử mà không xảy ra xung đột hay tạo trùng bản ghi.
     */
    @Test
    fun testConcurrentRecordBluetoothEvent_withExistingDevice_updatesAtomically() {
        runBlocking {
            val targetMac = "AA:BB:CC:DD:EE:FF"

            // Tạo trước 1 thiết bị trong DB
            val initialPair = repository.recordBluetoothEvent(
                name = "Initial Device",
                macAddress = targetMac,
                deviceType = "WATCH",
                eventType = "CONNECT",
                timestamp = System.currentTimeMillis(),
                latitude = 10.0,
                longitude = 106.0,
                accuracy = 5.0f,
                locationAddress = "Initial Address"
            )

            val coroutineCount = 8
            val deferreds = (1..coroutineCount).map { index ->
                async(Dispatchers.IO) {
                    repository.recordBluetoothEvent(
                        name = "Updated Device $index",
                        macAddress = targetMac,
                        deviceType = "WATCH",
                        eventType = "CONNECT",
                        timestamp = System.currentTimeMillis() + (index * 100),
                        latitude = 10.0 + index,
                        longitude = 106.0 + index,
                        accuracy = 5.0f,
                        locationAddress = "Updated Address $index"
                    )
                }
            }

            val results = deferreds.awaitAll()

            // Vẫn chỉ có đúng 1 DeviceEntity trong DB
            val devices = deviceDao.getAllDevicesFlow().first()
            assertEquals(1, devices.size)
            assertEquals(initialPair.first, devices[0].id)

            // Toàn bộ 8 coroutine trả về cùng initialPair.first
            assertTrue(results.all { it.first == initialPair.first })

            // Tổng cộng 1 sự kiện ban đầu + 8 sự kiện mới = 9 sự kiện
            val events = eventDao.getAllEventsFlow().first()
            assertEquals(1 + coroutineCount, events.size)
            assertTrue(events.all { it.deviceId == initialPair.first })
        }
    }
}
