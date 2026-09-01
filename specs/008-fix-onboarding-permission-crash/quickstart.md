# Quickstart Validation Guide: Fix Onboarding Permission Crash

**Feature**: Safe Permission Onboarding & Android 14+ Foreground Service Crash Prevention (`008-fix-onboarding-permission-crash`)  
**Status**: Ready for Validation  
**Date**: 2026-09-02  

---

## 1. Automated Test Execution

Run the complete unit test suite including the new Robolectric Android 14 (API 34) test:

```powershell
.\gradlew.bat testDebugUnitTest
```

**Expected Result**: All tests pass (`BUILD SUCCESSFUL`), verifying that:
1. `OnboardingPermissionSkipTest` verifies clicking Skip does not launch `BluetoothWatcherService`.
2. Invoking `onStartCommand` on API 34 without permissions does not call `ServiceCompat.startForeground()` and throws 0 exceptions.
3. `PreferencesRepository.isServiceEnabledFlow` safely transitions to `false`.

---

## 2. Code Inspection & Verification

Verify the decoupled onboarding signatures and permission guards:

```powershell
# 1. Verify PermissionOnboardingScreen has separate onSkip parameter
Select-String -Path "app/src/main/java/com/example/ui/screens/PermissionOnboardingScreen.kt" -Pattern "onSkip:"

# 2. Verify BluetoothHelper has hasRequiredPermissionsForService
Select-String -Path "app/src/main/java/com/example/util/BluetoothHelper.kt" -Pattern "fun hasRequiredPermissionsForService"

# 3. Verify BluetoothWatcherService checks permissions before startForeground
Select-String -Path "app/src/main/java/com/example/service/BluetoothWatcherService.kt" -Pattern "hasRequiredPermissionsForService"
```

**Expected Result**: Each pattern is found in the corresponding source file.
