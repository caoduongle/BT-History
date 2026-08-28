package com.example.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "devices",
    indices = [Index(value = ["mac_address"], unique = true)]
)
data class DeviceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "mac_address")
    val macAddress: String,

    @ColumnInfo(name = "device_type")
    val deviceType: String = "OTHER", // HEADSET, SPEAKER, WATCH, CAR, PHONE, COMPUTER, OTHER

    @ColumnInfo(name = "is_connected")
    val isConnected: Boolean = false,

    @ColumnInfo(name = "last_event_timestamp")
    val lastEventTimestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_event_type")
    val lastEventType: String = "DISCONNECT",

    @ColumnInfo(name = "last_latitude")
    val lastLatitude: Double? = null,

    @ColumnInfo(name = "last_longitude")
    val lastLongitude: Double? = null,

    @ColumnInfo(name = "last_location_address")
    val lastLocationAddress: String? = null
)
