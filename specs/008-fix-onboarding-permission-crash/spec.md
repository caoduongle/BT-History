# Feature Specification: Safe Permission Onboarding & Android 14+ Foreground Service Crash Prevention

**Feature Branch**: `008-fix-onboarding-permission-crash`

**Created**: 2026-09-02

**Status**: Draft

**Input**: User description: "Trong PermissionOnboardingScreen, nút \"Bỏ qua / Vào giao diện chính\" (stringRes btn_skip_to_main, testTag \"skip_onboarding_button\") đang dùng CHUNG callback onPermissionsGranted với nút \"Cấp quyền & Bắt đầu\". Vì vậy MainActivity gọi thẳng DeviceViewModel.completeOnboarding() -> BluetoothWatcherService.startService() ngay cả khi người dùng CHƯA cấp ACCESS_FINE_LOCATION/ACCESS_COARSE_LOCATION hoặc BLUETOOTH_CONNECT. Trên Android 14 (API 34) trở lên, BluetoothWatcherService.onStartCommand() gọi ServiceCompat.startForeground(...) với foregroundServiceType = CONNECTED_DEVICE hoặc LOCATION mà KHÔNG kiểm tra quyền tương ứng đã được cấp hay chưa, khiến hệ thống ném SecurityException / MissingForegroundServiceTypeException ngay khi start -> app crash ngay khi người dùng bấm \"Bỏ qua\" trên thiết bị Android 14+. Hãy: 1. Tách rõ hai luồng trong PermissionOnboardingScreen: \"Cấp quyền & Bắt đầu\" (yêu cầu quyền thật rồi mới cho phép start service) và \"Bỏ qua\" (chỉ điều hướng vào màn hình chính, KHÔNG được gọi completeOnboarding()/startService() nếu quyền Bluetooth/Location cần thiết chưa được cấp). 2. Thêm một hàm kiểm tra tổng hợp, ví dụ BluetoothHelper.hasRequiredPermissionsForService(context), và gọi nó ngay đầu BluetoothWatcherService.onStartCommand() (và ở mọi nơi gọi BluetoothWatcherService.startService()) để chủ động không start foreground service (và không set foregroundServiceType tương ứng) nếu thiếu quyền, thay vì để hệ thống ném exception. 3. Nếu thiếu quyền khi được yêu cầu start: service phải dừng an toàn (stopSelf()), ghi log rõ ràng, và đảm bảo preferencesRepository.isServiceEnabledFlow không bị kẹt ở true theo cách gây crash-loop mỗi lần BootReceiver cố khởi động lại sau khi khởi động máy. 4. Khi người dùng bấm \"Bỏ qua\", vẫn cho vào màn hình chính bình thường, nhưng hiển thị rõ ràng (banner hoặc trạng thái trong SettingsScreen) rằng dịch vụ giám sát nền hiện đang TẮT vì thiếu quyền, kèm lối tắt để cấp quyền sau. 5. Viết test (Robolectric, theo phong cách các test hiện có trong app/src/test/java/com/example) tái hiện đúng kịch bản: bấm nút Skip khi quyền chưa được cấp -> xác nhận BluetoothWatcherService không gọi startForeground()/ServiceCompat.startForeground() và không có exception nào bị ném ra ngoài. 6. Tạo spec mới specs/008-fix-onboarding-permission-crash theo đúng khuôn Spec Kit hiện có của repo (spec.md, plan.md, tasks.md, theo format của specs/004 và specs/007), mô tả rõ root cause, giải pháp, Acceptance Scenarios và Success Criteria đo được."

---

## Root Cause Summary

1. **Coupled Callback in `PermissionOnboardingScreen`**:
   The "Bỏ qua / Vào giao diện chính" button (`testTag("skip_onboarding_button")`) triggers the exact same `onPermissionsGranted` lambda as the "Cấp quyền & Bắt đầu" button (`testTag("grant_permissions_button")`).
2. **Premature Foreground Service Startup**:
   `MainActivity` invokes `DeviceViewModel.completeOnboarding()`, which reads `isServiceEnabled.value` (defaulting to `true`) and immediately calls `BluetoothWatcherService.startService(context)`.
