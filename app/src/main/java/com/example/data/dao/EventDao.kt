package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("SELECT * FROM events WHERE device_id = :deviceId ORDER BY timestamp DESC")
    fun getEventsByDeviceIdFlow(deviceId: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE device_id = :deviceId ORDER BY timestamp DESC")
    suspend fun getEventsByDeviceId(deviceId: Long): List<EventEntity>

    @Query("SELECT * FROM events WHERE device_id = :deviceId AND event_type = 'DISCONNECT' ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastDisconnectEvent(deviceId: Long): EventEntity?

    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    fun getAllEventsFlow(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getEventsSinceFlow(sinceTimestamp: Long): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity): Long

    @Delete
    suspend fun delete(event: EventEntity)

    @Query("DELETE FROM events WHERE device_id = :deviceId")
    suspend fun deleteEventsByDeviceId(deviceId: Long)

    @Query("DELETE FROM events")
    suspend fun deleteAll()
}
