# Feature Specification: Gate Simulation Controls & Developer Mode Protection

**Feature Branch**: `009-gate-simulate-feature`  
**Created**: 2026-09-02  
**Status**: Draft  
**Input**: User description: "FAB \"Mô phỏng\" + SimulateEventDialog trong DeviceListScreen.kt, menu \"Mô phỏng Kết nối\"/\"Mô phỏng Ngắt kết nối\" trong DeviceDetailScreen.kt, và DeviceViewModel.simulateTestEvent() cho phép người dùng cuối tự tạo thiết bị + sự kiện + toạ độ GPS GIẢ thẳng vào lịch sử thật, không có BuildConfig.DEBUG hay cờ ẩn nào bảo vệ. README mô tả đây là tính năng chỉ để test trên Emulator, nhưng hiện đang được build vào bản release cho mọi người dùng. Hãy: 1. Ẩn hoàn toàn FAB \"Mô phỏng\" (testTag \"simulate_fab\"), nút \"Thêm dữ liệu mẫu để thử nghiệm\", và 2 menu item mô phỏng trong DeviceDetailScreen khi app chạy ở bản release, dùng BuildConfig.DEBUG làm điều kiện mặc định. 2. Nếu cần dùng được cả trên bản release cho QA nội bộ: thêm 1 cờ \"developerModeEnabled\" trong PreferencesRepository (mặc định false, không lộ ra UI thường), chỉ bật được qua thao tác ẩn (ví dụ bấm 7 lần vào dòng phiên bản app trong SettingsScreen — theo đúng convention \"Developer options\" của Android), và chỉ hiển thị các control mô phỏng khi (BuildConfig.DEBUG || developerModeEnabled) là true. 3. Giữ nguyên DeviceViewModel.simulateTestEvent() cho mục đích test, chỉ ẩn lối vào UI, không xoá logic. 4. Viết test (Robolectric/Compose UI test) xác nhận: khi developer mode tắt và không phải debug build, FAB/menu mô phỏng không được render. 5. Cập nhật README mô tả cách bật developer mode để test trên Emulator, thay đoạn mô tả hiện tại đang ngầm coi đây là tính năng public. 6. Tạo specs/009-gate-simulate-feature theo khuôn Spec Kit hiện có (spec.md, plan.md, tasks.md)."

---

## Problem & Root Cause Summary

1. **Unprotected Mock Event Controls in Production**:
   - The FloatingActionButton "Mô phỏng" (`testTag("simulate_fab")`) and `SimulateEventDialog` on `DeviceListScreen.kt`.
   - The empty-state action button "Thêm dữ liệu mẫu để thử nghiệm" (`btn_add_sample_data`) on `DeviceListScreen.kt`.
   - The overflow menu actions "Mô phỏng Kết nối" and "Mô phỏng Ngắt kết nối" on `DeviceDetailScreen.kt`.
   All of these UI controls are currently unconditionally rendered for all users, including production release builds.
2. **Pollution of Real User History**:
   - End users can tap these controls to generate synthetic devices, artificial connection timestamps, and fake GPS coordinates directly into their permanent SQLite Room database.
   - This compromises data integrity and user trust in real-world tracking logs.
3. **Lack of Internal QA Access Control**:
   - While README mentions that simulation is intended for testing on Android Emulators lacking physical Bluetooth hardware, no gating mechanism exists to differentiate between ordinary release users and QA/developers testing release builds.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Clean Production Experience for End Users (Priority: P1) 🎯 MVP

As an end user running a production release build of BT Watcher, I want the UI to be free of debugging and simulation controls (no "Mô phỏng" FAB, no "Thêm dữ liệu mẫu" button, no simulation menu options), so that the application only logs authentic Bluetooth events and presents a clean, professional interface.

**Why this priority**: Production builds must never leak testing tools that allow fake data injection. Gating these controls by default on non-debug builds eliminates data corruption risks.

**Independent Test**: Launch the app in release configuration (or with `developerModeEnabled = false` in tests). Observe `DeviceListScreen` and `DeviceDetailScreen`. Verify the "Mô phỏng" FAB, "Thêm dữ liệu mẫu" button, and simulation menu items are completely absent from the UI hierarchy.

**Acceptance Scenarios**:

