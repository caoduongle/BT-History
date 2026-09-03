package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.dao.DeviceDao
import com.example.data.dao.EventDao
import com.example.data.entity.DeviceEntity
import com.example.data.entity.EventEntity
import com.example.util.ExportHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RetentionAndExportTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var deviceDao: DeviceDao
    private lateinit var eventDao: EventDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        deviceDao = database.deviceDao()
        eventDao = database.eventDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testRetentionPruning_deletesOnlyExpiredEvents_preservesParentDevice() = runBlocking {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60 * 60 * 1000L

        val deviceId = deviceDao.insert(
            DeviceEntity(
                name = "Sony WH-1000XM5",
                macAddress = "11:22:33:44:55:66",
                deviceType = "HEADSET",
                isConnected = true,
                lastEventTimestamp = now,
                lastEventType = "CONNECT"
            )
        )

        // Insert 3 events: 200 days old, 90 days old, 1 hour old
        val oldEventId = eventDao.insert(
            EventEntity(
                deviceId = deviceId,
                eventType = "CONNECT",
                timestamp = now - (200 * dayMs)
            )
        )
        val validEventId1 = eventDao.insert(
            EventEntity(
                deviceId = deviceId,
                eventType = "DISCONNECT",
                timestamp = now - (90 * dayMs)
            )
        )
        val validEventId2 = eventDao.insert(
            EventEntity(
                deviceId = deviceId,
                eventType = "CONNECT",
                timestamp = now - 3600000L
            )
        )

        // Retention cutoff: 180 days ago
        val cutoff = now - (180 * dayMs)
        val deletedCount = eventDao.deleteEventsOlderThan(cutoff)

        assertEquals("Should delete exactly 1 expired event", 1, deletedCount)

        val remainingEvents = eventDao.getEventsByDeviceId(deviceId)
        assertEquals("Remaining events count should be 2", 2, remainingEvents.size)
        assertTrue("Remaining events must contain 90-day event", remainingEvents.any { it.id == validEventId1 })
        assertTrue("Remaining events must contain recent event", remainingEvents.any { it.id == validEventId2 })
        assertTrue("Expired event must not exist", remainingEvents.none { it.id == oldEventId })

        // Ensure parent device record was NEVER deleted
        val parentDevice = deviceDao.getDeviceById(deviceId)
        assertNotNull("Parent device entity must still exist", parentDevice)
        assertEquals("Parent device name must remain intact", "Sony WH-1000XM5", parentDevice?.name)
    }

    @Test
    fun testPagedEventQueries_returnsBoundedBatches() = runBlocking {
        val now = System.currentTimeMillis()
        val deviceId = deviceDao.insert(
            DeviceEntity(
                name = "Galaxy Buds Pro",
                macAddress = "AA:BB:CC:DD:EE:FF",
                deviceType = "HEADSET",
                isConnected = false,
                lastEventTimestamp = now,
                lastEventType = "DISCONNECT"
            )
        )

        // Insert 10 events with increasing timestamps
        for (i in 1..10) {
            eventDao.insert(
                EventEntity(
                    deviceId = deviceId,
                    eventType = if (i % 2 == 0) "CONNECT" else "DISCONNECT",
                    timestamp = now + (i * 1000L)
                )
            )
        }

        // Page 1: limit 4, offset 0 -> 4 newest items
        val page1 = eventDao.getEventsPaged(limit = 4, offset = 0)
        assertEquals(4, page1.size)
        assertEquals("Newest event first", now + 10000L, page1[0].timestamp)

        // Page 2: limit 4, offset 4 -> next 4 items
        val page2 = eventDao.getEventsPaged(limit = 4, offset = 4)
        assertEquals(4, page2.size)
        assertEquals(now + 6000L, page2[0].timestamp)

        // Page 3: limit 4, offset 8 -> remaining 2 items
        val page3 = eventDao.getEventsPaged(limit = 4, offset = 8)
        assertEquals(2, page3.size)
        assertEquals(now + 2000L, page3[0].timestamp)

        // Device-specific paged query
        val devicePage = eventDao.getEventsByDeviceIdPaged(deviceId = deviceId, limit = 5, offset = 0)
        assertEquals(5, devicePage.size)
    }

    @Test
    fun testExportToJson_generatesValidJsonFormat() {
        val devices = listOf(
            DeviceEntity(
                id = 1L,
                name = "Pixel Buds \"Pro\"",
                macAddress = "00:11:22:33:44:55",
                deviceType = "HEADSET",
                isConnected = true,
                lastEventTimestamp = 1756804500000L,
                lastEventType = "CONNECT",
                lastLatitude = 10.7769,
                lastLongitude = 106.7009,
                lastLocationAddress = "Quận 1, TP. HCM"
            )
        )
        val events = listOf(
            EventEntity(
                id = 10L,
                deviceId = 1L,
                eventType = "CONNECT",
                timestamp = 1756804500000L,
                latitude = 10.7769,
                longitude = 106.7009,
                accuracy = 12.5f,
                locationAddress = "Quận 1, TP. HCM",
                isUnexpectedDisconnect = false
            )
        )

        val baos = ByteArrayOutputStream()
        ExportHelper.exportToJson(baos, devices, events)
        val json = baos.toString("UTF-8")

        assertTrue("JSON must define version 1", json.contains("\"version\": 1"))
        assertTrue("JSON must contain device_count", json.contains("\"device_count\": 1"))
        assertTrue("JSON must contain event_count", json.contains("\"event_count\": 1"))
        assertTrue("JSON must contain escaped device name", json.contains("Pixel Buds \\\"Pro\\\""))
        assertTrue("JSON must contain MAC address", json.contains("00:11:22:33:44:55"))
        assertTrue("JSON must contain events list", json.contains("\"events\": ["))
    }

    @Test
    fun testExportToCsv_generatesRFC4180CompliantCsv() {
        val devices = listOf(
            DeviceEntity(
                id = 1L,
                name = "Apple Watch, Series 8",
                macAddress = "99:88:77:66:55:44",
                deviceType = "WATCH",
                isConnected = false,
                lastEventTimestamp = 1756804500000L,
                lastEventType = "DISCONNECT"
            )
        )
        val events = listOf(
            EventEntity(
                id = 20L,
                deviceId = 1L,
                eventType = "DISCONNECT",
                timestamp = 1756804500000L,
                locationAddress = "Hà Nội, Việt Nam",
                isUnexpectedDisconnect = true
            )
        )

        val baos = ByteArrayOutputStream()
        ExportHelper.exportToCsv(baos, devices, events)
        val csv = baos.toString("UTF-8")
        val lines = csv.lines()

        assertTrue("CSV must have at least 2 lines (header + row)", lines.size >= 2)
        assertEquals(
            "CSV header must match contract",
            "device_name,mac_address,device_type,event_type,timestamp,date_time,latitude,longitude,accuracy,location_address,is_unexpected_disconnect",
            lines[0]
        )
        // Check escaping of comma in device name: "Apple Watch, Series 8"
        assertTrue("Name with comma must be quoted", lines[1].contains("\"Apple Watch, Series 8\""))
        assertTrue("MAC must match", lines[1].contains("99:88:77:66:55:44"))
        assertTrue("Address with comma must be quoted", lines[1].contains("\"Hà Nội, Việt Nam\""))
        assertTrue("isUnexpectedDisconnect must be true", lines[1].endsWith("true"))
    }
}
