# Feature Specification: Prune Unused Template Dependencies

**Feature Branch**: `005-prune-unused-dependencies`

**Created**: 2026-09-02

**Status**: Draft

**Input**: User description: "File gradle/libs.versions.toml và app/build.gradle.kts của repo BT-History còn nhiều dependency/plugin từ template gốc (Firebase, Retrofit, Moshi, OkHttp, CameraX, Credentials/GoogleId, Coil) mà phần lớn đang bị comment hoặc không được import ở bất kỳ file .kt nào trong app/src. Hãy: 1. Quét toàn bộ app/src để xác nhận dependency nào thực sự được dùng. 2. Xoá khỏi libs.versions.toml, build.gradle.kts (project và app) và settings.gradle.kts những dependency/plugin không dùng (Firebase, Retrofit, Moshi, OkHttp, CameraX, Credentials, GoogleId, secrets-gradle-plugin nếu không cần .env, v.v.), giữ lại đúng những gì app thật sự cần (Room, DataStore, Compose, Navigation, Play Services Location, Coroutines). 3. Đảm bảo project build thành công sau khi dọn (chạy ./gradlew assembleDebug hoặc tương đương) và không có warning về plugin thiếu."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Lean & Fast Build Configuration (Priority: P1)

As an Android developer working on the BT-History project, I want the Gradle build scripts and version catalog (`libs.versions.toml`) to only declare dependencies and plugins that are genuinely required and imported by the codebase, so that build overhead is reduced, configuration cache resolution is faster, and the repository is free of confusing template boilerplate.

**Why this priority**: Leftover template dependencies (Firebase, CameraX, Retrofit, Moshi, OkHttp, Coil, Credentials, Secrets plugin) clutter the project, slow down dependency resolution, pull in unnecessary transitive dependencies, and run dead KSP annotation processors during compilation.

**Independent Test**: Inspect `libs.versions.toml`, `build.gradle.kts`, and `app/build.gradle.kts` to verify no unused libraries or plugins remain; then execute `./gradlew testDebugUnitTest` and confirm clean, successful compilation.

**Acceptance Scenarios**:

1. **Given** `gradle/libs.versions.toml`, **When** inspecting the version catalog, **Then** all unused libraries (Firebase, Retrofit, Moshi, OkHttp, CameraX, Coil, Credentials, GoogleId, Accompanist) and unused plugins (`google-services`, `secrets`) are completely removed.
2. **Given** `build.gradle.kts` and `app/build.gradle.kts`, **When** reviewing plugin and dependency blocks, **Then** the unused plugins (`secrets`, `google-services`), configuration blocks (`secrets { ... }`, `googleServices { ... }`), and commented-out dependency lines are eliminated.
3. **Given** the pruned Gradle setup, **When** executing a full build (`./gradlew testDebugUnitTest` or `./gradlew assembleDebug`), **Then** Gradle builds successfully with code 0 and no missing plugin or unresolved symbol warnings.

---

### User Story 2 - Minimal Annotation Processing & Clean Dependency Graph (Priority: P2)

As a developer maintaining the codebase, I want KSP and build configuration to only execute processors needed for the app's active modules (Room compiler), so that build time is not wasted generating code for unused libraries.

**Why this priority**: Running `"ksp"(libs.moshi.kotlin.codegen)` when no Moshi models exist incurs unnecessary KSP task execution overhead on every compile.

**Independent Test**: Verify `app/build.gradle.kts` only includes KSP for Room compiler, and confirm all Room DAOs compile properly.

**Acceptance Scenarios**:

1. **Given** `app/build.gradle.kts`, **When** checking the KSP configurations, **Then** `"ksp"(libs.moshi.kotlin.codegen)` is removed and only `"ksp"(libs.androidx.room.compiler)` is retained.

---

### Edge Cases

- **BuildConfig & Environment Secrets**: Ensure removing `secrets-gradle-plugin` does not break `BuildConfig` references; `app/src` only references standard Android `BuildConfig.APPLICATION_ID`.
- **Roborazzi Screenshot Plugin**: `roborazzi` is actively used by `GreetingScreenshotTest.kt` and must be retained.
- **Compose Tooling Preview & Extended Icons**: Actively used in UI screens and must be retained.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST verify that the following core libraries are preserved:
  - Jetpack Compose (BOM, UI, Material 3, Icons, Activity Compose, Navigation Compose, Lifecycle Compose)
  - Room (Runtime, KTX, Compiler KSP)
  - DataStore (Preferences)
  - Google Play Services Location
  - Kotlinx Coroutines (Core, Android, Test)
  - AndroidX Core-KTX, Lifecycle-KTX
  - Testing (JUnit 4, AndroidX Test, Robolectric, Roborazzi)
- **FR-002**: The system MUST remove the following unused libraries and their version entries from `gradle/libs.versions.toml`:
  - `firebase-bom`, `firebase-ai`, `firebase-appcheck-recaptcha`, `firebase-appcheck-debug`, `firebase-firestore`, `firebase-auth`
  - `coil-compose`
  - `retrofit`, `converter-moshi`
  - `okhttp`, `logging-interceptor`
  - `moshi-kotlin`, `moshi-kotlin-codegen`
  - `androidx-camera-camera2`, `androidx-camera-lifecycle`, `androidx-camera-view`, `androidx-camera-core`
  - `androidx-credentials`, `androidx-credentials-play-services`, `googleid`
  - `accompanist-permissions`
- **FR-003**: The system MUST remove the following unused plugins from `gradle/libs.versions.toml`:
  - `secrets` (`com.google.android.libraries.mapsplatform.secrets-gradle-plugin`)
  - `google-services` (`com.google.gms.google-services`)
- **FR-004**: The system MUST remove the unused plugin aliases from `build.gradle.kts` and `app/build.gradle.kts`.
- **FR-005**: The system MUST remove `secrets { ... }` and `googleServices { ... }` configuration blocks and unused commented dependencies from `app/build.gradle.kts`.
- **FR-006**: The system MUST remove `"ksp"(libs.moshi.kotlin.codegen)` from `app/build.gradle.kts`.
- **FR-007**: The system MUST verify that the project builds cleanly without errors (`./gradlew testDebugUnitTest`).

### Key Entities

- **gradle/libs.versions.toml**: The centralized Gradle version catalog defining dependencies, versions, and plugins.
- **build.gradle.kts**: Project root build script defining top-level plugins.
- **app/build.gradle.kts**: App module build script defining compile options, active plugins, and dependency implementations.
- **settings.gradle.kts**: Repository settings and dependency resolution management.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% removal of all 20+ dead library artifacts and 2 unused Gradle plugins from `libs.versions.toml` and build scripts.
- **SC-002**: 0 build warnings or configuration cache errors regarding missing plugins or unused dependencies.
- **SC-003**: Reduction in lines of code in `gradle/libs.versions.toml` by at least 40%.
- **SC-004**: 100% pass rate on all automated unit tests and successful build compilation.

## Assumptions

- None of the removed dependencies (Firebase, CameraX, Retrofit, Moshi, OkHttp, Coil, Credentials) are imported or referenced in any `.kt`, `.xml`, or test file in the repository.
- Google Play Services Location and Android Jetpack Room/Compose are sufficient for all current and planned app functionality.
