# Quickstart Validation Guide: Remove Background Location Permission

**Feature**: Remove Redundant Background Location Permission (`004-remove-background-location-permission`)  
**Status**: Ready for Validation  
**Date**: 2026-09-02  

---

## 1. Static Manifest Audit

Execute the following PowerShell command to verify that `ACCESS_BACKGROUND_LOCATION` does not exist in `AndroidManifest.xml`:

```powershell
Select-String -Path "app/src/main/AndroidManifest.xml" -Pattern "ACCESS_BACKGROUND_LOCATION"
```

**Expected Result**: Zero matching lines found.

---

## 2. Automated Test Execution

Run the complete unit test suite to ensure no regression in location or service behavior:

```powershell
.\gradlew.bat testDebugUnitTest
```

**Expected Result**: All tests pass (`BUILD SUCCESSFUL`).
