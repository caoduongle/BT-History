# Implementation Plan: Remove Redundant Background Location Permission

**Branch**: `004-remove-background-location-permission` | **Date**: 2026-09-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/004-remove-background-location-permission/spec.md`

## Summary

`app/src/main/AndroidManifest.xml` previously declared `<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />`.

Technical analysis confirms:
1. Location is captured exclusively during Bluetooth events while `BluetoothWatcherService` is actively running.
2. `BluetoothWatcherService` is a Foreground Service with an ongoing notification and declared `foregroundServiceType="connectedDevice|location"`.
3. Under Android OS architecture (API 29 to 35+), location queries executed within an active foreground service with type `location` are treated as **Foreground Location Access**, requiring only `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` ("While using the app").
4. Declaring `ACCESS_BACKGROUND_LOCATION` without using background location outside a foreground service unnecessarily subjects the app to Google Play Store's strictest review process (video demo, prominent disclosure, rejection risks).
5. The plan safely removes `ACCESS_BACKGROUND_LOCATION` from the manifest, documents the rationale in `AndroidManifest.xml` and `README.md`, verifies the onboarding screens, and validates the build with regression unit tests.

## Technical Context

**Language/Version**: Kotlin 2.2.10, Java 11 / Android SDK (compileSdk 36, minSdk 24, targetSdk 36)

**Primary Dependencies**: Android Jetpack, Google Play Services Location 21.3.0

**Security & Permissions**: Android Permissions API, Google Play Policy Compliance

**Testing**: JUnit 4 (4.13.2), Robolectric (4.16.1), Gradle Test Runner

**Target Platform**: Android 7.0 (API 24) through Android 15 (API 35/36)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Privacy & Security**: Upheld. Eliminating unnecessary dangerous permissions adheres to the Principle of Least Privilege.
- **YAGNI / Simplicity**: Upheld. Removing redundant permission declarations simplifies Play Store deployment and user onboarding.
- **Verification**: Verified. Automated unit tests ensure no regressions.
- **No Gate Violations**: All criteria satisfied.

## Project Structure

### Documentation (this feature)

```text
specs/004-remove-background-location-permission/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan (this file)
├── research.md          # Phase 0 research & Android/Google Play policy analysis
├── quickstart.md        # Phase 1 validation guide
├── contracts/           # Phase 1 component contracts
│   └── location-permission-contract.md
└── checklists/
    └── requirements.md  # Specification quality checklist
```

### Source Code (repository root)

```text
app/
├── src/main/
│   ├── AndroidManifest.xml                             # [MODIFY] Remove ACCESS_BACKGROUND_LOCATION & add comments
│   └── java/com/example/ui/screens/
│       └── PermissionOnboardingScreen.kt               # [AUDIT] Verify runtime permission request
README.md                                               # [MODIFY] Add permission architecture & Google Play compliance note
```

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
| :--- | :--- | :--- |
| None | N/A | Removing an unnecessary permission is an architecture simplification |