1. **Given** an app running in release mode with `developerModeEnabled == false`, **When** the user views `DeviceListScreen`, **Then** the "Mô phỏng" FAB (`testTag("simulate_fab")`) is NOT rendered.
2. **Given** an app running in release mode with `developerModeEnabled == false`, **When** the device list is empty, **Then** the "Thêm dữ liệu mẫu để thử nghiệm" button is NOT displayed in the empty state.
3. **Given** an app running in release mode with `developerModeEnabled == false`, **When** the user opens `DeviceDetailScreen` and taps the overflow menu, **Then** "Mô phỏng Kết nối" and "Mô phỏng Ngắt kết nối" items are NOT present in the menu.

---

### User Story 2 - Hidden Developer Mode Unlock in Settings (Priority: P1) 🎯 MVP

As a QA engineer or developer testing a release build on an Android Emulator or physical device, I want to unlock Developer Mode by tapping the App Version row 7 times in `SettingsScreen` (following Android OS Developer Options conventions), so that I can access simulation tools without requiring a separate debug APK.

**Why this priority**: Internal QA teams often need to test production release builds on emulators where real Bluetooth peripherals are unavailable. An easter egg unlock provides access without compromising security for ordinary users.

**Independent Test**: Open `SettingsScreen`, locate the App Version row at the bottom, and tap it repeatedly. Verify that countdown toasts appear on taps 4 through 6, a success toast appears on the 7th tap, and `developerModeEnabled` is persisted in preferences.

**Acceptance Scenarios**:

1. **Given** `developerModeEnabled == false`, **When** the user taps the App Version row in `SettingsScreen` 4 to 6 times within a short time window, **Then** the system displays a transient toast indicating the remaining taps needed (e.g., "Bạn còn X lần nhấn nữa để bật chế độ nhà phát triển").
2. **Given** the user reaches the 7th tap within the threshold, **When** tapped, **Then** `developerModeEnabled` is set to `true` in `PreferencesRepository`, and a confirmation toast is displayed (e.g., "Đã bật chế độ nhà phát triển!").
3. **Given** `developerModeEnabled == true`, **When** the user taps the App Version row again, **Then** a toast informs them "Bạn đã là nhà phát triển rồi!".
4. **Given** Developer Mode is activated, **When** returning to `DeviceListScreen` and `DeviceDetailScreen`, **Then** all simulation controls (FAB, empty state sample button, detail menu items) become visible and functional.

---

### User Story 3 - Preserved Core Simulation Logic (Priority: P2)

As an automated test suite or internal QA tester with Developer Mode enabled, I want `DeviceViewModel.simulateTestEvent()` to function identically to its existing implementation, so that backend simulation capabilities remain 100% operational for regression and UI testing.

**Why this priority**: The goal is to gate UI access, not remove simulation logic. Keeping `simulateTestEvent()` intact prevents breaking existing integration tests or developer workflows.

**Independent Test**: Trigger `viewModel.simulateTestEvent(...)` programmatically or via the unlocked UI dialog. Verify that mock devices, connection events, and coordinates are inserted into Room database and state flows update normally.

**Acceptance Scenarios**:

1. **Given** Developer Mode is enabled or running in a debug build, **When** selecting a mock device and event type in `SimulateEventDialog`, **Then** `DeviceViewModel.simulateTestEvent()` executes normally and updates the database.
2. **Given** a direct programmatic call to `DeviceViewModel.simulateTestEvent()` in unit tests, **When** invoked, **Then** the event is successfully recorded regardless of UI gating flags.

---

### User Story 4 - Automated Testing & Documentation Verification (Priority: P1)

As a maintainer and QA engineer, I want automated Robolectric/Compose UI tests confirming that simulation controls are hidden when developer mode is off and revealed when developer mode is on, and I want `README.md` updated to document the developer mode unlock procedure for emulator testing.

**Why this priority**: Automated tests prevent future regressions where debug buttons accidentally reappear in release builds. Clear documentation guides QA and developers on how to test without confusing end users.

**Independent Test**: Run `./gradlew.bat testDebugUnitTest` and check that UI gating tests pass. Read `README.md` to verify the new Developer Mode unlock guide.

**Acceptance Scenarios**:

1. **Given** a Robolectric UI test with `developerModeEnabled = false` and non-debug mode simulated, **When** `DeviceListScreen` is rendered, **Then** `onNodeWithTag("simulate_fab")` does not exist.
2. **Given** `developerModeEnabled = true`, **When** `DeviceListScreen` is rendered, **Then** `onNodeWithTag("simulate_fab")` is visible and clickable.
3. **Given** the repository `README.md`, **When** viewing the testing section, **Then** it clearly explains that simulation controls require enabling Developer Mode via Settings (7 taps on version).

