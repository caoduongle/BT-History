# Implementation Plan: Prune Unused Template Dependencies

**Branch**: `005-prune-unused-dependencies` | **Date**: 2026-09-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/005-prune-unused-dependencies/spec.md`

## Summary

The repository contains numerous residual dependencies and Gradle plugins inherited from an initial multi-purpose template (Firebase, Retrofit, Moshi, OkHttp, CameraX, Credentials, GoogleId, Coil, Secrets plugin). None of these are imported or referenced anywhere in `app/src`.

The implementation plan:
1. Prunes unused dependencies, versions, and plugins from `gradle/libs.versions.toml`.
2. Cleans `build.gradle.kts` (root) by removing unused plugin declarations (`secrets`, `google-services`).
3. Cleans `app/build.gradle.kts` by removing unused plugins, plugin DSL configurations (`secrets`, `googleServices`), commented-out dependency declarations, and dead KSP processors (`moshi.kotlin.codegen`).
4. Verifies `settings.gradle.kts` remains clean and valid.
5. Validates project compilation and test execution via `./gradlew testDebugUnitTest` and `./gradlew assembleDebug`.

## Technical Context

**Language/Version**: Kotlin 2.2.10, Gradle 9.3.1, AGP 9.1.1, KSP 2.3.5

**Build System**: Gradle Version Catalog (`gradle/libs.versions.toml`), Kotlin DSL (`.gradle.kts`)

**Active Dependencies**:
- Jetpack Compose (BOM 2024.09.00)
- Room 2.7.0 (Runtime, KTX, Compiler KSP)
- DataStore Preferences 1.1.7
- Google Play Services Location 21.3.0
- Kotlinx Coroutines 1.10.2
- Testing: JUnit 4, Robolectric 4.16.1, Roborazzi 1.59.0

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Simplicity & YAGNI**: Upheld. Removing dead code and dead dependencies directly honors the YAGNI principle.
- **Fast Build**: Upheld. Eliminating unused KSP annotation processors saves build time.
- **Verification**: Verified. Automated test execution and APK assembly.
- **No Gate Violations**: All criteria satisfied.

## Project Structure

### Documentation (this feature)

```text
specs/005-prune-unused-dependencies/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan (this file)
├── research.md          # Phase 0 research & dependency inventory
├── quickstart.md        # Phase 1 validation guide
└── checklists/
    └── requirements.md  # Specification quality checklist
```

### Source Code (repository root)

```text
gradle/
└── libs.versions.toml   # [MODIFY] Prune unused versions, libraries, and plugins
build.gradle.kts         # [MODIFY] Remove unused root plugin declarations
app/
└── build.gradle.kts     # [MODIFY] Remove unused plugins, configs, KSP, and dependencies
settings.gradle.kts      # [AUDIT] Verify settings repository resolution
```

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
| :--- | :--- | :--- |
| None | N/A | Removing dead dependencies simplifies the project |
