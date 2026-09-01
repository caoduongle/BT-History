# Technical Research: Transactional Device Event Upsert

**Feature**: Transactional Device Event Upsert (`003-transactional-device-upsert`)  
**Status**: Completed  
**Date**: 2026-09-02  

---

## 1. Problem Analysis: Concurrency & Race Conditions

In `DeviceRepository.recordBluetoothEvent()`:
```kotlin
val existingDevice = deviceDao.getDeviceByMac(macAddress)
// ... decide between update and insert ...
if (existingDevice != null) {
    deviceDao.update(updated)
    existingDevice.id
} else {
    deviceDao.insertOrUpdate(newDevice)
}
// ... insert event ...
eventDao.insert(event)
```

### The Flaw
1. **No Transaction Boundary**: The query `getDeviceByMac(macAddress)`, followed by the conditional insert/update, followed by `eventDao.insert(event)` were executed as standalone, un-transactional SQL statements.
2. **Race Condition under Concurrency**: When two or more background coroutines receive connection events for a newly paired or connecting device at the same time:
   - Coroutine A checks `getDeviceByMac("AA:BB:CC...")` $\rightarrow$ returns `null`.
   - Coroutine B checks `getDeviceByMac("AA:BB:CC...")` $\rightarrow$ returns `null`.
   - Both coroutines proceed to the `else` branch and attempt to insert a new `DeviceEntity`.
3. **Foreign Key Cascade Hazard of `OnConflictStrategy.REPLACE`**:
   - `DeviceDao.insertOrUpdate` used `@Insert(onConflict = OnConflictStrategy.REPLACE)`.
   - In SQLite, `REPLACE` on a unique column (`mac_address`) deletes the conflicting existing row and inserts a new row with a new auto-generated `id`.
   - Because `EventEntity` has `ForeignKey(..., onDelete = CASCADE)`, deleting the existing row deletes all past events associated with that device, or causes `SQLiteConstraintException: FOREIGN KEY constraint failed`.

---

## 2. Research Decisions

### Decision 1: Use `RoomDatabase.withTransaction` for Atomic Orchestration
- **Decision**: Wrap the entire read-then-write logic of `recordBluetoothEvent()` inside `database.withTransaction { ... }`.
- **Rationale**:
  - `RoomDatabase.withTransaction` is the official, idiomatic Android Jetpack mechanism for coordinating multiple DAOs (`DeviceDao` and `EventDao`) within a single ACID transaction.
  - In SQLite (WAL mode), write transactions are serialized. When Coroutine A enters `withTransaction`, Coroutine B will wait. Coroutine A inserts the device and commits. When Coroutine B enters the transaction, `getDeviceByMac` immediately finds the newly committed device and takes the update path.
- **Alternatives Considered**:
  - *Moving all logic into a single `@Transaction` method in `DeviceDao`*: Would require `DeviceDao` to manage `EventEntity` insertion, violating separation of concerns between `DeviceDao` and `EventDao`.
  - *Mutex in memory*: Only protects the local JVM process and does not leverage database ACID guarantees.

### Decision 2: Safe Upsert Pattern with `OnConflictStrategy.IGNORE`
- **Decision**: Replace `OnConflictStrategy.REPLACE` in `DeviceDao` with `OnConflictStrategy.IGNORE` on insert:
  ```kotlin
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insert(device: DeviceEntity): Long
  ```
  If `insert` returns `-1L` (indicating an ignored constraint conflict), the code safely falls back to re-querying `getDeviceByMac(macAddress)` and updating the existing record.
- **Rationale**:
  - Eliminates the silent deletion behavior of `REPLACE`.
  - Prevents `SQLiteConstraintException` from bubbling up if an unexpected collision occurs.
  - Guarantees that the device's auto-generated primary key `id` remains stable, preserving all foreign key relationships with `EventEntity`.
- **Alternatives Considered**:
  - *`OnConflictStrategy.ABORT` (default)*: Throws `SQLiteConstraintException` on race condition, crashing the caller.
  - *Room `@Upsert`*: Room's `@Upsert` targets primary keys; `DeviceEntity` has an auto-generated primary key `id` where the uniqueness constraint is on `mac_address`.

### Decision 3: Repository Dependency Injection / Constructor Flexibility
- **Decision**: Update `DeviceRepository` to accept `database: AppDatabase` while providing an optional/overloaded secondary constructor for mock testing without database instances:
  ```kotlin
  class DeviceRepository(
      private val database: AppDatabase?,
      private val deviceDao: DeviceDao,
      private val eventDao: EventDao
  ) {
      constructor(deviceDao: DeviceDao, eventDao: EventDao) : this(null, deviceDao, eventDao)
  ```
- **Rationale**:
  - `BtWatcherApplication` injects `database, database.deviceDao(), database.eventDao()`.
  - Existing or future unit tests that only mock DAOs continue to function seamlessly without requiring a full SQLite mock.

---

## 3. Concurrency Testing Strategy

- Implement `ConcurrentEventRecordingTest` in `app/src/test/java/com/example/ConcurrentEventRecordingTest.kt`.
- Use `kotlinx.coroutines.async` with `Dispatchers.IO` to launch 10 simultaneous coroutines calling `repository.recordBluetoothEvent` with identical MAC addresses.
- Use `awaitAll()` to wait for all coroutines to complete.
- Assert:
  1. No exceptions thrown.
  2. `deviceDao.getAllDevicesFlow().first().size == 1` (exactly 1 device created).
  3. All recorded events share the identical `deviceId`.
