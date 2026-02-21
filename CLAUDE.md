# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This App Does

Mobile app (Terminowo) that scans documents, extracts expiry dates via OCR, stores them locally, and sends reminder notifications. See [ARCHITECTURE.md](ARCHITECTURE.md) for full system design.

## Build & Test Commands

```bash
./gradlew :shared:testDebugUnitTest                    # Run all unit tests
./gradlew :shared:testDebugUnitTest --tests "*.DocumentAiMapperTest"  # Run single test class
./gradlew :shared:testDebugUnitTest --tests "*.DocumentAiMapperTest.extractDate*"  # Single test method
./gradlew :androidApp:assembleDebug                    # Build debug APK
./gradlew :androidApp:installDebug                     # Install on device/emulator
./run.sh                                               # Build + install + launch (checks for connected device)
```

**Manual testing**: Always use `./run.sh` to deploy and test on a connected Android device. It builds, installs, and launches the app in one step.

## Tech Stack

Kotlin Multiplatform (2.1.10) + Compose Multiplatform (1.7.3), Gradle 8.11.1, AGP 8.9.1, compileSdk 36, minSdk 26, JVM target 17. Key libraries: Ktor 3.1.1, SQLDelight 2.0.2, Koin BOM 4.0.2, CameraK 0.2.0, Navigation Compose 2.8.0-alpha10, kotlinx-datetime 0.6.2. Versions managed in `gradle/libs.versions.toml`.

## Architecture

Clean Architecture in `:shared` (KMP) and `:androidApp` modules. Package: `com.stc.terminowo`. Four layers: domain, data, presentation, platform + Koin DI. See [ARCHITECTURE.md](ARCHITECTURE.md) for layer details, data flows, navigation graph, and file tree.

## Database

SQLDelight schema at `shared/src/commonMain/sqldelight/.../Document.sq`. Schema version 3. Dates as ISO 8601, reminders as CSV, category as text key. See [ARCHITECTURE.md](ARCHITECTURE.md#database-schema) for full schema.

## Document AI OCR

Backend proxy (GCP Cloud Function) → `DocumentAiMapper` with 3-strategy date extraction + multilingual category detection. See [ARCHITECTURE.md](ARCHITECTURE.md#ocr-pipeline) for pipeline details.

## Document Categories

`DocumentCategory` enum: `INSURANCE`, `PAYMENT`, `AGREEMENT`, `DRIVER_LICENSE`, `TECHNICAL_INSPECTION`, `OTHER` (default, hidden on list). Auto-detected from OCR text, user-overridable. See [ARCHITECTURE.md](ARCHITECTURE.md#domain-layer).

## Gotchas

- **Clock conflict**: `kotlinx.datetime.Clock` conflicts with `kotlin.time.Clock` in Kotlin 2.1.x. Always use `import kotlinx.datetime.Clock as DateTimeClock`.
- **KMP source layout**: Android sources go in `androidMain/` not `main/` — applies to both `:shared` and `:androidApp` modules.
- **Compiler flags**: `-Xexpect-actual-classes` is set in shared/build.gradle.kts. Opt-ins: `ExperimentalTime`, `ExperimentalUuidApi`.
- **CameraK requires compileSdk 36**: Transitively pulls `activity-compose:1.11.0`.
- **iOS targets**: Configured in shared/build.gradle.kts but only compile on macOS (skipped on Linux).
- **Alarmee removed**: Required Kotlin 2.2.20+. Notifications use native AlarmManager via expect/actual instead.
- **ProGuard**: Release builds have minification enabled. Rules keep Ktor, kotlinx-serialization, and serializer classes (`androidApp/proguard-rules.pro`). **Bug**: ProGuard rules still reference `com.docscanner.**` instead of `com.stc.terminowo.**` — needs fixing before release builds work correctly.

## What's Not Done Yet

- iOS implementation (requires Mac)
