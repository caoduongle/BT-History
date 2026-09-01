# Feature Specification: Classify Unexpected Disconnect

**Feature Branch**: `002-classify-unexpected-disconnect`

**Created**: 2026-09-02

**Status**: Draft

**Input**: User description: "Trong BluetoothEventReceiver.handleBluetoothAction(), biến isUnexpectedDisconnect hiện luôn được set true cho mọi ACTION_ACL_DISCONNECTED và mọi CONNECTION_STATE_CHANGED chuyển sang DISCONNECTED, không phân biệt người dùng chủ động tắt Bluetooth/ngắt thiết bị với việc bị rớt kết nối bất thường. Hãy thiết kế lại logic phân loại 'ngắt kết nối bất ngờ' theo hướng: - Nếu ngay trước đó có ACTION_STATE_CHANGED của BluetoothAdapter chuyển sang STATE_OFF hoặc STATE_TURNING_OFF trong một khoảng thời gian ngắn (ví dụ 3-5 giây) trước sự kiện DISCONNECT, coi là NGẮT CHỦ ĐỘNG (không cảnh báo). - Nếu không có tín hiệu adapter tắt trước đó, coi là bất ngờ (giữ hành vi cảnh báo hiện tại). - Đưa ngưỡng thời gian ra hằng số có thể chỉnh, viết rõ comment giải thích heuristic này có thể chưa hoàn hảo 100% và lý do đánh đổi. Sau khi sửa, viết test cho các trường hợp: (a) tắt Bluetooth thủ công rồi ngắt kết nối -> không cảnh báo; (b) thiết bị rớt kết nối đột ngột trong khi Bluetooth vẫn bật -> có cảnh báo."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Eliminate False Alarms on Intentional Bluetooth Disablement (Priority: P1)

As an app user who frequently turns off my phone's Bluetooth or disables connections manually, I want the app to recognize when a disconnection is caused by disabling Bluetooth so that I am not bombarded with annoying false alarms stating my device was "lost" or "unexpectedly disconnected".

**Why this priority**: Currently, every time a user turns off Bluetooth, a high-priority heads-up warning notification is triggered. This creates significant user annoyance, diminishes trust in actual disconnection alerts, and degrades app quality.

**Independent Test**: Connect a peripheral, turn off phone Bluetooth via the system quick settings, and verify that the disconnection event is saved as intentional (`isUnexpectedDisconnect = false`) and no alert notification is triggered.

**Acceptance Scenarios**:

1. **Given** a device is currently connected and the monitoring service is active, **When** the phone's Bluetooth adapter transitions to `STATE_TURNING_OFF` or `STATE_OFF` and peripheral disconnection occurs within the heuristic time threshold, **Then** the disconnection event is recorded with `isUnexpectedDisconnect = false` and no disconnection alert notification is shown.
2. **Given** a device is currently connected and the monitoring service is active with Bluetooth remaining enabled, **When** the peripheral unexpectedly disconnects (e.g. powered down, leaves RF range, or connection dropped), **Then** the disconnection event is recorded with `isUnexpectedDisconnect = true` and a disconnection alert notification is shown to the user (if alerts are enabled in settings).

---

### User Story 2 - Maintainable Heuristic Threshold & Architectural Transparency (Priority: P2)

As an engineer maintaining the project, I want the timing window separating intentional vs. unexpected disconnections to be encapsulated as an easily tunable constant, accompanied by comprehensive architectural documentation explaining the heuristic nature, assumptions, and edge cases.

**Why this priority**: Operating system scheduling delays and device-specific hardware timing vary across Android manufacturers. Having an explicit constant and clear documentation ensures the heuristic can be safely calibrated and reasoned about without introducing regressions.

**Independent Test**: Inspect the codebase to verify the threshold is defined as an exported/configurable constant and the rationale/trade-offs are thoroughly documented in code comments.

**Acceptance Scenarios**:

1. **Given** the receiver logic, **When** reviewing the time window check, **Then** the threshold is defined as a named constant (e.g., `ADAPTER_OFF_HEURISTIC_WINDOW_MS = 5000L`) rather than a magic number.
2. **Given** the heuristic implementation, **When** reviewing code comments, **Then** the documentation explicitly explains why an adapter-state heuristic was selected, what trade-offs exist (e.g., delayed OS intents vs. rapid manual reconnects), and how it operates.

---

### User Story 3 - Automated Regression Safeguard for Disconnect Scenarios (Priority: P3)

As a developer, I want automated unit tests verifying both intentional adapter-off disconnections and spontaneous connection drops, ensuring that notification alerts and database flags behave exactly as specified.

**Why this priority**: Ensures that future refactoring or lifecycle changes will never reintroduce false alerts or suppress genuine lost-device alerts.

**Independent Test**: Execute the automated test suite verifying scenario (a) adapter turn-off suppresses alerts, and scenario (b) sudden drop triggers alerts.

**Acceptance Scenarios**:

