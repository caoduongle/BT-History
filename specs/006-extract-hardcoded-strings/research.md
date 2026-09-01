# Technical Research: String Extraction & Localization Catalog

**Feature**: Extract Hardcoded Strings to Resource Catalog (`006-extract-hardcoded-strings`)  
**Status**: Completed  
**Date**: 2026-09-02  

---

## 1. Inventory & Categorization of Hardcoded Strings

A thorough scan of `app/src/main/` identified the following string categories and target keys:

### 1.1 Device & Entity Defaults
| Source Location | Original String Literal | Proposed String Key | Value |
| :--- | :--- | :--- | :--- |
| `DeviceRepository.kt`, `BluetoothHelper.kt` | `"Thiết bị không rõ"` | `device_unknown_name` | "Thiết bị không rõ" |
| `DeviceRepository.kt`, `BluetoothHelper.kt` | `"Thiết bị Bluetooth"` | `device_default_name` | "Thiết bị Bluetooth" |
| `BluetoothHelper.kt` | `"Thiết bị (%s)"` | `device_name_with_mac_suffix` | "Thiết bị (%s)" |
| `BluetoothHelper.kt` | `"Tai nghe/Loa"` | `device_type_audio` | "Tai nghe/Loa" |
| `BluetoothHelper.kt` | `"Đồng hồ thông minh"` | `device_type_watch` | "Đồng hồ thông minh" |
| `BluetoothHelper.kt` | `"Điện thoại"` | `device_type_phone` | "Điện thoại" |
| `BluetoothHelper.kt` | `"Khác"` | `device_type_other` | "Khác" |

### 1.2 System Notifications & Channels (`NotificationHelper.kt`)
| Original String Literal | Proposed String Key | Value / Format |
| :--- | :--- | :--- |
| `"Dịch vụ giám sát BT Watcher"` | `notif_channel_service_name` | "Dịch vụ giám sát BT Watcher" |
| `"Thông báo dịch vụ nền giám sát kết nối Bluetooth và vị trí GPS"` | `notif_channel_service_desc` | "Thông báo dịch vụ nền giám sát..." |
| `"Cảnh báo ngắt kết nối thiết bị"` | `notif_channel_alert_name` | "Cảnh báo ngắt kết nối thiết bị" |
| `"Cảnh báo ngay khi thiết bị Bluetooth bị mất kết nối kèm vị trí GPS"` | `notif_channel_alert_desc` | "Cảnh báo ngay khi thiết bị..." |
| `"Đang sẵn sàng ghi nhận sự kiện"` | `notif_service_default_last_event` | "Đang sẵn sàng ghi nhận sự kiện" |
| `"Dịch vụ đang hoạt động"` | `notif_service_title` | "Dịch vụ đang hoạt động" |
| `"Đang kết nối: %d thiết bị"` | `notif_service_connected_count` | "Đang kết nối: %d thiết bị" |
| `"Đang kết nối: %1$d thiết bị\nSự kiện gần nhất: %2$s"` | `notif_service_big_text` | "Đang kết nối: %1$d thiết bị\nSự kiện gần nhất: %2$s" |
| `"⚠️ Thiết bị vừa ngắt kết nối!"` | `notif_alert_title` | "⚠️ Thiết bị vừa ngắt kết nối!" |
| `"%1$s đã ngắt kết nối tại: %2$s"` | `notif_alert_content` | "%1$s đã ngắt kết nối tại: %2$s" |
| Multi-line alert details | `notif_alert_big_text` | "Thiết bị \"%1$s\" đã bị ngắt kết nối.\n📍 Vị trí ghi nhận: %2$s\n⏰ Thời gian: %3$s" |

### 1.3 Time & Location Formatter (`TimeFormatter.kt`, `LocationHelper.kt`)
| Source Location | Original String Literal | Proposed String Key | Value / Format |
| :--- | :--- | :--- | :--- |
| `LocationHelper.kt`, `NotificationHelper.kt` | `"Chưa xác định tọa độ GPS"` | `location_unknown_coordinates` | "Chưa xác định tọa độ GPS" |
| `LocationHelper.kt` | `"Không thể lấy vị trí hiện tại"` | `location_fetch_error` | "Không thể lấy vị trí hiện tại" |
| `TimeFormatter.kt` | `"Vừa xong"` | `time_just_now` | "Vừa xong" |
| `TimeFormatter.kt` | `"%d phút trước"` | `time_minutes_ago` | "%d phút trước" |
| `TimeFormatter.kt` | `"%d giờ trước"` | `time_hours_ago` | "%d giờ trước" |
| `TimeFormatter.kt` | `"Hôm qua, %s"` | `time_yesterday` | "Hôm qua, %s" |

### 1.4 ViewModel Enums & Filter Chips (`DeviceViewModel.kt`)
| Filter Enum | Display Text | Proposed String Key |
| :--- | :--- | :--- |
| `TimeFilter.ALL` | "Tất cả" | `filter_all` |
| `TimeFilter.TODAY` | "Hôm nay" | `filter_today` |
| `TimeFilter.LAST_24_HOURS` | "24 giờ qua" | `filter_last_24_hours` |
| `TimeFilter.LAST_7_DAYS` | "7 ngày qua" | `filter_last_7_days` |

### 1.5 UI Screens & Components
- **`PermissionOnboardingScreen.kt`**:
  - `onboarding_title`, `onboarding_subtitle`, `onboarding_bt_title`, `onboarding_bt_desc`, `onboarding_loc_title`, `onboarding_loc_desc`, `onboarding_notif_title`, `onboarding_notif_desc`, `onboarding_grant_button`, `onboarding_privacy_note`.
- **`DeviceListScreen.kt`**:
  - `screen_devices_title`, `tab_all_devices`, `tab_connected_devices`, `search_placeholder`, `empty_devices_title`, `empty_devices_desc`, `status_connected`, `status_disconnected`.
- **`DeviceDetailScreen.kt`**:
  - `screen_device_detail_title`, `detail_section_status`, `detail_section_events`, `btn_open_maps`, `event_connected`, `event_disconnected`, `event_unexpected_disconnect`, `empty_events`.
- **`SettingsScreen.kt`**:
  - `screen_settings_title`, `setting_service_title`, `setting_service_desc`, `setting_alert_title`, `setting_alert_desc`, `setting_sync_button`, `setting_check_permissions_button`, `setting_about_title`, `setting_about_desc`.
- **`WarningBanner.kt`**:
  - `warning_banner_title`, `warning_banner_message`.

---

## 2. Design Decisions & Best Practices

1. **Naming Convention**:
   - Keys use lowercase snake_case with clear component/domain prefixes: `notif_*`, `device_*`, `filter_*`, `location_*`, `time_*`, `setting_*`, `onboarding_*`, `detail_*`.
2. **Context-Free Formatting**:
   - For `TimeFormatter`, overloaded functions accept `Context` or provide default fallback string constants while ensuring standard usages pass `Context`.
3. **Enum String Mapping**:
   - `TimeFilter` declares `@StringRes val titleRes: Int`. In Compose UI, chips call `stringResource(filter.titleRes)`.
4. **Data Isolation**:
   - Database columns (e.g. `eventType = "CONNECT"`, `deviceType = "AUDIO"`) remain raw data strings. Only when rendering them to users do helper mappers convert them to localized string resources.
