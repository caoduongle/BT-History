# Technical Research: Pruning Unused Template Dependencies

**Feature**: Prune Unused Template Dependencies (`005-prune-unused-dependencies`)  
**Status**: Completed  
**Date**: 2026-09-02  

---

## 1. Audit of Codebase References (`app/src`)

A full recursive scan of all Kotlin (`.kt`), XML (`.xml`), and Gradle configuration files was conducted to detect references to external libraries.

### 1.1 Unused Dependencies Identified
| Category | Artifacts | Status in Codebase |
| :--- | :--- | :--- |
| **Firebase Suite** | `firebase-bom`, `firebase-ai`, `firebase-auth`, `firebase-firestore`, `firebase-appcheck-*`, `google-services` plugin | 0 imports; 0 usage |
| **Networking** | `retrofit`, `converter-moshi`, `okhttp`, `logging-interceptor` | 0 imports; 0 usage |
| **JSON / Codegen** | `moshi-kotlin`, `moshi-kotlin-codegen` (KSP processor) | 0 imports; 0 usage; wastes KSP build cycles |
| **CameraX** | `camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view` | 0 imports; 0 usage |
| **Identity & Auth** | `credentials`, `googleid` | 0 imports; 0 usage |
| **Image Loading** | `coil-compose` | 0 imports; 0 usage |
| **Permissions Helper** | `accompanist-permissions` | 0 imports; 0 usage (standard Jetpack Compose `rememberLauncherForActivityResult` is used) |
| **Secrets Plugin** | `secrets-gradle-plugin` | 0 secret keys used (no API keys, only standard `APPLICATION_ID` in `BuildConfig`) |

---

## 2. Active Dependencies Retained

| Module / Layer | Artifacts | Rationale |
| :--- | :--- | :--- |
| **Jetpack Compose UI** | `compose-bom`, `ui`, `ui-graphics`, `ui-tooling`, `ui-tooling-preview`, `material3`, `material-icons-*`, `activity-compose`, `navigation-compose`, `lifecycle-runtime-compose` | Powers the entire modern declarative UI and navigation stack |
| **Persistence (Room)** | `room-runtime`, `room-ktx`, `room-compiler` (KSP) | Manages SQLite database for `devices` and `events` tables |
| **Settings (DataStore)** | `datastore-preferences` | Manages persistent user preferences (service enabled, alert enabled) |
| **Location Services** | `play-services-location` | FusedLocationProviderClient used in `LocationHelper` |
| **Coroutines** | `kotlinx-coroutines-core`, `kotlinx-coroutines-android`, `kotlinx-coroutines-test` | Asynchronous operations and reactive flows |
| **Testing** | `junit`, `androidx-junit`, `espresso-core`, `androidx-core`, `runner`, `robolectric`, `roborazzi` (compose + junit rule) | Unit testing, database tests, service tests, screenshot testing |

---

## 3. Implementation Blueprint

1. **`gradle/libs.versions.toml`**:
   - Clean up `[versions]`, `[libraries]`, and `[plugins]` to strictly include retained artifacts.
   - Consolidate duplicate version entries (e.g. `roomRuntime`, `roomKtx`, `roomCompiler` $\rightarrow$ `room = "2.7.0"`).
2. **`build.gradle.kts`**:
   - Strip `secrets` and `google.services` plugin aliases.
3. **`app/build.gradle.kts`**:
   - Remove unused plugins `secrets` and `google.services`.
   - Remove `import com.google.gms.googleservices...`.
   - Remove `secrets { ... }` and `googleServices { ... }` DSL blocks.
   - Remove dead KSP processor `"ksp"(libs.moshi.kotlin.codegen)`.
   - Remove commented-out dependencies.
4. **Verification**:
   - Run `./gradlew testDebugUnitTest` and/or `./gradlew assembleDebug` to ensure 0 build errors.
