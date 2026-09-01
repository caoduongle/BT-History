# Feature Specification: Deduplicate Bluetooth Receiver

**Feature Branch**: `001-deduplicate-bluetooth-receiver`

**Created**: 2026-09-02

**Status**: Draft

**Input**: User description: "Trong repo BT-History (Android/Kotlin), BluetoothEventReceiver hiện được khai báo tĩnh trong app/src/main/AndroidManifest.xml VÀ được đăng ký động trong BluetoothWatcherService.registerBluetoothReceiver(), khiến mỗi sự kiện Bluetooth bị xử lý 2 lần (ghi trùng DB, gọi định vị 2 lần, bắn thông báo trùng). Hãy: 1. Gỡ bỏ khai báo <receiver android:name=\".receiver.BluetoothEventReceiver\"> khỏi AndroidManifest.xml (chỉ giữ đăng ký động qua registerReceiver trong BluetoothWatcherService). 2. Đảm bảo BluetoothWatcherService đăng ký receiver ngay khi service khởi động và huỷ đăng ký đúng cách trong onDestroy (đã có, kiểm tra lại không bị leak). 3. Xác nhận app vẫn nhận được sự kiện Bluetooth khi service đang chạy nền, và giải thích ngắn gọn trong code comment lý do chọn đăng ký động thay vì tĩnh (liên quan đến hạn chế implicit broadcast từ Android 8+). 4. Viết một test (Robolectric hoặc unit test có mock) xác nhận một sự kiện ACL_CONNECTED chỉ tạo đúng 1 EventEntity trong DB, không phải 2."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Single Processing of Bluetooth Events (Priority: P1)

As a mobile user tracking Bluetooth devices, when my Bluetooth peripheral connects or disconnects, I want the application to record exactly one timeline entry, query my location once, and send at most one alert notification so that my device log is accurate and my battery and system resources are not wasted on redundant duplicate processing.

**Why this priority**: Duplicate event handling directly impairs core functionality by cluttering history with double entries, causing duplicate notifications that irritate users, and triggering redundant high-accuracy GPS requests that drain the battery.

**Independent Test**: Can be fully tested by establishing a Bluetooth connection with a paired device while the tracking service is active, then inspecting the history timeline to confirm that only a single connection record appears with single location information and no duplicate alerts.

**Acceptance Scenarios**:

1. **Given** the tracking service is active and listening, **When** a Bluetooth device connects (`ACL_CONNECTED`), **Then** exactly one connection event is saved to the history log and location acquisition is triggered once.
2. **Given** the tracking service is active with disconnect notifications enabled, **When** a tracked device unexpectedly disconnects (`ACL_DISCONNECTED`), **Then** exactly one disconnection event is recorded in the history log and exactly one disconnect alert notification is presented to the user.

---

### User Story 2 - Safe Lifecycle Management for Background Monitoring (Priority: P2)

As a user running the app in the background, I want the application to reliably detect connection events whenever the tracking service is running, and release all receiver resources cleanly when tracking is stopped so that background battery consumption is controlled and the app does not leak system resources or crash.

**Why this priority**: Dynamic registration requires strict lifecycle pairing. Failure to register on startup loses events; failure to unregister on teardown causes memory leaks or runtime exceptions.

**Independent Test**: Can be tested by toggling the tracking service on and off, confirming events are intercepted while active, and ensuring the receiver unregisters without crashes or leaks when the service is destroyed.

**Acceptance Scenarios**:

1. **Given** the tracking service is started, **When** initialized, **Then** the Bluetooth event receiver is registered immediately in `onCreate` with the necessary intent filters.
2. **Given** the tracking service is running, **When** the service is stopped or destroyed (`onDestroy`), **Then** the dynamic receiver is properly unregistered and references are cleared without throwing unregistration exceptions.
3. **Given** the app is in the background with the foreground service running, **When** a Bluetooth event occurs, **Then** the event is captured and processed reliably.

---

### User Story 3 - Automated Regression Safeguard (Priority: P3)

As a developer and maintainer of the project, I want an automated test suite verifying that dispatching a single connection event generates exactly one database entity so that future refactoring or manifest changes cannot inadvertently reintroduce duplicate event processing.

**Why this priority**: Protects against regressions where duplicate manifest declarations or multiple receiver subscriptions might be added back in the future.

**Independent Test**: Can be tested by running the automated unit/Robolectric test verifying database record count after dispatching an `ACL_CONNECTED` broadcast intent.

**Acceptance Scenarios**:

