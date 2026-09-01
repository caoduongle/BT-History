# Quickstart Validation Guide: Prune Unused Template Dependencies

**Feature**: Prune Unused Template Dependencies (`005-prune-unused-dependencies`)  
**Status**: Ready for Validation  
**Date**: 2026-09-02  

---

## 1. Automated Build & Test Execution

Run the complete test suite to confirm dependency graph integrity and clean compilation:

```powershell
.\gradlew.bat testDebugUnitTest
```

**Expected Result**: All tests pass (`BUILD SUCCESSFUL`), configuration cache is populated/reused, and zero missing dependency or plugin warnings appear.

---

## 2. Debug Assembly Verification

Compile the debug APK to ensure all build features, KSP annotation processors, and packaging configurations function properly:

```powershell
.\gradlew.bat assembleDebug
```

**Expected Result**: Debug APK built successfully (`BUILD SUCCESSFUL`).
