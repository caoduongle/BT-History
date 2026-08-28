package com.example.data.repository

import com.example.data.dao.DeviceDao
import com.example.data.dao.EventDao
import com.example.data.entity.DeviceEntity
import com.example.data.entity.EventEntity
import kotlinx.coroutines.flow.Flow

class DeviceRepository(
    private val deviceDao: DeviceDao,
    private val eventDao: EventDao
) {
    val allDevicesFlow: Flow<List<DeviceEntity>> = deviceDao.getAllDevicesFlow()
    val connectedDevicesFlow: Flow<List<DeviceEntity>> = deviceDao.getConnectedDevicesFlow()
    val connectedCountFlow: Flow<Int> = deviceDao.getConnectedCountFlow()
    val allEventsFlow: Flow<List<EventEntity>> = eventDao.getAllEventsFlow()

    fun getDeviceByIdFlow(id: Long): Flow<DeviceEntity?> = deviceDao.getDeviceByIdFlow(id)

    suspend fun getDeviceById(id: Long): DeviceEntity? = deviceDao.getDeviceById(id)

    suspend fun getDeviceByMac(macAddress: String): DeviceEntity? = deviceDao.getDeviceByMac(macAddress)

    fun getEventsByDeviceIdFlow(deviceId: Long): Flow<List<EventEntity>> = eventDao.getEventsByDeviceIdFlow(deviceId)

    suspend fun getLastDisconnectEvent(deviceId: Long): EventEntity? = eventDao.getLastDisconnectEvent(deviceId)

    suspend fun recordBluetoothEvent(
        name: String,
        macAddress: String,
        deviceType: String,
        eventType: String, // "CONNECT" or "DISCONNECT"
        timestamp: Long,
        latitude: Double?,
        longitude: Double?,
        accuracy: Float?,
        locationAddress: String?,
        isUnexpectedDisconnect: Boolean = false
    ): Pair<Long, Long> {
        val existingDevice = deviceDao.getDeviceByMac(macAddress)
        val isConnected = eventType.equals("CONNECT", ignoreCase = true)

        val deviceId: Long = if (existingDevice != null) {
            val updated = existingDevice.copy(
                name = if (name.isNotBlank() && name != "Thiết bị không rõ") name else existingDevice.name,
                deviceType = if (deviceType != "OTHER") deviceType else existingDevice.deviceType,
                isConnected = isConnected,
                lastEventTimestamp = timestamp,
                lastEventType = eventType,
                lastLatitude = latitude ?: existingDevice.lastLatitude,
                lastLongitude = longitude ?: existingDevice.lastLongitude,
                lastLocationAddress = locationAddress ?: existingDevice.lastLocationAddress
            )
            deviceDao.update(updated)
            existingDevice.id
        } else {
            val newDevice = DeviceEntity(
                name = name.ifBlank { "Thiết bị Bluetooth" },
                macAddress = macAddress,
                deviceType = deviceType,
                isConnected = isConnected,
                lastEventTimestamp = timestamp,
                lastEventType = eventType,
                lastLatitude = latitude,
                lastLongitude = longitude,
                lastLocationAddress = locationAddress
            )
            deviceDao.insertOrUpdate(newDevice)
        }

        val event = EventEntity(
            deviceId = deviceId,
            eventType = eventType,
            timestamp = timestamp,
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            locationAddress = locationAddress,
            isUnexpectedDisconnect = isUnexpectedDisconnect
        )
        val eventId = eventDao.insert(event)

        return Pair(deviceId, eventId)
    }

    suspend fun deleteDevice(device: DeviceEntity) {
        deviceDao.delete(device)
    }

    suspend fun deleteDeviceById(deviceId: Long) {
        deviceDao.deleteById(deviceId)
    }

    suspend fun clearAll() {
        eventDao.deleteAll()
        deviceDao.deleteAll()
    }
}