3. **Android 14 (API 34+) Foreground Service Type Enforcement**:
   `BluetoothWatcherService.onStartCommand()` calls `ServiceCompat.startForeground()` specifying `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE` and `FOREGROUND_SERVICE_TYPE_LOCATION`. Under Android 14+, invoking `startForeground()` with these types when the calling application lacks `BLUETOOTH_CONNECT` and `ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION` results in an immediate platform-level `SecurityException` (`MissingForegroundServiceTypeException`), crashing the application process immediately upon onboarding skip.
4. **Post-Reboot Crash Loop Vulnerability**:
   If `isServiceEnabled` remains `true` in `PreferencesRepository`, `BootReceiver` automatically invokes `BluetoothWatcherService.startService()` upon `ACTION_BOOT_COMPLETED` or `ACTION_MY_PACKAGE_REPLACED`, resulting in persistent crash loops whenever the phone restarts without granted permissions.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Decouple Onboarding Skip from Permission-Grant Flow (Priority: P1) 🎯 MVP

As a user installing or opening BT Watcher for the first time who prefers to explore the UI before granting sensitive location and Bluetooth permissions, I want clicking "Bỏ qua / Vào giao diện chính" to navigate me directly to the device list without initiating background Bluetooth/location tracking services, so that the application never crashes and respects my decision to defer permissions.

**Why this priority**: Crashing immediately on the first user interaction (skipping onboarding) causes a 100% immediate churn rate on Android 14+ devices. Separating the Skip flow from the Grant flow is essential for platform stability and compliance.

**Independent Test**: Launch the application on an Android 14 (API 34) device or Robolectric test environment with all permissions revoked. Click the "Bỏ qua / Vào giao diện chính" button. Verify the app navigates to `DeviceListScreen`, onboarding is marked complete, no foreground service is started, and no `SecurityException` is raised.

**Acceptance Scenarios**:

1. **Given** the user is on `PermissionOnboardingScreen` with permissions ungranted, **When** clicking "Bỏ qua / Vào giao diện chính", **Then** the screen invokes `onSkip()` instead of `onPermissionsGranted()`.
2. **Given** `onSkip()` is triggered, **When** `MainActivity` handles the event, **Then** `DeviceViewModel.skipOnboarding()` is called, navigating to `device_list` without calling `BluetoothWatcherService.startService()`.
3. **Given** the user clicks "Cấp quyền & Bắt đầu" and grants all required permissions via the system dialog, **When** permissions are confirmed granted, **Then** `onPermissionsGranted()` is triggered, `DeviceViewModel.completeOnboarding()` starts `BluetoothWatcherService`, and navigation proceeds to `device_list`.

---

### User Story 2 - Comprehensive Foreground Service Permission Gatekeeper (Priority: P1) 🎯 MVP

As an Android system and application maintainer, I want `BluetoothWatcherService` and its entry points to proactively verify all necessary runtime permissions before requesting foreground execution, so that under no circumstances can an unauthorized foreground service launch and cause a platform `SecurityException`.

**Why this priority**: Defense-in-depth is required. Even if onboarding is skipped safely, other entry points (such as `BootReceiver`, `SettingsScreen`, or external intents) could attempt to start `BluetoothWatcherService`. Centralized permission guarding guarantees safety regardless of call site.

**Independent Test**: Programmatically trigger `BluetoothWatcherService.startService(context)` and invoke `onStartCommand()` when permissions are revoked. Verify that `BluetoothWatcherService` does not invoke `ServiceCompat.startForeground()`, logs an informative warning, calls `stopSelf()`, sets `isServiceEnabled` to `false`, and returns `START_NOT_STICKY`.

**Acceptance Scenarios**:

1. **Given** a context lacking `BLUETOOTH_CONNECT` (on API 31+) or `ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION`, **When** `BluetoothHelper.hasRequiredPermissionsForService(context)` is evaluated, **Then** it returns `false`.
2. **Given** `BluetoothWatcherService.startService(context)` is called when `hasRequiredPermissionsForService` is `false`, **When** executed, **Then** the method aborts with a diagnostic log warning and does not dispatch a start intent to the OS.
3. **Given** `BluetoothWatcherService.onStartCommand()` is executed without required permissions, **When** evaluated, **Then** it stops foreground execution (`stopForeground(STOP_FOREGROUND_REMOVE)`), calls `stopSelf()`, updates `PreferencesRepository.setServiceEnabled(false)`, and returns `START_NOT_STICKY`.
4. **Given** a device reboot triggers `BootReceiver`, **When** permissions are missing, **Then** `BootReceiver` checks `BluetoothHelper.hasRequiredPermissionsForService(context)` and disables the service preference rather than attempting to launch the service.

