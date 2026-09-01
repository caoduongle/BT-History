package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.TimeFilter
import com.example.util.BluetoothHelper
import com.example.util.TimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StringResourcesTest {

    private val context = ApplicationProvider.getApplicationContext<BtWatcherApplication>()

    @Test
    fun testTimeFilterEnum_hasValidStringResources() {
        for (filter in TimeFilter.entries) {
            assertTrue("titleRes must be non-zero", filter.titleRes != 0)
            val localized = context.getString(filter.titleRes)
            assertNotNull(localized)
            assertTrue("Localized string must not be blank", localized.isNotBlank())
            assertEquals(filter.label, localized)
        }
    }

    @Test
    fun testTimeFormatter_contextOverloads() {
        val now = System.currentTimeMillis()

        // Just now
        val justNow = TimeFormatter.formatRelativeTime(context, now - 1000)
        assertEquals("Vừa xong", justNow)

        // 5 minutes ago
        val fiveMinAgo = TimeFormatter.formatRelativeTime(context, now - 5 * 60 * 1000)
        assertEquals("5 phút trước", fiveMinAgo)

        // Unknown
        val unknown = TimeFormatter.formatRelativeTime(context, -1L)
        assertEquals("Chưa xác định", unknown)

        // Coordinates
        val coords = TimeFormatter.formatCoordinates(context, 10.7769, 106.7009)
        assertEquals("10.77690, 106.70090", coords)

        val nullCoords = TimeFormatter.formatCoordinates(context, null, null)
        assertEquals("Chưa có tọa độ GPS", nullCoords)
    }

    @Test
    fun testBluetoothHelper_constantsAndDefaultStrings() {
        assertEquals("Thiết bị Bluetooth", BluetoothHelper.DEFAULT_DEVICE_NAME)
        assertEquals("Thiết bị không rõ", BluetoothHelper.UNKNOWN_DEVICE_NAME)

        assertEquals("Thiết bị Bluetooth", context.getString(R.string.device_default_name))
        assertEquals("Thiết bị không rõ", context.getString(R.string.device_unknown_name))
    }

    @Test
    fun testNotificationAndBannerResources_arePresent() {
        assertEquals("Dịch vụ giám sát BT Watcher", context.getString(R.string.notif_channel_service_name))
        assertEquals("Cảnh báo ngắt kết nối thiết bị", context.getString(R.string.notif_channel_alert_name))
        assertEquals("BT Watcher đang hoạt động", context.getString(R.string.notif_service_active_title))
        assertEquals("⚠️ Thiết bị vừa ngắt kết nối!", context.getString(R.string.notif_alert_title))

        assertTrue(context.getString(R.string.warning_banner_title).isNotBlank())
        assertTrue(context.getString(R.string.search_placeholder).isNotBlank())
        assertTrue(context.getString(R.string.settings_title).isNotBlank())
    }
}