1. **Given** a connected device, **When** `ACTION_STATE_CHANGED` (`STATE_TURNING_OFF`/`STATE_OFF`) is broadcast followed immediately by `ACTION_ACL_DISCONNECTED`, **Then** `isUnexpectedDisconnect` is `false` in the database and 0 notifications are posted.
2. **Given** a connected device with Bluetooth adapter active, **When** `ACTION_ACL_DISCONNECTED` is broadcast without prior adapter turn-off, **Then** `isUnexpectedDisconnect` is `true` in the database and 1 notification is posted.

---

### Edge Cases

- **Simultaneous Disconnection of Multiple Devices**: When a user turns off Bluetooth with 3 peripherals connected, all 3 disconnect broadcasts arrive almost simultaneously. All 3 must be correctly recognized as intentional (`isUnexpectedDisconnect = false`).
- **Lagged Disconnect Broadcast**: Under heavy CPU load or aggressive power saving, if the disconnect broadcast from the OS is delayed beyond the heuristic window after adapter turn-off, it may fall back to unexpected disconnect. The window must be generous enough (3–5 seconds) to accommodate normal OS queue delays.
- **Rapid Bluetooth Toggle (Off then On)**: If a user rapidly turns Bluetooth off and on, new connections must not be affected, and subsequent sudden disconnections must be evaluated fresh against the latest state change.
- **Adapter State Changed with STATE_ON / STATE_TURNING_ON**: Transitioning to ON must reset or invalidate any past adapter-off timestamp so that future unexpected disconnects are not mistakenly suppressed.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST intercept `BluetoothAdapter.ACTION_STATE_CHANGED` broadcasts in `BluetoothEventReceiver`.
- **FR-002**: When `BluetoothAdapter.ACTION_STATE_CHANGED` indicates `STATE_TURNING_OFF` or `STATE_OFF`, the system MUST record the timestamp of the adapter turn-off event.
- **FR-003**: When a disconnection event occurs (`BluetoothDevice.ACTION_ACL_DISCONNECTED` or profile `CONNECTION_STATE_CHANGED` transitioning to `STATE_DISCONNECTED`), the system MUST evaluate whether an adapter turn-off occurred within the heuristic window (`ADAPTER_OFF_HEURISTIC_WINDOW_MS`).
- **FR-004**: If an adapter turn-off occurred within the heuristic window, the system MUST classify the disconnection as intentional (`isUnexpectedDisconnect = false`) and suppress the disconnection alert notification.
- **FR-005**: If no adapter turn-off occurred within the heuristic window, the system MUST classify the disconnection as unexpected (`isUnexpectedDisconnect = true`) and display the disconnection alert notification if alerts are enabled in settings.
- **FR-006**: The heuristic window MUST be defined as a named constant (e.g. 3000ms to 5000ms) and documented in code comments detailing the trade-offs and rationale.
- **FR-007**: When `BluetoothAdapter.ACTION_STATE_CHANGED` indicates `STATE_ON` or `STATE_TURNING_ON`, the system MUST clear/reset any prior adapter-off timestamp.
- **FR-008**: The system MUST include automated tests for:
  - (a) Manual adapter shut-down followed by peripheral disconnect -> `isUnexpectedDisconnect = false`, 0 alert notifications.
  - (b) Unexpected peripheral disconnect while adapter remains active -> `isUnexpectedDisconnect = true`, 1 alert notification.

### Key Entities

- **EventEntity**: Stores the `is_unexpected_disconnect` boolean flag alongside `eventType = "DISCONNECT"`, device identifiers, and location.
- **BluetoothAdapterState**: Represents the phone's Bluetooth radio power states (`STATE_OFF`, `STATE_TURNING_OFF`, `STATE_ON`, `STATE_TURNING_ON`).
- **BluetoothEventReceiver**: The receiver capturing both device connection transitions and adapter state changes, applying the heuristic to classify disconnect intent.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% elimination of false-alarm disconnect notifications when user intentionally turns off phone Bluetooth (0 false alerts).
- **SC-002**: 100% retention of legitimate disconnect notifications when peripheral drops connection while Bluetooth adapter is active.
- **SC-003**: Disconnect classification decision completes in under 5 milliseconds during broadcast processing without blocking main thread.
- **SC-004**: 100% pass rate on automated tests covering both intentional and accidental disconnect flows.

## Assumptions

- An in-memory timestamp stored within `BluetoothEventReceiver` (or in a companion object / singleton / application context) is sufficient because `BluetoothWatcherService` keeps the process alive while background tracking is enabled.
- 4000ms–5000ms is an optimal heuristic window across Android versions: sufficiently long to encompass OS intent dispatch jitter during radio shutdown, but short enough that an unrelated sudden disconnect hours later will never be misclassified.
- Turning off Bluetooth by the user from Quick Settings, Settings app, or Airplane Mode always broadcasts `BluetoothAdapter.ACTION_STATE_CHANGED` with `STATE_TURNING_OFF` or `STATE_OFF`.
