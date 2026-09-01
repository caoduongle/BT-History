# Quickstart & Verification Guide: Room Schema Export & Migration Framework

**Feature**: `007-room-schema-migrations`  
**Purpose**: Step-by-step instructions to verify schema export, migration framework readiness, and regression-free operation.

---

## Prerequisites

- Java 17 / Eclipse Adoptium JDK
- Android SDK installed (Android 14/15/16 preview API 34-36)
- Git repository `BT-History`

---

## Verification Steps

### Step 1: Verify Schema Export Generation
Run the unit test build to invoke KSP and generate the Room schema JSON:
```powershell
.\gradlew.bat testDebugUnitTest
```
**Expected Outcome**:
- `app/schemas/com.example.data.AppDatabase/1.json` is generated.
- Inspect `1.json`: Verify that it contains valid JSON with `"version": 1` and declarations for both `devices` and `events` tables.

### Step 2: Verify Removal of `fallbackToDestructiveMigration`
Inspect `app/src/main/java/com/example/data/AppDatabase.kt`:
```powershell
Select-String -Path app/src/main/java/com/example/data/AppDatabase.kt -Pattern "fallbackToDestructiveMigration"
```
**Expected Outcome**:
- Zero matches found (the method call has been removed from `AppDatabase.kt`).

### Step 3: Verify Unit & Integration Tests
Run all unit tests to confirm that DAOs, repositories, and receivers work properly with the updated database builder:
```powershell
.\gradlew.bat testDebugUnitTest
```
**Expected Outcome**:
- All test suites pass with `BUILD SUCCESSFUL`.

### Step 4: Verify APK Packaging
Assemble the debug APK:
```powershell
.\gradlew.bat assembleDebug
```
**Expected Outcome**:
- `BUILD SUCCESSFUL` with clean APK output.
