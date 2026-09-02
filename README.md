# BT-History (Bluetooth Watcher)

Ứng dụng Android theo dõi và lưu trữ lịch sử kết nối Bluetooth, tự động ghi nhận vị trí GPS tại thời điểm kết nối/ngắt kết nối và gửi cảnh báo khi thiết bị bị rớt kết nối đột ngột.

---

## 📌 Tính năng chính

- 🔄 **Giám sát kết nối Bluetooth thời gian thực**: Theo dõi trạng thái kết nối/ngắt kết nối của tai nghe, loa, đồng hồ thông minh đã ghép đôi.
- 📍 **Ghi nhận vị trí GPS**: Tự động lưu tọa độ GPS và địa chỉ tại thời điểm thiết bị kết nối hoặc ngắt kết nối.
- ⚠️ **Phân loại ngắt kết nối thông minh (Smart Disconnect Heuristic)**:
  - Phân biệt giữa hành vi **người dùng chủ động tắt Bluetooth** (không gửi cảnh báo làm phiền).
  - Và hành vi **thiết bị rớt kết nối đột ngột** (phát cảnh báo khẩn cấp kèm vị trí cuối cùng).
- 🛡️ **Xử lý sự kiện nguyên tử (Thread-safe & Concurrency-safe)**: Ghi nhận sự kiện qua Room Transaction (`withTransaction`), chống trùng lặp thiết bị và bảo toàn toàn vẹn khóa ngoại.

---

## 🔒 Kiến trúc Quyền Vị trí & Tuân thủ Google Play Policy

### Ứng dụng có cần quyền `ACCESS_BACKGROUND_LOCATION` không?

**Câu trả lời: HOÀN TOÀN KHÔNG.**

Ứng dụng **không khai báo và không yêu cầu** quyền `ACCESS_BACKGROUND_LOCATION` ("Cho phép mọi lúc / Allow all the time") vì các lý do kỹ thuật và chính sách sau:

### 1. Ngữ cảnh Foreground Service theo chuẩn Android (API 29+)
- Việc lấy vị trí GPS chỉ diễn ra duy nhất tại thời điểm phát sinh sự kiện Bluetooth thông qua `BluetoothWatcherService`.
- `BluetoothWatcherService` chạy dưới dạng một **Foreground Service** tích cực, duy trì thông báo thường trực (ongoing notification) và được khai báo rõ ràng trong `AndroidManifest.xml`:
  ```xml
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
  ...
  <service
      android:name=".service.BluetoothWatcherService"
      android:foregroundServiceType="connectedDevice|location" />
  ```
- Theo tài liệu chính thức của Google Android: Khi một ứng dụng thực hiện lấy vị trí trong lúc có một Foreground Service mang type `location` đang hoạt động, hệ điều hành Android định danh đây là **Foreground Location Access** (truy cập vị trí tiền đề).
- Do đó, ứng dụng chỉ cần quyền thông thường `ACCESS_FINE_LOCATION` hoặc `ACCESS_COARSE_LOCATION` (tương ứng với tùy chọn *"Trong khi dùng ứng dụng / While using the app"*) là có thể truy xuất tọa độ GPS chính xác và ổn định.

### 2. Tuân thủ Chính sách Google Play Store (Google Play Policy Compliance)
- **Loại bỏ rủi ro bị từ chối phát hành**: Google Play áp dụng quy trình xét duyệt đặc biệt nghiêm ngặt đối với quyền `ACCESS_BACKGROUND_LOCATION` (yêu cầu điền form giải trình chi tiết, quay video minh họa lý do không thể thay thế bằng Foreground Service, và bắt buộc tạo giao diện prominent disclosure riêng biệt).
- Theo nguyên tắc của Google Play: Nếu nhu cầu vị trí có thể đáp ứng được bằng Foreground Service hiển thị thông báo thì **không được phép** xin quyền Background Location.
- Việc loại bỏ hoàn toàn `ACCESS_BACKGROUND_LOCATION` giúp ứng dụng:
  - Đảm bảo 100% tuân thủ Google Play Location Policy.
  - Tối ưu hóa trải nghiệm người dùng (UX): Người dùng chỉ cần cấp quyền 1 chạm trong onboarding thay vì phải bị điều hướng vào sâu trong Cài đặt hệ thống của Android.
  - Tăng độ tin cậy và bảo vệ quyền riêng tư người dùng (Principle of Least Privilege).

---

## 🧪 Kiểm thử trên Android Emulator & Chế độ Nhà phát triển (Developer Mode)

### Môi trường máy ảo Emulator không có phần cứng Bluetooth
- Khi thử nghiệm ứng dụng trên máy ảo Android Emulator hoặc thiết bị không có tai nghe/loa Bluetooth thật, ứng dụng cung cấp bộ công cụ **Mô phỏng sự kiện Bluetooth (Simulation Tools)** để tiêm thử các sự kiện Kết nối / Ngắt kết nối và tọa độ GPS mẫu.
- Để đảm bảo tính toàn vẹn dữ liệu cho người dùng cuối trên các bản phát hành (Production Release), toàn bộ các nút bấm và menu mô phỏng này **bị ẩn hoàn toàn theo mặc định**.

### Cách mở khoá Chế độ Nhà phát triển (Developer Options Easter Egg)
1. Mở màn hình **Cài đặt (SettingsScreen)** trong ứng dụng.
2. Cuộn xuống dưới cùng đến dòng hiển thị **Phiên bản ứng dụng** (`Phiên bản x.x.x`).
3. **Chạm liên tục 7 lần** vào dòng phiên bản:
   - Từ lần chạm thứ 4 đến thứ 6: Hệ thống hiển thị thông báo đếm ngược (*"Bạn còn X lần nhấn nữa để bật chế độ nhà phát triển"*).
   - Lần chạm thứ 7: Thông báo kích hoạt thành công xuất hiện (*"Đã bật chế độ nhà phát triển! Các công cụ mô phỏng đã sẵn sàng"*).
4. Sau khi mở khoá, nút **"Mô phỏng" (FAB)**, nút **"Thêm dữ liệu mẫu"** và các menu item mô phỏng trong chi tiết thiết bị sẽ tự động xuất hiện.
- *Lưu ý: Đối với bản build Debug (`BuildConfig.DEBUG`), các công cụ mô phỏng luôn luôn hiển thị mặc định mà không cần thao tác mở khoá.*

---

## 🛠️ Yêu cầu kỹ thuật & Công nghệ sử dụng

- **Ngôn ngữ**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose, Material 3
- **Kiến trúc**: MVVM + Clean Architecture / Unidirectional Data Flow
- **Cơ sở dữ liệu**: Room 2.7.0 với SQLite Coroutines Transactions
- **Định vị**: Google Play Services FusedLocationProviderClient
- **Kiểm thử**: Robolectric 4.16.1, JUnit 4, AndroidX Test Core, Roborazzi
