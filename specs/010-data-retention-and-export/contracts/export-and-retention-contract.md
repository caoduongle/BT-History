# Contract: Data Retention, History Pagination & Export

**Branch**: `010-data-retention-and-export`  
**Date**: 2026-09-02  

---

## 1. Preferences Repository Interface

```kotlin
interface IPreferencesRepository {
    // Flow emitting current retention setting in days (default 180, 0 = Unlimited)
    val historyRetentionDaysFlow: Flow<Int>

    // Persist new retention setting
    suspend fun setHistoryRetentionDays(days: Int)
}
```

---

## 2. ViewModel Contracts (`DeviceViewModel`)

```kotlin
// Data Retention State
val historyRetentionDays: StateFlow<Int>
fun setHistoryRetentionDays(days: Int)

// Pagination State for Global Timeline
val paginatedTimelineEvents: StateFlow<List<EventEntity>>
val hasMoreTimelineEvents: StateFlow<Boolean>
fun loadMoreTimelineEvents()
fun refreshTimelineEvents()

// Pagination State for Device Detail
val paginatedDeviceEvents: StateFlow<List<EventEntity>>
val hasMoreDeviceEvents: StateFlow<Boolean>
fun loadInitialDeviceEvents(deviceId: Long)
fun loadMoreDeviceEvents(deviceId: Long)

// Data Export & Maintenance
suspend fun exportHistoryToUri(uri: Uri, format: ExportFormat = ExportFormat.JSON): Result<ExportSummary>
suspend fun pruneExpiredEvents(): Int
```

### Export Models
```kotlin
enum class ExportFormat {
    JSON, CSV
}

data class ExportSummary(
    val deviceCount: Int,
    val eventCount: Int,
    val byteSize: Long
)
```

---

## 3. UI Component Contracts

### `SettingsScreen`
- **Retention Row**:
  - Displays title: `"Thời gian lưu lịch sử"`
  - Subtitle: Current setting label (e.g., `"180 ngày (Mặc định)"`, `"Không giới hạn"`, etc.)
  - Tapping opens single-choice dialog or dropdown with options:
    - 30 ngày
    - 90 ngày
    - 180 ngày (Mặc định)
    - 1 năm (365 ngày)
    - Không giới hạn
  - `testTag`: `"setting_retention_period"`
- **Export Row**:
  - Displays title: `"Xuất lịch sử ra file"`
  - Subtitle: `"Lưu trữ bản sao dữ liệu thiết bị và toạ độ"`
  - Tapping launches Android SAF `CreateDocument` launcher.
  - `testTag`: `"setting_export_history"`

### `ClearAllHistoryDialog`
- Confirmation dialog includes two distinct primary options:
  1. `"Xuất dữ liệu trước"` (`testTag("btn_export_before_clear")`): Opens SAF file export flow.
  2. `"Xoá toàn bộ"` (`testTag("btn_confirm_clear_all")`): Executes permanent wipe.