---

### User Story 3 - In-App Visibility & Recovery for Disabled Service (Priority: P2)

As a user who skipped initial permission onboarding, I want to clearly understand on the main dashboard (`DeviceListScreen`) and in `SettingsScreen` that background monitoring is currently inactive due to missing permissions, and I want an effortless 1-tap shortcut to grant them whenever I am ready, so that I am never left wondering why device disconnect events are not being recorded.

**Why this priority**: Users who skip permissions must be informed that the core value proposition (automatic background tracking) is dormant, and they must be provided an intuitive recovery flow without digging through Android OS system settings.

**Independent Test**: After skipping onboarding, observe `DeviceListScreen` and `SettingsScreen`. Verify a prominent banner or status card states that background monitoring is paused due to missing permissions, and clicking the action button prompts the user to grant permissions.

**Acceptance Scenarios**:

1. **Given** the user is on `DeviceListScreen` and the background service is disabled due to missing permissions, **When** viewing the screen, **Then** an informative banner displays: "Dịch vụ giám sát nền đang tắt do chưa cấp đủ quyền Bluetooth hoặc Vị trí" with a "Cấp quyền" button.
2. **Given** the user clicks "Cấp quyền" on the banner or in `SettingsScreen`, **When** the permission dialog launcher completes with granted permissions, **Then** the banner disappears, `BluetoothWatcherService` is started, and status updates to "Đang chạy".
3. **Given** the user toggles the Foreground Service switch in `SettingsScreen` to ON while permissions are still missing, **When** clicked, **Then** the app requests the necessary permissions rather than silently failing or crashing.

---

### User Story 4 - Automated Robolectric Regression Suite on API 34 (Priority: P1)

As a developer and CI/CD maintainer, I want comprehensive Robolectric unit tests running on Android 14 (API 34) simulating the Skip onboarding path and unauthorized service starts, so that regressions cannot be introduced in future pull requests.

**Why this priority**: Automated unit tests ensure long-term stability and prove that Android 14 foreground service crash scenarios are permanently mitigated.

**Independent Test**: Run `./gradlew.bat testDebugUnitTest` and verify that tests targeting Android 14 (API 34) pass with 100% success.

**Acceptance Scenarios**:

1. **Given** an ungranted permission environment on API 34, **When** simulating the Skip button click, **Then** the test confirms `DeviceViewModel.skipOnboarding()` sets `isOnboardingCompleted = true`, `isServiceEnabled = false`, and does not invoke `startForeground`.
2. **Given** `BluetoothWatcherService` on API 34 without permissions, **When** `onStartCommand()` is executed, **Then** no `SecurityException` is thrown, `stopSelf()` is called, and `isServiceEnabledFlow` emits `false`.

---

### Edge Cases

- **Android Version Variances**:
  - **API 34+ (Android 14+)**: Strictly enforces `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE` (requires `BLUETOOTH_CONNECT`) and `FOREGROUND_SERVICE_TYPE_LOCATION` (requires `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION`).
  - **API 31-33 (Android 12-13)**: `BLUETOOTH_CONNECT` is a runtime permission; location foreground service type exists but connectedDevice enforcement began in API 34.
  - **API 24-30 (Android 7.0-11)**: Bluetooth permissions are install-time; `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` are runtime permissions.
