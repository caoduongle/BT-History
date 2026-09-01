# Quickstart Validation Guide: Transactional Device Event Upsert

**Feature**: Transactional Device Event Upsert (`003-transactional-device-upsert`)  
**Status**: Ready for Validation  
**Date**: 2026-09-02  

---

## 1. Automated Concurrency Test

Execute the concurrency test suite via Gradle:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.ConcurrentEventRecordingTest"
```

### Expected Test Results

1. `testConcurrentRecordBluetoothEvent_createsExactlyOneDevice`:
   - Launches 10 parallel coroutines on `Dispatchers.IO` targeting the same MAC address (`"AA:BB:CC:DD:EE:FF"`).
   - Awaits completion of all 10 coroutines simultaneously.
   - Asserts:
     - Exactly 1 `DeviceEntity` exists in the database (`deviceDao.getAllDevicesFlow().first().size == 1`).
     - Exactly 10 `EventEntity` records exist in the database (`eventDao.getAllEventsFlow().first().size == 10`).
     - All 10 `EventEntity` records possess `deviceId` equal to the single created device's ID.
     - 0 uncaught exceptions.

2. `testConcurrentRecordBluetoothEvent_withExistingDevice_updatesCleanly`:
   - Pre-seeds 1 device into the database.
   - Launches 10 concurrent coroutines updating that device with different timestamps and connection events.
   - Asserts:
     - Still exactly 1 `DeviceEntity` in the database.
     - Device entity has the latest state reflected.
     - All 10 events recorded without deadlock or conflict.

---

## 2. Full Regression Suite

```powershell
.\gradlew.bat testDebugUnitTest
```
All existing tests (`BluetoothDeduplicationTest`, `DisconnectClassificationTest`, etc.) must continue to pass 100%.
