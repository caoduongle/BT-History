package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.entity.DeviceEntity
import com.example.data.entity.EventEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseMigrationFrameworkTest {

    private lateinit var app: BtWatcherApplication
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext<BtWatcherApplication>()
        database = AppDatabase.getDatabase(app)
    }

    @Test
    fun testSchemaExportArtifact_existsAndDeclaresVersionOne() {
        val schemaFile = java.io.File("app/schemas/com.example.data.AppDatabase/1.json").takeIf { it.exists() }
            ?: java.io.File("schemas/com.example.data.AppDatabase/1.json").takeIf { it.exists() }
        assertNotNull("1.json schema artifact must exist on disk", schemaFile)
        val content = schemaFile!!.readText()
        assertTrue("Schema must define version 1", content.contains("\"version\": 1"))
        assertTrue("Schema must define devices table", content.contains("\"tableName\": \"devices\""))
        assertTrue("Schema must define events table", content.contains("\"tableName\": \"events\""))
    }

    @Test
    fun testAllMigrationsRegistry_isDefinedAndAccessible() {
        assertNotNull("ALL_MIGRATIONS array must not be null", AppDatabase.ALL_MIGRATIONS)
        // At version 1, ALL_MIGRATIONS is initially empty
        assertEquals("Version 1 should start with 0 registered migrations", 0, AppDatabase.ALL_MIGRATIONS.size)
    }

    @Test
    fun testDatabaseOperations_succeedWithoutDestructiveMigration() = runBlocking {
        val deviceDao = database.deviceDao()
        val eventDao = database.eventDao()

        val testMac = "AA:BB:CC:DD:EE:01"
        val testDevice = DeviceEntity(
            name = "Test Safe Headset",
            macAddress = testMac,
            deviceType = "HEADSET",
            isConnected = true,
            lastEventTimestamp = System.currentTimeMillis(),
            lastEventType = "CONNECT",
            lastLatitude = 10.7769,
            lastLongitude = 106.7009
        )

        val deviceId = deviceDao.insert(testDevice)
        assertTrue("Inserted device ID must be > 0", deviceId > 0)

        val retrievedDevice = deviceDao.getDeviceByMac(testMac)
        assertNotNull(retrievedDevice)
        assertEquals("Test Safe Headset", retrievedDevice?.name)

        val testEvent = EventEntity(
            deviceId = deviceId,
            eventType = "CONNECT",
            timestamp = System.currentTimeMillis(),
            latitude = 10.7769,
            longitude = 106.7009,
            accuracy = 10f,
            isUnexpectedDisconnect = false
        )

        val eventId = eventDao.insert(testEvent)
        assertTrue("Inserted event ID must be > 0", eventId > 0)

        val events = eventDao.getEventsByDeviceId(deviceId)
        assertEquals(1, events.size)
        assertEquals("CONNECT", events[0].eventType)

        // Cleanup
        eventDao.deleteEventsByDeviceId(deviceId)
        deviceDao.delete(retrievedDevice!!)
    }
}
