# Quickstart & Verification Guide: Simulation Gating & Developer Mode

**Feature**: `009-gate-simulate-feature`  
**Date**: 2026-09-02  

---

## 1. Automated Test Execution

Run the complete Robolectric and unit test suite:

```bash
./gradlew.bat testDebugUnitTest
```

Targeted test execution for simulation gating:

```bash
./gradlew.bat testDebugUnitTest --tests "com.example.SimulationGatingTest"
```

Expected output:
```text
BUILD SUCCESSFUL in ...
```

---

## 2. Manual Emulator Verification Procedure

### Test 1: Production Release Behavior (Default)
1. Launch the application with a fresh install (or clear app storage).
2. Observe `DeviceListScreen`:
   - Verify there is **NO** FloatingActionButton with speed/simulate icon ("Mô phỏng").
   - When the device list is empty, verify the "Thêm dữ liệu mẫu để thử nghiệm" button is **NOT** present.
3. Open a device's detail screen (`DeviceDetailScreen`):
   - Tap the overflow menu (three vertical dots).
   - Verify the menu only contains "Xóa thiết bị này", and **NO** simulation options ("Mô phỏng Kết nối" / "Mô phỏng Ngắt kết nối").

### Test 2: Unlocking Developer Mode (7 Taps)
1. Open the drawer or top bar navigation and go to `SettingsScreen`.
2. Scroll to the very bottom to find the App Version row ("Phiên bản 1.0.0").
3. Tap the version row rapidly:
   - On tap 4: Toast says "Bạn còn 3 lần nhấn nữa để bật chế độ nhà phát triển."
   - On tap 5: Toast says "Bạn còn 2 lần nhấn nữa để bật chế độ nhà phát triển."
   - On tap 6: Toast says "Bạn còn 1 lần nhấn nữa để bật chế độ nhà phát triển."
   - On tap 7: Toast says "Đã bật chế độ nhà phát triển! Các công cụ mô phỏng đã sẵn sàng."
4. Tap again: Toast says "Bạn đã là nhà phát triển rồi!"

### Test 3: Verifying Simulation Controls After Unlock
1. Return to `DeviceListScreen`:
   - The FloatingActionButton "Mô phỏng" is now **VISIBLE**.
   - Tapping it opens the `SimulateEventDialog` normally.
2. In empty state:
   - The button "Thêm dữ liệu mẫu để thử nghiệm" is now **VISIBLE**.
3. Open `DeviceDetailScreen`:
   - Open overflow menu: "Mô phỏng Kết nối" and "Mô phỏng Ngắt kết nối" are now **VISIBLE** and functional.
