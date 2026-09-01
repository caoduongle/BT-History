# Implementation Plan: Extract Hardcoded Strings to Resource Catalog

**Branch**: `006-extract-hardcoded-strings` | **Date**: 2026-09-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/006-extract-hardcoded-strings/spec.md`

## Summary

Various Vietnamese text strings were previously hardcoded directly into Kotlin classes (e.g. `"Thiết bị không rõ"`, `"Thiết bị Bluetooth"`, `TimeFilter` labels, notification messages in `NotificationHelper`, Toast messages in `LocationHelper`, screen titles and button labels).

The implementation plan:
1. Populates `app/src/main/res/values/strings.xml` with comprehensive, semantic string resource keys covering all domains.
2. Updates `util/BluetoothHelper.kt`, `util/NotificationHelper.kt`, `util/LocationHelper.kt`, and `util/TimeFormatter.kt` to load strings via `context.getString(R.string.*)`.
3. Updates `ui/viewmodel/DeviceViewModel.kt` so that `TimeFilter` enum defines `@StringRes val titleRes: Int`.
4. Replaces hardcoded string literals across all Compose screens (`DeviceListScreen`, `DeviceDetailScreen`, `SettingsScreen`, `PermissionOnboardingScreen`) and UI components (`DeviceCard`, `TimelineItem`, `WarningBanner`) with `stringResource(R.string.*)`.
5. Validates changes using `./gradlew testDebugUnitTest` and `./gradlew assembleDebug`.

## Technical Context

**Language/Version**: Kotlin 2.2.10, Android Jetpack Compose Material 3

**Resource Management**: Android Resource System (`res/values/strings.xml`), `@StringRes`, `stringResource`

**Testing**: JUnit 4, Robolectric, Roborazzi Screenshot Tests

**Scope**:
- `app/src/main/res/values/strings.xml`
- Utilities: `BluetoothHelper.kt`, `NotificationHelper.kt`, `LocationHelper.kt`, `TimeFormatter.kt`
- ViewModel: `DeviceViewModel.kt`
- Repository: `DeviceRepository.kt`
- UI: `DeviceListScreen.kt`, `DeviceDetailScreen.kt`, `SettingsScreen.kt`, `PermissionOnboardingScreen.kt`, `DeviceCard.kt`, `TimelineItem.kt`, `WarningBanner.kt`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Maintainability & Localization**: Upheld. Centralizing strings is standard Android development practice and prepares the app for multi-language support.
- **Semantic Integrity**: Upheld. 100% of original Vietnamese text meanings and formats are preserved.
- **Verification**: Verified. Full regression test suite will run.
- **No Gate Violations**: All criteria satisfied.

## Project Structure

### Documentation (this feature)

```text
specs/006-extract-hardcoded-strings/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan (this file)
├── research.md          # Phase 0 research & string catalog mapping
├── quickstart.md        # Phase 1 validation guide
└── checklists/
    └── requirements.md  # Specification quality checklist
```

### Source Code (repository root)

```text
app/src/main/
├── res/values/
│   └── strings.xml                                     # [MODIFY] Add centralized string resources
└── java/com/example/
    ├── util/
    │   ├── BluetoothHelper.kt                          # [MODIFY] Replace hardcoded strings
    │   ├── NotificationHelper.kt                       # [MODIFY] Replace hardcoded strings
    │   ├── LocationHelper.kt                           # [MODIFY] Replace hardcoded strings
    │   └── TimeFormatter.kt                            # [MODIFY] Replace hardcoded strings
    ├── ui/
    │   ├── viewmodel/DeviceViewModel.kt                # [MODIFY] Add titleRes to TimeFilter enum
    │   ├── components/
    │   │   ├── DeviceCard.kt                           # [MODIFY] Use stringResource
    │   │   ├── TimelineItem.kt                         # [MODIFY] Use stringResource
    │   │   └── WarningBanner.kt                        # [MODIFY] Use stringResource
    │   └── screens/
    │       ├── DeviceListScreen.kt                     # [MODIFY] Use stringResource
    │       ├── DeviceDetailScreen.kt                   # [MODIFY] Use stringResource
    │       ├── SettingsScreen.kt                       # [MODIFY] Use stringResource
    │       └── PermissionOnboardingScreen.kt           # [MODIFY] Use stringResource
    └── data/repository/DeviceRepository.kt             # [MODIFY] Align fallback device names
```

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
| :--- | :--- | :--- |
| None | N/A | Centralizing strings in `strings.xml` is the standard Android way |
