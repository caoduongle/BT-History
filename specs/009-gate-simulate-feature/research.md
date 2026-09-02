# Technical Research: Simulation Control Gating & Developer Mode Easter Egg

**Feature**: `009-gate-simulate-feature`  
**Date**: 2026-09-02  

---

## 1. Problem Analysis & Risk Assessment

### Current Vulnerability
In the existing codebase:
- `DeviceListScreen.kt` unconditionally instantiates `FloatingActionButton(..., modifier = Modifier.testTag("simulate_fab"))`.
- In the empty state of `DeviceListScreen.kt`:
  ```kotlin
  OutlinedButton(
      onClick = { showSimulateDialog = true },
      ...
  ) {
      Text(stringResource(R.string.btn_add_sample_data), ...)
  }
  ```
- In `DeviceDetailScreen.kt`:
  ```kotlin
  DropdownMenuItem(
      text = { Text(stringResource(R.string.menu_simulate_connect), ...) },
      onClick = { viewModel.simulateTestEvent(...) }
  )
  DropdownMenuItem(
      text = { Text(stringResource(R.string.menu_simulate_disconnect), ...) },
      onClick = { viewModel.simulateTestEvent(...) }
  )
  ```
- Because neither `BuildConfig.DEBUG` nor any permission/preference guard is applied, any user installing a release APK can click these buttons, creating synthetic devices with fake MAC addresses and fake locations in their device history.

### Risks
1. **Data Integrity Violation**: Artificial data mixes with real Bluetooth hardware logs.
2. **User Confusion**: Non-technical users might click "Mô phỏng" out of curiosity and think their device is connected or disconnected erratically.
3. **App Store Review Flag**: Reviewers may flag non-functional or debug tools in production release builds.

---

## 2. Industry Standards: Android Developer Options Easter Egg

### Convention
In Android OS:
- Settings > About phone > Build number:
  - Tapping 7 times activates "Developer options".
  - Taps 4 through 6 display countdown toasts: "You are now X steps away from being a developer."
  - Tap 7 displays: "You are now a developer!"
  - Subsequent taps display: "No need, you are already a developer."

### Why This Pattern Fits BT-History
- **Invisible to Regular Users**: Normal users do not rapidly tap version labels in Settings.
- **Immediate Familiarity**: Android QA testers and developers already know this pattern by heart.
- **Release APK Compatibility**: Allows QA to test signed production release builds on emulators (which lack physical Bluetooth radios) without requiring a special debug build flavor.

---

## 3. Implementation Alternatives Considered

| Alternative | Pros | Cons | Decision |
|:---|:---|:---|:---|
| **Option A: Pure `BuildConfig.DEBUG` (Strip entirely in release)** | Very simple; impossible for release users to see. | Internal QA cannot test release APKs on emulators. | **Rejected** - QA needs release verification on emulators. |
| **Option B: Visible Developer Mode switch in Settings** | Easy to find. | Regular users can see and toggle it, defeating the purpose of hiding debug tools. | **Rejected** - Must be hidden. |
| **Option C: `BuildConfig.DEBUG || developerModeEnabled` with 7-tap Easter Egg** | Clean release UI by default; QA can unlock on release APK; zero extra clicks for debug builds. | Requires handling tap counter in Settings. | **Selected** - Best balance of production cleanliness and QA flexibility. |

---

## 4. Compose UI State Management for 7-Tap Gesture

```kotlin
var clickCount by remember { mutableIntStateOf(0) }
var lastClickTime by remember { mutableLongStateOf(0L) }

val onVersionClick = {
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastClickTime > 3000L) {
        clickCount = 1
    } else {
        clickCount++
    }
    lastClickTime = currentTime

    if (isDeveloperModeEnabled) {
        Toast.makeText(context, context.getString(R.string.toast_dev_mode_already_active), Toast.LENGTH_SHORT).show()
    } else if (clickCount >= 7) {
        viewModel.setDeveloperModeEnabled(true)
        clickCount = 0
        Toast.makeText(context, context.getString(R.string.toast_dev_mode_activated), Toast.LENGTH_SHORT).show()
    } else if (clickCount >= 4) {
        val remaining = 7 - clickCount
        val message = context.resources.getQuantityString(R.plurals.toast_dev_mode_countdown, remaining, remaining)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
```