---

## Edge Cases

- **Rapid Tapping Delay**: If the user pauses for more than 3 seconds between taps, the tap counter resets to zero to prevent accidental activation from spread-out taps.
- **Persistence Across App Restarts**: Once `developerModeEnabled` is set to `true` in `PreferencesRepository`, it persists across process recreation and device reboots until app data is cleared.
- **Debug vs. Release Builds**: On `BuildConfig.DEBUG == true`, simulation controls are ALWAYS visible by default, avoiding extra clicks for active developers during local debug development. The condition is `(BuildConfig.DEBUG || isDeveloperModeEnabled)`.
- **Empty State Layout Integrity**: When the device list is empty in release mode without developer mode, the empty-state card gracefully hides the "Thêm dữ liệu mẫu" button without leaving broken whitespace or layout misalignment.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: `PreferencesRepository` MUST define a persistent boolean preference `KEY_DEVELOPER_MODE_ENABLED` (defaulting to `false`) with an exposed Flow `isDeveloperModeEnabledFlow` and update function `setDeveloperModeEnabled(Boolean)`.
- **FR-002**: `DeviceViewModel` MUST expose an observable state `isDeveloperModeEnabled: StateFlow<Boolean>` and an aggregated state `isSimulationAvailable: StateFlow<Boolean>` (or computed via `BuildConfig.DEBUG || isDeveloperModeEnabled`).
- **FR-003**: `DeviceListScreen` MUST conditionally render the "Mô phỏng" FloatingActionButton (`testTag("simulate_fab")`) ONLY when `isSimulationAvailable` is `true`.
- **FR-004**: `DeviceListScreen` MUST conditionally render the "Thêm dữ liệu mẫu để thử nghiệm" button in the empty state ONLY when `isSimulationAvailable` is `true`.
- **FR-005**: `DeviceDetailScreen` MUST conditionally render the "Mô phỏng Kết nối" and "Mô phỏng Ngắt kết nối" dropdown menu items ONLY when `isSimulationAvailable` is `true`.
- **FR-006**: `SettingsScreen` MUST display an App Version item/row at the bottom of the screen.
- **FR-007**: Tapping the App Version item in `SettingsScreen` 7 times within a 3-second consecutive window MUST toggle `developerModeEnabled` to `true`, displaying countdown Toasts on taps 4–6 and an activation Toast on tap 7. Subsequent taps when already enabled MUST display an informational Toast ("Bạn đã là nhà phát triển rồi!").
- **FR-008**: `DeviceViewModel.simulateTestEvent()` MUST be retained unchanged to support mock testing and QA workflows.
- **FR-009**: Automated Robolectric / Compose UI tests MUST verify that simulation controls are not rendered when `isSimulationAvailable` is `false`, and ARE rendered when `isSimulationAvailable` is `true`.
- **FR-010**: `README.md` MUST be updated to document the Developer Mode 7-tap gesture for testing on Android Emulators.

---

### Key Entities

- **`PreferencesRepository`**: Persistent DataStore storage managing settings flags (`KEY_DEVELOPER_MODE_ENABLED`).
- **`DeviceViewModel`**: Exposes `isDeveloperModeEnabled` and `isSimulationAvailable` to UI screens.
- **`SettingsScreen`**: Renders version info and captures the 7-tap easter egg gesture to toggle Developer Mode.
- **`DeviceListScreen`**: Gated dashboard with conditional FAB and sample data button.
- **`DeviceDetailScreen`**: Gated device detail with conditional simulation menu actions.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: **0 Unintended Simulation Controls in Release**: On production release builds with default preferences, 0 simulation buttons, FABs, or menu items are visible or accessible to end users.
- **SC-002**: **100% Easter Egg Success**: Tapping 7 times on the version label in Settings reliably unlocks Developer Mode and exposes simulation controls without app restart.
- **SC-003**: **0 Breaking Changes to Existing Tests**: All existing 32+ unit tests continue to pass with 0 failures (`./gradlew.bat testDebugUnitTest`).
- **SC-004**: **100% Pass Rate for New Gating Tests**: New Robolectric tests validating visibility under enabled/disabled developer mode pass with 0 failures.

---

## Assumptions & Dependencies

- `BuildConfig.DEBUG` is generated by Android Gradle Plugin and evaluates to `true` on debug build variants and `false` on release build variants.
- The 7-tap gesture on version is the well-known Android standard for developer option activation, minimizing user friction for QA teams while remaining completely invisible to everyday users.
