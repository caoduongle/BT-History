# Contract: Simulation Gating & Developer Mode Interfaces

**Feature**: `009-gate-simulate-feature`  
**Date**: 2026-09-02  

---

## 1. PreferencesRepository Contract

```kotlin
interface PreferencesRepositoryContract {
    /**
     * Observable flow emitting the current developer mode state.
     * Defaults to false in persistent storage.
     */
    val isDeveloperModeEnabledFlow: Flow<Boolean>

    /**
     * Persists the developer mode state to DataStore.
     */
    suspend fun setDeveloperModeEnabled(enabled: Boolean)
}
```

---

## 2. DeviceViewModel Contract

```kotlin
interface DeviceViewModelGatingContract {
    /**
     * Flow tracking whether developer mode has been unlocked via preferences.
     */
    val isDeveloperModeEnabled: StateFlow<Boolean>

    /**
     * Aggregate availability flow: true if BuildConfig.DEBUG is true OR developer mode is active.
     */
    val isSimulationAvailable: StateFlow<Boolean>

    /**
     * Enables or disables developer mode.
     */
    fun setDeveloperModeEnabled(enabled: Boolean)

    /**
     * Core simulation engine: MUST be preserved for test and emulator verification.
     */
    fun simulateTestEvent(
        deviceName: String,
        macAddress: String,
        deviceType: DeviceType,
        isConnect: Boolean,
        latitude: Double?,
        longitude: Double?
    )
}
```

---

## 3. UI Screen Contracts

### DeviceListScreen
- When `isSimulationAvailable == false`:
  - `onNodeWithTag("simulate_fab")` does NOT exist in the Compose hierarchy.
  - `onNodeWithText(R.string.btn_add_sample_data)` does NOT exist in the Compose hierarchy.
- When `isSimulationAvailable == true`:
  - `onNodeWithTag("simulate_fab")` is visible and clickable.
  - `onNodeWithText(R.string.btn_add_sample_data)` is visible and clickable when device list is empty.

### DeviceDetailScreen
- When `isSimulationAvailable == false`:
  - Overflow menu does NOT contain "Mô phỏng Kết nối" (`R.string.menu_simulate_connect`).
  - Overflow menu does NOT contain "Mô phỏng Ngắt kết nối" (`R.string.menu_simulate_disconnect`).
- When `isSimulationAvailable == true`:
  - Overflow menu contains both simulation actions.

### SettingsScreen
- Version row must display version name and code.
- Must have testTag `"settings_app_version"`.
- Clicking 7 times toggles `developerModeEnabled`.
