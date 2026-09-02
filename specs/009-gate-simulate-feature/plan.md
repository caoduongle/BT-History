# Implementation Plan: Gate Simulation Controls & Developer Mode Protection

**Branch**: `009-gate-simulate-feature` | **Date**: 2026-09-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/009-gate-simulate-feature/spec.md`

---

## Summary

This plan gates simulation tools and mock event injection in production release builds while providing internal QA/devs with a friction-free easter egg unlock:
1. **Persistent Developer Flag**: Add `KEY_DEVELOPER_MODE_ENABLED` in `PreferencesRepository` (default `false`).
2. **ViewModel Exposure**: Expose `isDeveloperModeEnabled` and `isSimulationAvailable` (`BuildConfig.DEBUG || isDeveloperModeEnabled`) in `DeviceViewModel`.
3. **UI Gating**:
   - `DeviceListScreen`: Conditionally show FAB "Mô phỏng" (`simulate_fab`) and "Thêm dữ liệu mẫu để thử nghiệm" button only when `isSimulationAvailable` is `true`.
   - `DeviceDetailScreen`: Conditionally show "Mô phỏng Kết nối" and "Mô phỏng Ngắt kết nối" overflow menu actions only when `isSimulationAvailable` is `true`.
4. **Easter Egg Activation in Settings**:
   - Display App Version at the bottom of `SettingsScreen`.
   - Tapping 7 times within a 3-second consecutive window activates Developer Mode, displaying Android standard countdown toasts and an activation toast.
5. **Preserved Backend Logic**: Retain `DeviceViewModel.simulateTestEvent()` unchanged for testability.
6. **Automated Robolectric Tests**: Write `SimulationGatingTest` to assert that simulation UI elements are hidden when developer mode is off and displayed when enabled.
7. **Documentation**: Update `README.md` to document the 7-tap gesture for emulator testing.

---

## Technical Context

**Language/Version**: Kotlin 2.2.10, Java 17  
**Primary Dependencies**: Android Jetpack (Compose, Material 3, DataStore Preferences, ViewModel, Coroutines)  
**Testing**: JUnit 4 (4.13.2), Robolectric (4.16.1), Compose UI Testing, AndroidX Test Core  
**Target Platform**: Android (minSdk 24, compileSdk 36, targetSdk 36)  
**Build Configuration**: `BuildConfig.DEBUG` flag from Android Gradle Plugin  
**Project Type**: Native Android Application (Jetpack Compose + Bento UI)  
**Constraints**: 0 mock buttons visible in release by default; 100% test pass rate; no regression to existing 32+ tests.

---

## Constitution Check

- **Data Integrity**: PASS. Production release users cannot inadvertently pollute their actual Bluetooth history with fake devices and fabricated GPS points.
- **Principle of Least Astonishment & Industry Convention**: PASS. Following the official Android "Build Number / Version 7 taps" convention for developer options provides immediate familiarity to QA and Android engineers.
- **Test Integrity**: PASS. Mock event engine is preserved for automated testing and internal QA emulator verification.

---

## Project Structure

### Documentation (this feature)

```text
specs/009-gate-simulate-feature/
├── spec.md                                     # Feature specification
├── plan.md                                     # Implementation plan (this file)
├── research.md                                 # Technical research & architectural decisions
├── data-model.md                               # DataStore preference schema & state transitions
├── quickstart.md                               # Validation guide & test instructions
├── contracts/
│   └── simulation-gating-contract.md           # API signatures & UI component contracts
└── checklists/
    └── requirements.md                         # Spec quality checklist
```

### Source Code Touchpoints

```text
app/src/main/java/com/example/
├── data/repository/
│   └── PreferencesRepository.kt                # Add KEY_DEVELOPER_MODE_ENABLED & Flow
├── ui/viewmodel/
│   └── DeviceViewModel.kt                      # Expose isDeveloperModeEnabled & isSimulationAvailable
├── ui/screens/
│   ├── DeviceListScreen.kt                     # Gate simulate_fab & empty state sample button
│   ├── DeviceDetailScreen.kt                   # Gate simulation overflow menu items
│   └── SettingsScreen.kt                       # Add version row & 7-tap unlock gesture
└── ...

app/src/main/res/values/
└── strings.xml                                 # Toast & version labels

app/src/test/java/com/example/
└── SimulationGatingTest.kt                     # Robolectric test for gating & unlock flow

README.md                                       # Update developer testing documentation
```

---

## Architecture & Component Breakdown

### 1. Data Layer (`PreferencesRepository.kt`)
- Key: `booleanPreferencesKey("developer_mode_enabled")`
- Flow: `isDeveloperModeEnabledFlow: Flow<Boolean>` defaulting to `false`
- Function: `suspend fun setDeveloperModeEnabled(enabled: Boolean)`

### 2. ViewModel Layer (`DeviceViewModel.kt`)
- `val isDeveloperModeEnabled: StateFlow<Boolean>`
- `val isSimulationAvailable: StateFlow<Boolean>` computed as:
  ```kotlin
  val isSimulationAvailable: StateFlow<Boolean> = isDeveloperModeEnabled
      .map { it || BuildConfig.DEBUG }
      .stateIn(viewModelScope, SharingStarted.Eagerly, BuildConfig.DEBUG)
  ```
- `fun setDeveloperModeEnabled(enabled: Boolean)`

### 3. UI Layer
- **`DeviceListScreen.kt`**:
  ```kotlin
  val isSimulationAvailable by viewModel.isSimulationAvailable.collectAsStateWithLifecycle()
  // FAB
  if (isSimulationAvailable) {
      FloatingActionButton(testTag = "simulate_fab", ...)
  }
  // Empty State
  if (isSimulationAvailable) {
      OutlinedButton(onClick = { showSimulateDialog = true }, ...) {
          Text(stringResource(R.string.btn_add_sample_data))
      }
  }
  ```
- **`DeviceDetailScreen.kt`**:
  ```kotlin
  if (isSimulationAvailable) {
      DropdownMenuItem(text = { Text(stringResource(R.string.menu_simulate_connect)) }, ...)
      DropdownMenuItem(text = { Text(stringResource(R.string.menu_simulate_disconnect)) }, ...)
  }
  ```
- **`SettingsScreen.kt`**:
  - Version row at bottom displaying "Phiên bản ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})".
  - Tap handler tracking click timestamp and consecutive count.
  - Displays Toast:
    - Clicks 4 to 6: "Bạn còn ${7 - count} lần nhấn nữa để bật chế độ nhà phát triển."
    - Click 7: "Đã bật chế độ nhà phát triển! Các công cụ mô phỏng đã sẵn sàng."
    - Already enabled: "Bạn đã là nhà phát triển rồi!"

### 4. Test Verification Layer
- `SimulationGatingTest.kt`:
  - Test 1: When `isDeveloperModeEnabled == false` and non-debug simulated, FAB and menu items are not present.
  - Test 2: When `isDeveloperModeEnabled == true`, FAB and menu items are visible.
  - Test 3: Unlocking via `setDeveloperModeEnabled(true)` updates preference and state reactively.

---

## Verification Plan

### Automated Tests
```bash
./gradlew.bat testDebugUnitTest
```
- Verify all existing 32 tests pass.
- Verify new `SimulationGatingTest` passes.

### Manual Verification
- Launch app in release mode -> confirm no "Mô phỏng" FAB is shown.
- Navigate to Settings -> tap App Version 7 times -> confirm Toast appears and Developer Mode is enabled.
- Return to Device List -> confirm "Mô phỏng" FAB appears and opens dialog.
