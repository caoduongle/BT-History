package com.example

import android.app.Service
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.receiver.BootReceiver
import com.example.service.BluetoothWatcherService
import com.example.ui.viewmodel.DeviceViewModel
import com.example.util.BluetoothHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingPermissionSkipTest {

    private lateinit var app: BtWatcherApplication

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext<BtWatcherApplication>()
        // Drain any pending started services in shadow
        while (Shadows.shadowOf(app).nextStartedService != null) {
            // drained
        }
    }

    /**
     * Xác nhận BluetoothHelper.hasRequiredPermissionsForService trả về false
     * khi chưa có quyền Bluetooth và Location.
     */
    @Test
    fun testHasRequiredPermissionsForService_returnsFalseWhenPermissionsMissing() {
        val hasPerms = BluetoothHelper.hasRequiredPermissionsForService(app)
        assertFalse("Mặc định khi chưa cấp quyền, hasRequiredPermissionsForService phải là false", hasPerms)
    }

    /**
     * Xác nhận skipOnboarding() trong DeviceViewModel đánh dấu onboarding hoàn tất
     * và tắt dịch vụ (isServiceEnabled = false) khi thiếu quyền, không khởi chạy service.
     */
    @Test
    fun testSkipOnboarding_setsOnboardingCompletedAndDisablesService() {
        runBlocking {
            val viewModel = DeviceViewModel(
                repository = app.repository,
                preferencesRepository = app.preferencesRepository,
                context = app
            )

            viewModel.skipOnboarding()

            // Đợi DataStore cập nhật
            var completed = false
            var serviceEnabled = true
            val start = System.currentTimeMillis()
            while ((!completed || serviceEnabled) && System.currentTimeMillis() - start < 3000) {
                Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                completed = app.preferencesRepository.isOnboardingCompletedFlow.first()
                serviceEnabled = app.preferencesRepository.isServiceEnabledFlow.first()
                if (!completed || serviceEnabled) {
                    delay(50)
                }
            }

            assertTrue("Onboarding phải được đánh dấu hoàn thành sau khi skip", completed)
            assertFalse("Dịch vụ phải bị tắt khi skip mà chưa có quyền", serviceEnabled)

            // Xác nhận không có Intent start service nào được phát ra
            val nextServiceIntent = Shadows.shadowOf(app).nextStartedService
            assertNull("Không được khởi động service khi skip onboarding mà chưa có quyền", nextServiceIntent)
        }
    }

    /**
     * Xác nhận BluetoothWatcherService.startService() chủ động chặn không gửi Intent
     * nếu thiếu quyền cần thiết.
     */
    @Test
    fun testBluetoothWatcherServiceStartService_abortsWhenPermissionsMissing() {
        BluetoothWatcherService.startService(app)
        val nextServiceIntent = Shadows.shadowOf(app).nextStartedService
        assertNull("startService() phải huỷ sớm và không gửi start intent khi thiếu quyền", nextServiceIntent)
    }

    /**
     * Kịch bản cốt lõi: Khi BluetoothWatcherService nhận onStartCommand() trên Android 14 (API 34)
     * mà không có quyền Bluetooth/Location:
     * 1. Không ném SecurityException / MissingForegroundServiceTypeException ra ngoài.
     * 2. Không gọi startForeground() / ServiceCompat.startForeground().
     * 3. Gọi stopSelf() an toàn và trả về START_NOT_STICKY.
     * 4. Đặt preferences isServiceEnabledFlow về false để chống crash-loop.
     */
    @Test
    fun testServiceOnStartCommand_handlesMissingPermissionsGracefullyOnSdk34() {
        runBlocking {
            val serviceController = Robolectric.buildService(BluetoothWatcherService::class.java)
            val service = serviceController.create().get()
            val shadowService = Shadows.shadowOf(service)

            val intent = Intent(BluetoothWatcherService.ACTION_START_SERVICE)

            // Gọi onStartCommand mà không có quyền trên Android 14 (SDK 34)
            val result = service.onStartCommand(intent, 0, 1)

            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

            // 1. Phải trả về START_NOT_STICKY
            assertEquals("Service phải trả về START_NOT_STICKY khi thiếu quyền", Service.START_NOT_STICKY, result)

            // 2. Service phải tự dừng (stopSelf)
            assertTrue("Service phải gọi stopSelf() khi thiếu quyền", shadowService.isStoppedBySelf)

            // 3. Không được đặt foreground notification
            assertNull("Không được gọi startForeground khi thiếu quyền trên Android 14", shadowService.lastForegroundNotification)

            // 4. Preferences isServiceEnabled phải được chuyển về false
            var serviceEnabled = true
            val start = System.currentTimeMillis()
            while (serviceEnabled && System.currentTimeMillis() - start < 3000) {
                Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                serviceEnabled = app.preferencesRepository.isServiceEnabledFlow.first()
                if (serviceEnabled) {
                    delay(50)
                }
            }
            assertFalse("isServiceEnabledFlow phải được chuyển về false để tránh crash-loop", serviceEnabled)

            serviceController.destroy()
        }
    }

    /**
     * Xác nhận BootReceiver không khởi chạy service sau khi khởi động máy
     * nếu quyền chưa được cấp, và chủ động set isServiceEnabled = false.
     */
    @Test
    fun testBootReceiver_suppressesServiceStartAndDisablesPrefWhenPermissionsMissing() {
        runBlocking {
            // Giả lập trạng thái isServiceEnabled đang là true
            app.preferencesRepository.setServiceEnabled(true)

            val receiver = BootReceiver()
            val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED)

            receiver.onReceive(app, bootIntent)

            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

            var serviceEnabled = true
            val start = System.currentTimeMillis()
            while (serviceEnabled && System.currentTimeMillis() - start < 3000) {
                Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                serviceEnabled = app.preferencesRepository.isServiceEnabledFlow.first()
                if (serviceEnabled) {
                    delay(50)
                }
            }

            assertFalse("BootReceiver phải tắt isServiceEnabled nếu thiếu quyền", serviceEnabled)
            val nextServiceIntent = Shadows.shadowOf(app).nextStartedService
            assertNull("BootReceiver không được start service khi thiếu quyền", nextServiceIntent)
        }
    }
}
