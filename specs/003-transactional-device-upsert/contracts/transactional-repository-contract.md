# Component Contract: Transactional Event Recording

**Feature**: Transactional Device Event Upsert (`003-transactional-device-upsert`)  
**Status**: Completed  
**Date**: 2026-09-02  

---

## 1. `DeviceDao` Contract

```kotlin
@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices WHERE mac_address = :macAddress LIMIT 1")
    suspend fun getDeviceByMac(macAddress: String): DeviceEntity?

    /**
     * Inserts a new device entity using IGNORE strategy to prevent row deletion/replacement on collision.
     * Returns the newly generated row ID, or -1 if the insert was ignored due to constraint conflict.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(device: DeviceEntity): Long

    @Update
    suspend fun update(device: DeviceEntity)

    // Existing queries retained
    @Query("SELECT * FROM devices ORDER BY last_event_timestamp DESC")
    fun getAllDevicesFlow(): Flow<List<DeviceEntity>>

    @Query("DELETE FROM devices")
    suspend fun deleteAll()
}
```

---

## 2. `DeviceRepository` Contract

```kotlin
class DeviceRepository(
    private val database: AppDatabase?,
    private val deviceDao: DeviceDao,
    private val eventDao: EventDao
) {
    /** Secondary constructor for mock testing without full database instance */
    constructor(deviceDao: DeviceDao, eventDao: EventDao) : this(null, deviceDao, eventDao)

    /**
     * Atomically records a Bluetooth connection or disconnection event.
     * Wrapped in database.withTransaction to guarantee thread-safe read-and-write isolation.
     *
     * @return Pair of (deviceId, eventId)
     */
    suspend fun recordBluetoothEvent(
        name: String,
        macAddress: String,
        deviceType: String,
        eventType: String,
        timestamp: Long,
        latitude: Double?,
        longitude: Double?,
        accuracy: Float?,
        locationAddress: String?,
        isUnexpectedDisconnect: Boolean = false
    ): Pair<Long, Long>
}
```
