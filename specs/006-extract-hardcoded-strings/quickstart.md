# Quickstart Validation Guide: Extract Hardcoded Strings

**Feature**: Extract Hardcoded Strings to Resource Catalog (`006-extract-hardcoded-strings`)  
**Status**: Ready for Validation  
**Date**: 2026-09-02  

---

## 1. Automated Test Execution

Run the complete test suite to confirm zero UI or functional regressions:

```powershell
.\gradlew.bat testDebugUnitTest
```

**Expected Result**: All tests pass (`BUILD SUCCESSFUL`), including screenshot tests (`GreetingScreenshotTest`) and database/receiver unit tests.

---

## 2. Debug Assembly Verification

```powershell
.\gradlew.bat assembleDebug
```

**Expected Result**: `BUILD SUCCESSFUL` with no AAPT resource merging errors or missing string format specifiers.
