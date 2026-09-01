# Feature Specification: Extract Hardcoded Strings to Resource Catalog

**Feature Branch**: `006-extract-hardcoded-strings`

**Created**: 2026-09-02

**Status**: Draft

**Input**: User description: "Nhiều chuỗi tiếng Việt đang bị hard-code trực tiếp trong code Kotlin (ví dụ 'Thiết bị không rõ', 'Thiết bị Bluetooth', các nhãn trong TimeFilter, thông báo Toast trong LocationHelper, text trong NotificationHelper...) thay vì đặt trong app/src/main/res/values/strings.xml. Hãy: 1. Trích xuất toàn bộ string hiển thị cho người dùng (Toast, notification text, label UI, nhãn enum TimeFilter) sang strings.xml với tên key rõ nghĩa. 2. Thay các chỗ dùng string cứng bằng context.getString(R.string.xxx) hoặc stringResource(R.string.xxx) trong Compose. 3. Giữ nguyên hành vi hiện tại, không đổi nghĩa text."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Centralized UI & System Strings in Resource Catalog (Priority: P1)

As a developer and future translator of the BT-History application, I want all user-facing strings—including UI labels, headers, buttons, cards, notification channels, notification bodies, Toast messages, fallback device names, relative timestamps, and filter options—to be declared centrally in `app/src/main/res/values/strings.xml` with clear, semantic resource IDs.

**Why this priority**: Hardcoding string literals directly within Kotlin source code violates standard Android architecture, makes internationalization (i18n) and localization (l10n) difficult, causes duplicate string definitions, and prevents translators from working with standard XML resource catalogs.

**Independent Test**: Inspect `app/src/main/res/values/strings.xml` and all `.kt` files to verify that all user-visible text is accessed via `R.string.*` references; verify that UI screens, toasts, and notifications display the exact expected Vietnamese text.

**Acceptance Scenarios**:

1. **Given** the resource catalog `strings.xml`, **When** inspected, **Then** comprehensive string resources are declared with descriptive naming conventions (e.g. `device_default_name`, `device_unknown_name`, `notif_channel_service_name`, `filter_today`, `onboarding_grant_button`, etc.).
2. **Given** any Jetpack Compose screen or component (`DeviceListScreen`, `DeviceDetailScreen`, `SettingsScreen`, `PermissionOnboardingScreen`, `DeviceCard`, `TimelineItem`, `WarningBanner`), **When** displaying labels, **Then** `stringResource(R.string.xxx)` is used instead of hardcoded string literals.
3. **Given** system utility classes and services (`NotificationHelper`, `LocationHelper`, `BluetoothHelper`, `TimeFormatter`), **When** generating notifications, toasts, or fallback names, **Then** `context.getString(R.string.xxx, ...)` is used with appropriate string formatting.
4. **Given** the `TimeFilter` enum in `DeviceViewModel`, **When** rendered in UI filter chips, **Then** each enum value provides its title via a `@StringRes val titleRes: Int` parameter and renders via `stringResource(filter.titleRes)`.

---

### User Story 2 - Semantic & Functional Parity (Priority: P2)

As an app user, I want the application's appearance, notifications, device names, and messaging to remain completely identical in meaning and presentation before and after the resource extraction.

**Why this priority**: Refactoring string management must not accidentally alter grammar, punctuation, emojis, or formatting logic.

**Independent Test**: Run the full automated test suite (`./gradlew testDebugUnitTest`) including screenshot and Robolectric tests to verify zero regressions.

**Acceptance Scenarios**:

1. **Given** existing screenshot and unit tests, **When** running `./gradlew testDebugUnitTest`, **Then** all tests pass with zero assertion failures.
2. **Given** the app running on device, **When** reviewing notification channels and heads-up alerts, **Then** all texts match the original format, including parameters (device name, location address, formatted date/time).

---

### Edge Cases

- **Dynamic Plural / Time Formatting**: Relative time strings in `TimeFormatter` (e.g. `"Vừa xong"`, `"%d phút trước"`, `"%d giờ trước"`, `"Hôm qua"`) require parameterized formatting (e.g. `%d`).
- **Notification BigText Formatting**: Notification bodies containing multi-line formatted strings with placeholders (`%1$s`, `%2$s`, `%3$s`) must use properly escaped XML format strings.
- **Compose Preview & Non-Context Methods**: Any helper methods requiring strings without an explicit `Context` must accept `Context` or provide resource IDs directly to the calling Compose scope.
- **Test Invariants**: Automated tests checking hardcoded string assertions (e.g., `ExampleRobolectricTest` checking `R.string.app_name == "BT Watcher"`) must remain untouched and passing.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST extract all user-facing strings into `app/src/main/res/values/strings.xml` with clear, semantic resource IDs.
- **FR-002**: The system MUST replace hardcoded string literals in Jetpack Compose files with `stringResource(R.string.xxx)`.
- **FR-003**: The system MUST replace hardcoded string literals in non-Compose Kotlin files with `context.getString(R.string.xxx)` or pass `@StringRes` identifiers.
- **FR-004**: The system MUST update `TimeFilter` in `DeviceViewModel.kt` to declare `@StringRes val titleRes: Int` and map each filter (`ALL`, `TODAY`, `LAST_24_HOURS`, `LAST_7_DAYS`) to its respective resource ID.
- **FR-005**: The system MUST support parameterized string substitution in `strings.xml` using standard format specifiers (`%s`, `%d`, `%1$s`, etc.) for device names, coordinates, counts, and timestamps.
- **FR-006**: The system MUST retain 100% of the original Vietnamese text meanings, emojis, punctuation, and wording.
- **FR-007**: All automated unit tests and screenshot tests MUST continue to pass without error.

### Key Entities

- **app/src/main/res/values/strings.xml**: The centralized string resource catalog.
- **TimeFilter**: ViewModel enum defining time filter categories.
- **NotificationHelper**: Utility building foreground service and disconnect alert notifications.
- **BluetoothHelper**: Utility resolving device names and types.
- **LocationHelper**: Utility resolving GPS location and geocoding.
- **TimeFormatter**: Utility formatting relative timestamps and coordinates.
- **Compose UI Screens & Components**: Screens (`DeviceListScreen`, `DeviceDetailScreen`, `SettingsScreen`, `PermissionOnboardingScreen`) and UI widgets (`DeviceCard`, `TimelineItem`, `WarningBanner`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 0 user-facing Vietnamese string literals hardcoded in Kotlin source files.
- **SC-002**: 100% semantic and visual parity across all screens and notification templates.
- **SC-003**: 100% pass rate on all automated unit and UI tests (`./gradlew testDebugUnitTest`).
- **SC-004**: Clean build with zero resource resolution warnings.

## Assumptions

- Standard Android resource mechanisms (`context.getString` and Compose `stringResource`) provide complete coverage for all required user-facing text.
- Internal database constants (e.g., event types `"CONNECT"` and `"DISCONNECT"`, device types `"AUDIO"`, `"WATCH"`, `"OTHER"`) are protocol/data discriminators and remain unchanged in the data layer; only their presentation labels in UI are mapped to string resources.
