# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This App Does

Mobile app (Terminowo) that scans insurance documents, extracts expiry dates via Google Document AI OCR, stores documents locally with SQLDelight, and sends reminder notifications before expiry. No manual date entry — OCR handles it.

## Build & Test Commands

```bash
./gradlew :shared:testDebugUnitTest                    # Run all unit tests
./gradlew :shared:testDebugUnitTest --tests "*.DocumentAiMapperTest"  # Run single test class
./gradlew :shared:testDebugUnitTest --tests "*.DocumentAiMapperTest.extractDate*"  # Single test method
./gradlew :androidApp:assembleDebug                    # Build debug APK
./gradlew :androidApp:installDebug                     # Install on device/emulator
./run.sh                                               # Build + install + launch (checks for connected device)
```

## Tech Stack

Kotlin Multiplatform (2.1.10) + Compose Multiplatform (1.7.3), Gradle 8.11.1, AGP 8.9.1, compileSdk 36, minSdk 26, JVM target 17. Key libraries: Ktor 3.1.1, SQLDelight 2.0.2, Koin BOM 4.0.2, CameraK 0.2.0, Navigation Compose 2.8.0-alpha10, kotlinx-datetime 0.6.2. Versions managed in `gradle/libs.versions.toml`.

## Architecture

Clean Architecture in two Gradle modules: `:shared` (KMP) and `:androidApp`. Package: `com.stc.terminowo`. Android app package: `com.stc.terminowo.android`.

**Shared module layers** (`shared/src/commonMain/kotlin/com/stc/terminowo/`):
- **domain/** — models (`Document`, `ScanResult`, `ReminderInterval`), repository interfaces, usecases (each single-responsibility: `ScanDocumentUseCase`, `GetDocumentsUseCase`, `SaveDocumentUseCase`, `UpdateDocumentUseCase`, `DeleteDocumentUseCase`, `ScheduleRemindersUseCase`)
- **data/** — repository implementations, SQLDelight `DatabaseDriverFactory` (expect/actual), Ktor-based `DocumentAiService` + DTOs + `DocumentAiMapper` for OCR response parsing
- **presentation/** — Compose screens (main, camera, preview, detail), ViewModels, `NavGraph.kt` with `Screen` sealed interface using `@Serializable` routes, Material3 theme, reusable components
- **platform/** — expect declarations for `ImageStorage`, `NotificationScheduler`, `PlatformContext`, `ImageDecoder`
- **di/** — Koin modules split by layer: `DataModule`, `DomainModule`, `PresentationModule`, `PlatformModule` (expect/actual). All assembled in `AppModule.kt` as `appModules` list.

**Platform actuals** (`shared/src/androidMain/`): SQLite driver, file-based image storage, AlarmManager + BroadcastReceiver notifications.

**Android app module** (`androidApp/src/androidMain/`): `MainActivity`, `TerminowoApp` (Application class, initializes Koin), `ReminderReceiver` (BroadcastReceiver). Document AI credentials injected from `local.properties` via BuildConfig.

**Navigation flow**: Main (document list) → Camera → ImagePreview → Detail (new/edit dual-mode).

## Database

SQLDelight schema at `shared/src/commonMain/sqldelight/com/stc/terminowo/data/local/db/Document.sq`. Generated database class: `DocumentDatabase`. Dates stored as ISO 8601 text. Reminder days stored as CSV string (e.g., `"90,30,7,1"`).

## Document AI OCR

`DocumentAiMapper` date extraction priority: (1) structured `dateValue` fields, (2) ISO parse from `normalizedValue.text`, (3) regex on `mentionText` (DD/MM/YYYY, YYYY-MM-DD). Credentials in `local.properties`, injected via BuildConfig fields: `DOCUMENT_AI_PROJECT_ID`, `DOCUMENT_AI_LOCATION`, `DOCUMENT_AI_PROCESSOR_ID`, `DOCUMENT_AI_API_KEY`.

## Gotchas

- **Clock conflict**: `kotlinx.datetime.Clock` conflicts with `kotlin.time.Clock` in Kotlin 2.1.x. Always use `import kotlinx.datetime.Clock as DateTimeClock`.
- **KMP source layout**: Android sources go in `androidMain/` not `main/` — applies to both `:shared` and `:androidApp` modules.
- **Compiler flags**: `-Xexpect-actual-classes` is set in shared/build.gradle.kts. Opt-ins: `ExperimentalTime`, `ExperimentalUuidApi`.
- **CameraK requires compileSdk 36**: Transitively pulls `activity-compose:1.11.0`.
- **iOS targets**: Configured in shared/build.gradle.kts but only compile on macOS (skipped on Linux).
- **Alarmee removed**: Required Kotlin 2.2.20+. Notifications use native AlarmManager via expect/actual instead.
- **ProGuard**: Release builds have minification enabled. Rules keep Ktor, kotlinx-serialization, and serializer classes (`androidApp/proguard-rules.pro`).

## What's Not Done Yet

- CameraScreen is a placeholder — needs CameraK composable wiring
- Document AI processor not created in GCP yet
- `local.properties` needs real Document AI credentials
- iOS implementation (requires Mac)