- **Permissions Partially Granted**: If Bluetooth is granted but Location is denied (or vice-versa), `hasRequiredPermissionsForService()` evaluates to `false` because `BluetoothWatcherService` requires both types to operate its paired telemetry logging.
- **Permission Revocation While Running**: If a user revokes permissions from system settings while the service is active, subsequent invocations or reboots will trigger the permission check, immediately stopping the service cleanly without crashing.
- **Boot Completed with Missing Permissions**: `BootReceiver` checks `hasRequiredPermissionsForService` before starting the service; if false, it updates `PreferencesRepository` so that `isServiceEnabled` is cleanly set to `false`.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: `PermissionOnboardingScreen` MUST expose two distinct callbacks: `onPermissionsGranted: () -> Unit` and `onSkip: () -> Unit`.
- **FR-002**: In `PermissionOnboardingScreen`, clicking the "Bỏ qua / Vào giao diện chính" button (`testTag("skip_onboarding_button")`) MUST invoke `onSkip()`, and MUST NOT invoke `onPermissionsGranted()`.
- **FR-003**: `DeviceViewModel` MUST implement `skipOnboarding()`, which marks `isOnboardingCompleted` as `true` and sets `isServiceEnabled` to `false` when required permissions are missing.
- **FR-004**: `BluetoothHelper` MUST provide `hasRequiredPermissionsForService(context: Context): Boolean`, returning `true` only if both Bluetooth connect permission (`hasBluetoothConnectPermission`) and Location permission (`hasLocationPermission`) are granted.
- **FR-005**: `BluetoothWatcherService.startService(context: Context)` MUST check `BluetoothHelper.hasRequiredPermissionsForService(context)` before attempting to start the service, aborting with a warning log if missing.
- **FR-006**: `BluetoothWatcherService.onStartCommand()` MUST verify `BluetoothHelper.hasRequiredPermissionsForService(this)` before calling `ServiceCompat.startForeground()`. If permissions are absent, it MUST invoke `stopForeground(STOP_FOREGROUND_REMOVE)`, call `stopSelf()`, set `PreferencesRepository.setServiceEnabled(false)`, and return `START_NOT_STICKY`.
- **FR-007**: `BootReceiver.onReceive()` MUST verify `BluetoothHelper.hasRequiredPermissionsForService(context)` prior to calling `BluetoothWatcherService.startService()`. If permissions are absent, it MUST ensure `preferencesRepository.setServiceEnabled(false)`.
- **FR-008**: `DeviceListScreen` and `SettingsScreen` MUST display an alert banner / status indicator when the service is inactive due to missing permissions, with an interactive control allowing users to request permissions or open settings.
- **FR-009**: Automated Robolectric tests configured for API 34 MUST reproduce the onboarding skip scenario and confirm that no `SecurityException` is thrown and foreground service startup is safely prevented.

---

### Key Entities

- **`PermissionOnboardingScreen`**: Composable screen handling initial permission explanation, permission requests, and user onboarding completion or skip.
- **`BluetoothWatcherService`**: Android Foreground Service responsible for listening to Bluetooth connection broadcasts and recording GPS location upon connection changes.
- **`BluetoothHelper`**: Centralized utility holding permission checks (`hasBluetoothConnectPermission`, `hasLocationPermission`, `hasRequiredPermissionsForService`).
- **`PreferencesRepository`**: Persistent DataStore storage managing settings flags (`KEY_SERVICE_ENABLED`, `KEY_ONBOARDING_COMPLETED`).
- **`BootReceiver`**: BroadcastReceiver handling device reboot and package update intents.
- **`DeviceViewModel`**: Orchestrates UI state, onboarding transitions, and service toggling.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: **0 Crashes on Android 14+ (API 34+)**: Skipping onboarding on API 34+ produces 0 `SecurityException` / `MissingForegroundServiceTypeException` occurrences.
- **SC-002**: **100% Illegal Foreground Service Start Prevention**: When permissions are revoked, 0 calls to `ServiceCompat.startForeground()` or `startForeground()` occur across the entire application.
- **SC-003**: **0 Boot Crash Loops**: If device reboots with missing permissions, `BootReceiver` safely suppresses service startup and ensures `isServiceEnabled` is `false`.
- **SC-004**: **100% Automated Test Pass Rate**: Full test suite (`./gradlew.bat testDebugUnitTest`), including new Robolectric Android 14 tests, passes with 0 failures.

---

## Assumptions & Dependencies

- Android 14 (API 34) enforces mandatory runtime permissions corresponding to declared `foregroundServiceType` flags (`connectedDevice` requires `BLUETOOTH_CONNECT`; `location` requires `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION`).
- Normal app features (viewing cached device list, browsing past disconnect locations, searching devices) can operate safely in offline/read-only mode even when the background service is not running.
