package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeFormatter {

    private val fullDateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("vi", "VN"))
    private val timeOnlyFormat = SimpleDateFormat("HH:mm", Locale("vi", "VN"))
    private val dateOnlyFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))

    fun formatFullDateTime(timestamp: Long): String {
        if (timestamp <= 0) return "Chưa xác định"
        return fullDateTimeFormat.format(Date(timestamp))
    }

    fun formatRelativeTime(timestamp: Long): String {
        if (timestamp <= 0) return "Chưa xác định"
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        if (diff < 0) return "Vừa xong"

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 45 -> "Vừa xong"
            minutes < 60 -> "$minutes phút trước"
            hours < 24 -> "$hours giờ trước"
            days == 1L -> "Hôm qua lúc ${timeOnlyFormat.format(Date(timestamp))}"
            days < 7 -> "$days ngày trước"
            else -> dateOnlyFormat.format(Date(timestamp))
        }
    }

    fun isToday(timestamp: Long): Boolean {
        val calNow = Calendar.getInstance()
        val calTarget = Calendar.getInstance().apply { timeInMillis = timestamp }
        return calNow.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
                calNow.get(Calendar.DAY_OF_YEAR) == calTarget.get(Calendar.DAY_OF_YEAR)
    }

    fun isYesterday(timestamp: Long): Boolean {
        val calNow = Calendar.getInstance()
        calNow.add(Calendar.DAY_OF_YEAR, -1)
        val calTarget = Calendar.getInstance().apply { timeInMillis = timestamp }
        return calNow.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
                calNow.get(Calendar.DAY_OF_YEAR) == calTarget.get(Calendar.DAY_OF_YEAR)
    }

    fun isWithinLastNDays(timestamp: Long, days: Int): Boolean {
        val now = System.currentTimeMillis()
        val threshold = now - (days.toLong() * 24 * 60 * 60 * 1000)
        return timestamp >= threshold
    }

    fun formatCoordinates(lat: Double?, lon: Double?): String {
        if (lat == null || lon == null) return "Chưa có tọa độ GPS"
        return String.format(Locale.US, "%.5f, %.5f", lat, lon)
    }
}