1. **Given** an initialized environment with the receiver registered, **When** an `ACL_CONNECTED` broadcast intent is dispatched for a device, **Then** the database contains exactly 1 corresponding `EventEntity` record for that event.

---

### Edge Cases

- **Rapid Connection/Disconnection Flapping**: If a device rapidly connects and disconnects within seconds, the system must process each distinct transition sequentially without dropping either event or generating duplicate entries for either state.
- **Service Restart / Re-binding**: If the service is rapidly stopped and restarted (e.g., via settings toggle or OS recreation), the receiver must be unregistered and cleanly re-registered without throwing `IllegalArgumentException: Receiver not registered` or creating multiple active receiver instances.
- **Service Termination during Asynchronous Event Processing**: If the service is destroyed while an asynchronous event handler (`goAsync()` / coroutine) is executing, the ongoing job should complete or cancel safely without crashing the process.
- **Implicit Broadcast Restrictions on Android 8.0+ (API 26+)**: Android prohibits statically declared manifest receivers for implicit broadcasts like `ACTION_ACL_CONNECTED`. The system relies on dynamic registration within the foreground service to remain compliant and operational on modern Android versions.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST remove the static `<receiver android:name=".receiver.BluetoothEventReceiver">` declaration from `AndroidManifest.xml`.
- **FR-002**: The system MUST register the `BluetoothEventReceiver` dynamically when `BluetoothWatcherService` initializes (`onCreate`) with filters for all required Bluetooth actions (`ACL_CONNECTED`, `ACL_DISCONNECTED`, `BOND_STATE_CHANGED`, A2DP connection state changed, Headset connection state changed, Adapter state changed).
- **FR-003**: The system MUST unregister the dynamic `BluetoothEventReceiver` when `BluetoothWatcherService` is destroyed (`onDestroy`), ensuring no receiver leaks and guarding against unregistered receiver exceptions.
- **FR-004**: The system MUST maintain background event reception capability while `BluetoothWatcherService` runs as a foreground service.
- **FR-005**: For any discrete Bluetooth event received, the system MUST persist exactly one `EventEntity` in the database, trigger location lookup at most once, and issue at most one notification alert.
- **FR-006**: The codebase MUST include clear documentation in code comments explaining that dynamic registration is used because Android 8.0+ (API 26) limits implicit broadcasts for manifest-declared receivers.
- **FR-007**: The system MUST provide an automated test (using Robolectric or unit tests with mocks) confirming that an `ACL_CONNECTED` event creates exactly one `EventEntity` in the database.

### Key Entities

- **EventEntity**: Represents a recorded Bluetooth event in the local database. Key attributes include device MAC address, device name, event type (`CONNECT` or `DISCONNECT`), timestamp, latitude, longitude, address, and accuracy.
- **DeviceEntity**: Represents a unique tracked Bluetooth peripheral, storing MAC address, device name, device type, last seen timestamp, and current connection status.
- **BluetoothWatcherService**: The foreground service component responsible for managing the lifecycle of dynamic event listening and background device monitoring.
- **BluetoothEventReceiver**: The broadcast receiver component that parses system Bluetooth intents and delegates recording to the repository.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Zero duplicate event records: exactly 1 database log record is created per physical Bluetooth connection or disconnection event (100% deduplication).
- **SC-002**: Exactly 1 alert notification is displayed per unexpected disconnection event when disconnect alerts are active (0 duplicate notifications).
- **SC-003**: Exactly 1 location resolution is performed per received Bluetooth event.
- **SC-004**: Zero memory leaks and zero `ReceiverNotRegisteredException` crashes across repeated service start, stop, and restart cycles.
- **SC-005**: 100% pass rate on automated regression tests verifying that a single `ACL_CONNECTED` broadcast produces exactly 1 database event record.

## Assumptions

- The app utilizes a foreground service (`BluetoothWatcherService`) with connected device and location foreground service types, which satisfies Android requirements for background execution and dynamic broadcast reception while active.
- Android 8.0+ (API 26+) restrictions on implicit broadcast receivers mean that statically registered manifest receivers for `ACL_CONNECTED` and `ACL_DISCONNECTED` are deprecated/incompatible for background execution; moving entirely to dynamic registration within the active foreground service aligns with platform best practices.
- Boot completion (`BootReceiver`) and service auto-start logic will start `BluetoothWatcherService` when appropriate, which in turn registers the dynamic receiver.
- Existing database schema, repository interfaces, and notification helpers require no breaking changes to accommodate this single-receiver architecture.
