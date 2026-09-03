package com.example.util

import com.example.data.entity.DeviceEntity
import com.example.data.entity.EventEntity
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object ExportHelper {

    fun escapeJson(text: String?): String {
        if (text == null) return "null"
        val sb = StringBuilder("\"")
        for (c in text) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", c.code))
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        sb.append("\"")
        return sb.toString()
    }

    fun escapeCsv(text: String?): String {
        if (text == null) return ""
        val needsQuotes = text.contains(',') || text.contains('"') || text.contains('\n') || text.contains('\r')
        return if (needsQuotes) {
            "\"${text.replace("\"", "\"\"")}\""
        } else {
            text
        }
    }

    fun exportToJson(
        outputStream: OutputStream,
        devices: List<DeviceEntity>,
        events: List<EventEntity>
    ) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))
        writer.write("{\n")
        writer.write("  \"version\": 1,\n")
        writer.write("  \"exported_at\": ${System.currentTimeMillis()},\n")
        writer.write("  \"device_count\": ${devices.size},\n")
        writer.write("  \"event_count\": ${events.size},\n")

        // 1. Devices Array
        writer.write("  \"devices\": [\n")
        devices.forEachIndexed { index, device ->
            writer.write("    {\n")
            writer.write("      \"id\": ${device.id},\n")
            writer.write("      \"name\": ${escapeJson(device.name)},\n")
            writer.write("      \"mac_address\": ${escapeJson(device.macAddress)},\n")
            writer.write("      \"device_type\": ${escapeJson(device.deviceType)},\n")
            writer.write("      \"is_connected\": ${device.isConnected},\n")
            writer.write("      \"last_event_timestamp\": ${device.lastEventTimestamp},\n")
            writer.write("      \"last_event_type\": ${escapeJson(device.lastEventType)},\n")
            writer.write("      \"last_latitude\": ${device.lastLatitude ?: "null"},\n")
            writer.write("      \"last_longitude\": ${device.lastLongitude ?: "null"},\n")
            writer.write("      \"last_location_address\": ${escapeJson(device.lastLocationAddress)}\n")
            if (index < devices.size - 1) {
                writer.write("    },\n")
            } else {
                writer.write("    }\n")
            }
        }
        writer.write("  ],\n")

        // 2. Events Array
        writer.write("  \"events\": [\n")
        events.forEachIndexed { index, event ->
            writer.write("    {\n")
            writer.write("      \"id\": ${event.id},\n")
            writer.write("      \"device_id\": ${event.deviceId},\n")
            writer.write("      \"event_type\": ${escapeJson(event.eventType)},\n")
            writer.write("      \"timestamp\": ${event.timestamp},\n")
            writer.write("      \"latitude\": ${event.latitude ?: "null"},\n")
            writer.write("      \"longitude\": ${event.longitude ?: "null"},\n")
            writer.write("      \"accuracy\": ${event.accuracy ?: "null"},\n")
            writer.write("      \"location_address\": ${escapeJson(event.locationAddress)},\n")
            writer.write("      \"is_unexpected_disconnect\": ${event.isUnexpectedDisconnect}\n")
            if (index < events.size - 1) {
                writer.write("    },\n")
            } else {
                writer.write("    }\n")
            }
        }
        writer.write("  ]\n")
        writer.write("}\n")
        writer.flush()
    }

    fun exportToCsv(
        outputStream: OutputStream,
        devices: List<DeviceEntity>,
        events: List<EventEntity>
    ) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))
        // Map devices by id for fast lookup
        val deviceMap = devices.associateBy { it.id }

        // CSV Header
        writer.write("device_name,mac_address,device_type,event_type,timestamp,date_time,latitude,longitude,accuracy,location_address,is_unexpected_disconnect\n")

        for (event in events) {
            val device = deviceMap[event.deviceId]
            val deviceName = device?.name ?: "Unknown"
            val macAddress = device?.macAddress ?: ""
            val deviceType = device?.deviceType ?: "OTHER"
            val formattedDate = TimeFormatter.formatFullDateTime(event.timestamp)

            writer.write(escapeCsv(deviceName))
            writer.write(",")
            writer.write(escapeCsv(macAddress))
            writer.write(",")
            writer.write(escapeCsv(deviceType))
            writer.write(",")
            writer.write(escapeCsv(event.eventType))
            writer.write(",")
            writer.write(event.timestamp.toString())
            writer.write(",")
            writer.write(escapeCsv(formattedDate))
            writer.write(",")
            writer.write(event.latitude?.toString() ?: "")
            writer.write(",")
            writer.write(event.longitude?.toString() ?: "")
            writer.write(",")
            writer.write(event.accuracy?.toString() ?: "")
            writer.write(",")
            writer.write(escapeCsv(event.locationAddress))
            writer.write(",")
            writer.write(event.isUnexpectedDisconnect.toString())
            writer.write("\n")
        }
        writer.flush()
    }
}
