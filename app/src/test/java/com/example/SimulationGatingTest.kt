package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.DeviceViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SimulationGatingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var app: BtWatcherApplication
    private lateinit var viewModel: DeviceViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext<BtWatcherApplication>()
        viewModel = DeviceViewModel(
            repository = app.repository,
            preferencesRepository = app.preferencesRepository,
            context = app
        )
        // Reset developer mode preference to false
        runBlocking {
            app.preferencesRepository.setDeveloperModeEnabled(false)
        }
    }

    /**
     * Xác nhận preference developer_mode_enabled mặc định là false
     * và có thể cập nhật qua setDeveloperModeEnabled().
     */
    @Test
    fun testDeveloperModePreference_defaultsToFalseAndUpdates() {
        runBlocking {
            val initial = app.preferencesRepository.isDeveloperModeEnabledFlow.first()
            assertFalse("Mặc định developer mode phải là false", initial)

            app.preferencesRepository.setDeveloperModeEnabled(true)
            val updated = app.preferencesRepository.isDeveloperModeEnabledFlow.first()
            assertTrue("Sau khi set true, developer mode phải là true", updated)

            app.preferencesRepository.setDeveloperModeEnabled(false)
            val reverted = app.preferencesRepository.isDeveloperModeEnabledFlow.first()
            assertFalse("Sau khi set false, developer mode phải là false", reverted)
        }
    }

    /**
     * Xác nhận DeviceViewModel phản ánh đúng trạng thái developer mode
     * và cập nhật reactive khi gọi viewModel.setDeveloperModeEnabled().
     */
    @Test
    fun testDeviceViewModel_developerModeStateFlow() {
        runBlocking {
            viewModel.setDeveloperModeEnabled(true)

            var isDev = false
            val start = System.currentTimeMillis()
            while (!isDev && System.currentTimeMillis() - start < 3000) {
                Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                isDev = viewModel.isDeveloperModeEnabled.first()
                if (!isDev) delay(50)
            }
            assertTrue("ViewModel phải phát ra isDeveloperModeEnabled = true", isDev)

            viewModel.setDeveloperModeEnabled(false)
            while (isDev && System.currentTimeMillis() - start < 6000) {
                Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                isDev = viewModel.isDeveloperModeEnabled.first()
                if (isDev) delay(50)
            }
            assertFalse("ViewModel phải phát ra isDeveloperModeEnabled = false", isDev)
        }
    }

    /**
     * Xác nhận thao tác chạm vào dòng phiên bản 7 lần trong SettingsScreen
     * sẽ mở khoá Developer Mode thành công và lưu vào PreferencesRepository.
     */
    @Test
    fun testSettingsScreen_sevenTapEasterEggUnlocksDeveloperMode() {
        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onBack = {}
            )
        }

        val versionNode = composeTestRule.onNodeWithTag("settings_app_version")
        versionNode.performScrollTo()
        versionNode.assertExists()

        // Nhấn 7 lần liên tiếp
        repeat(7) {
            versionNode.performClick()
            composeTestRule.waitForIdle()
        }

        // Xác nhận trạng thái preference được lưu là true
        runBlocking {
            var devMode = false
            val start = System.currentTimeMillis()
            while (!devMode && System.currentTimeMillis() - start < 3000) {
                Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                devMode = app.preferencesRepository.isDeveloperModeEnabledFlow.first()
                if (!devMode) delay(50)
            }
            assertTrue("Nhấn 7 lần vào version row phải kích hoạt Developer Mode", devMode)
        }
    }

    /**
     * Xác nhận logic simulateTestEvent() của ViewModel vẫn hoạt động 100% nguyên vẹn
     * để phục vụ mục đích kiểm thử tự động và QA.
     */
    @Test
    fun testSimulateTestEvent_remainsFullyFunctional() {
        runBlocking {
            val testMac = "AA:BB:CC:DD:EE:FF"
            viewModel.simulateTestEvent(
                deviceName = "Mock QA Headset",
                macAddress = testMac,
                deviceType = "HEADSET",
                eventType = "CONNECT",
                latitude = 10.7769,
                longitude = 106.7009,
                address = "TP. Hồ Chí Minh"
            )

            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

            // Kiểm tra thiết bị đã được tạo trong Room Database
            var device = app.repository.getDeviceByMac(testMac)
            val start = System.currentTimeMillis()
            while (device == null && System.currentTimeMillis() - start < 3000) {
                Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                device = app.repository.getDeviceByMac(testMac)
                if (device == null) delay(50)
            }

            assertTrue("Thiết bị giả lập phải được lưu thành công vào cơ sở dữ liệu", device != null)
            assertEquals("Mock QA Headset", device?.name)
            assertTrue("Trạng thái thiết bị phải là đang kết nối", device?.isConnected == true)
        }
    }
}
