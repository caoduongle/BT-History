# Feature Specification: Remove Redundant Background Location Permission

**Feature Branch**: `004-remove-background-location-permission`

**Created**: 2026-09-02

**Status**: Draft

**Input**: User description: "App BT-History đang xin ACCESS_BACKGROUND_LOCATION trong AndroidManifest.xml. Vị trí chỉ được lấy tại thời điểm xảy ra sự kiện Bluetooth, trong lúc BluetoothWatcherService (foreground service) đang chạy. Hãy: 1. Giải thích rõ (dạng comment/README) app có thực sự cần ACCESS_BACKGROUND_LOCATION hay không, dựa trên việc lấy vị trí luôn xảy ra trong ngữ cảnh foreground service đang active. 2. Nếu không cần thiết, gỡ quyền này khỏi Manifest và cập nhật PermissionOnboardingScreen tương ứng (không xin quyền đó nữa). 3. Nếu vẫn cần giữ (ví dụ vì một luồng nào đó chạy ngoài foreground service), hãy chỉ rõ luồng đó trong code và đảm bảo tuân thủ policy Google Play về background location (mô tả rõ mục đích trong Play Console khi phát hành)."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Frictionless Permission Onboarding & Play Store Compliance (Priority: P1)

As an Android user installing BT-History, I want the permission request flow to be clear and straightforward—granting location access with standard "While using the app" permission—without being confused or alarmed by invasive "Allow all the time" system prompts or redirected into Android System Settings.

As an application publisher, I want the app package to be 100% compliant with the Google Play Location Policy, eliminating unnecessary declaration forms, video demonstrations, and the risk of policy rejections caused by unused background location declarations.

**Why this priority**: Declaring `ACCESS_BACKGROUND_LOCATION` in the manifest triggers Google Play's strictest review process (mandatory video proof, prominent in-app disclosure before system prompt, 2-step permission flow). Because BT-History only acquires location when Bluetooth events occur while its `BluetoothWatcherService` (Foreground Service) is running, Google's Android guidelines classify this as **foreground location access** (`foregroundServiceType="location"`), making `ACCESS_BACKGROUND_LOCATION` entirely redundant and harmful to user conversion and Play Store approval.

**Independent Test**: Build the application, inspect `AndroidManifest.xml` to verify `ACCESS_BACKGROUND_LOCATION` is absent, and verify that `LocationHelper.getCurrentLocation()` successfully records coordinates during foreground service operation.

**Acceptance Scenarios**:

1. **Given** the app manifest, **When** reviewing declared permissions, **Then** `android.permission.ACCESS_BACKGROUND_LOCATION` is completely removed, while `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `FOREGROUND_SERVICE`, and `FOREGROUND_SERVICE_LOCATION` are retained.
2. **Given** a user completing the onboarding flow, **When** granting location permissions on `PermissionOnboardingScreen`, **Then** the app only requests `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` without prompting for background location or redirecting to system settings.
3. **Given** the foreground service `BluetoothWatcherService` is active with `foregroundServiceType="connectedDevice|location"`, **When** a Bluetooth connection or disconnection broadcast is processed, **Then** `LocationHelper.getCurrentLocation()` successfully captures GPS coordinates under foreground permissions.

---

### User Story 2 - Architectural Transparency & Policy Documentation (Priority: P2)

As a maintainer or auditor of the project, I want clear technical documentation in the manifest and `README.md` explaining why `ACCESS_BACKGROUND_LOCATION` is unnecessary, how the foreground service location model operates according to Android 10-15 standards, and how it satisfies Google Play policies.

**Why this priority**: Future developers might naively re-add `ACCESS_BACKGROUND_LOCATION` thinking it is required for background Bluetooth tracking. Clear architectural explanations prevent technical regressions.

**Independent Test**: Review `AndroidManifest.xml` and `README.md` to confirm detailed explanations of the permission model and Google Play policy alignment.

**Acceptance Scenarios**:

1. **Given** `AndroidManifest.xml`, **When** inspecting the location permission block, **Then** a comprehensive comment explains that location is strictly tied to `BluetoothWatcherService` (`foregroundServiceType="location"`) and why `ACCESS_BACKGROUND_LOCATION` is excluded.
2. **Given** `README.md`, **When** reading the permissions or architecture section, **Then** the document clearly details the rationale, user privacy benefits, and Google Play compliance strategy.

---

### Edge Cases

- **Android 10 (API 29) to Android 14+ (API 34+) Compatibility**: On Android 10+, any location access without an active foreground service requires `ACCESS_BACKGROUND_LOCATION`. By ensuring `BluetoothWatcherService` starts with `foregroundServiceType="connectedDevice|location"` and displays an ongoing notification, Android treats all location requests initiated by the service/receiver as foreground requests.
- **Service Not Started / Stopped**: If the user turns off the service in settings, `BluetoothWatcherService` is stopped and dynamic receivers are unregistered. No background GPS polling occurs when the service is stopped, confirming background location is never needed.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST remove `<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />` from `app/src/main/AndroidManifest.xml`.
- **FR-002**: The system MUST retain `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `FOREGROUND_SERVICE`, and `FOREGROUND_SERVICE_LOCATION` in `AndroidManifest.xml`.
- **FR-003**: `BluetoothWatcherService` MUST retain `android:foregroundServiceType="connectedDevice|location"` in `AndroidManifest.xml`.
- **FR-004**: `PermissionOnboardingScreen` and `SettingsScreen` MUST NOT request `ACCESS_BACKGROUND_LOCATION` at runtime.
- **FR-005**: An explanatory architectural comment MUST be placed in `AndroidManifest.xml` detailing why `ACCESS_BACKGROUND_LOCATION` is intentionally omitted.
- **FR-006**: A dedicated documentation section MUST be added to `README.md` explaining the permission model, foreground service location justification, and Google Play policy compliance.
- **FR-007**: All automated unit tests must continue to pass without regression.

### Key Entities

- **AndroidManifest.xml**: Defines system permission declarations and service component metadata.
- **BluetoothWatcherService**: Foreground service providing the foreground process execution context for location acquisition.
- **PermissionOnboardingScreen**: User onboarding UI requesting required runtime permissions.
- **README.md**: Project architectural documentation explaining security and privacy choices.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 0 occurrences of `ACCESS_BACKGROUND_LOCATION` in `AndroidManifest.xml`.
- **SC-002**: 100% elimination of Google Play Background Location Policy declaration requirements (no video audit or background location forms required).
- **SC-003**: 100% retention of GPS location capture during Bluetooth events when `BluetoothWatcherService` is running.
- **SC-004**: 100% pass rate on all automated unit tests.

## Assumptions

- Google's official Android documentation and Google Play Policy specify that location access performed while a valid Foreground Service with `foregroundServiceType="location"` is running is classified as foreground access, fully satisfied by `ACCESS_FINE_LOCATION`.
- The application has no WorkManager jobs, AlarmManager receivers, or background threads requesting location when `BluetoothWatcherService` is not active.
