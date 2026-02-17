# Document Scanner KMP App

## What This App Does

Mobile app that scans insurance documents (car, health, home), automatically extracts expiry dates using cloud OCR (Google Document AI), stores documents locally, and sends notification reminders before expiry. The key differentiator: no manual date entry — OCR handles it automatically.

## Tech Stack

| Component | Library | Version |
|-----------|---------|---------|
| Framework | Kotlin Multiplatform + Compose Multiplatform | Kotlin 2.1.10, Compose 1.7.3 |
| Build | Gradle 8.11.1, AGP 8.9.1 | compileSdk 36 |
| Database | SQLDelight | 2.0.2 |
| HTTP | Ktor | 3.1.1 |
| DI | Koin (BOM) | 4.0.2 |
| Camera | CameraK (`io.github.kashif-mehmood-km:camerak`) | 0.2.0 |
| Navigation | Navigation Compose | 2.8.0-alpha10 |
| Date/Time | kotlinx-datetime | 0.6.2 |
| Serialization | kotlinx-serialization | 1.7.3 |
| Notifications | Native (AlarmManager + BroadcastReceiver) | expect/actual |

**Note:** Alarmee was removed due to requiring Kotlin 2.2.20+. Notifications use native Android AlarmManager via expect/actual.

## Architecture

Clean Architecture with expect/actual for platform-specific code:

```
DocumentScanner/
├── shared/src/
│   ├── commonMain/kotlin/com/docscanner/
│   │   ├── App.kt                        # Root composable
│   │   ├── di/                            # Koin modules (App, Data, Domain, Presentation, Platform expect)
│   │   ├── domain/
│   │   │   ├── model/                     # Document, ScanResult, ReminderInterval
│   │   │   ├── repository/                # DocumentRepository, OcrRepository (interfaces)
│   │   │   └── usecase/                   # Scan, GetDocuments, Save, Update, Delete, ScheduleReminders
│   │   ├── data/
│   │   │   ├── local/                     # DatabaseDriverFactory (expect)
│   │   │   ├── remote/                    # DocumentAiService, DTOs, Mapper, Config
│   │   │   └── repository/               # DocumentRepositoryImpl, OcrRepositoryImpl
│   │   ├── presentation/
│   │   │   ├── main/                      # MainViewModel, MainScreen (document list)
│   │   │   ├── camera/                    # CameraScreen (placeholder — needs CameraK wiring)
│   │   │   ├── preview/                   # ImagePreviewScreen (confirm + trigger OCR)
│   │   │   ├── detail/                    # DetailViewModel, DetailScreen (new/edit dual-mode)
│   │   │   ├── navigation/               # NavGraph, Screen sealed interface
│   │   │   └── components/               # DocumentListItem, LoadingOverlay, ReminderChips
│   │   └── platform/                      # ImageStorage, PlatformContext, NotificationScheduler (expect)
│   ├── commonTest/                        # Unit tests (MapperTest, ReminderIntervalTest)
│   ├── androidMain/kotlin/com/docscanner/  # Android actuals (DB driver, image storage, notifications)
│   └── iosMain/kotlin/com/docscanner/      # iOS stubs (not built until Mac available)
├── androidApp/src/androidMain/             # MainActivity, DocumentScannerApp (Koin init), Manifest
├── shared/src/commonMain/sqldelight/       # Document.sq schema
└── local.properties                        # SDK path + Document AI credentials (not committed)
```

## Database Schema (SQLDelight)

```sql
CREATE TABLE DocumentEntity (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    imagePath TEXT NOT NULL,
    thumbnailPath TEXT NOT NULL,
    expiryDate TEXT,              -- ISO 8601: "2026-03-15"
    rawOcrResponse TEXT,
    confidence REAL,
    reminderDays TEXT NOT NULL DEFAULT '90,30,7,1',
    createdAt TEXT NOT NULL
);
```

## User Flow

1. **Main Screen** — list of scanned documents (thumbnail, name, expiry date), FAB to scan
2. **Camera Screen** — capture document photo (CameraK placeholder)
3. **Image Preview Screen** — review photo, tap "Get Expiry Date" to trigger OCR
4. **Detail Screen** — view/edit name, date, reminder intervals; save or delete

## Google Document AI Integration

- REST API: `POST https://documentai.googleapis.com/v1/projects/{id}/locations/{loc}/processors/{pid}:process`
- Sends base64-encoded image, receives entities with `expiry_date` and `document_name`
- `DocumentAiMapper` date parsing priority: (1) structured `dateValue`, (2) ISO from `normalizedValue.text`, (3) regex on `mentionText` (DD/MM/YYYY, YYYY-MM-DD)
- Credentials stored in `local.properties` and injected via BuildConfig

## Build & Test Commands

```bash
./gradlew :shared:testDebugUnitTest       # Run unit tests
./gradlew :androidApp:assembleDebug       # Build debug APK (23MB)
./gradlew :androidApp:installDebug        # Install on device/emulator
```

## Known Issues & Gotchas

- **Clock conflict**: `kotlinx.datetime.Clock` conflicts with `kotlin.time.Clock` in Kotlin 2.1.x. Use `import kotlinx.datetime.Clock as DateTimeClock`.
- **expect/actual beta**: Requires `-Xexpect-actual-classes` compiler flag to suppress warnings.
- **CameraK requires compileSdk 36**: Transitively pulls `activity-compose:1.11.0` which needs it.
- **iOS targets**: Configured in shared/build.gradle.kts but only compiled on macOS. Skipped on Linux.
- **CameraScreen**: Currently a placeholder — needs actual CameraK composable wiring and device testing.
- **KMP source layout**: Android sources go in `androidMain/` not `main/` (both in shared and androidApp modules).

## What's Not Done Yet

- Wire CameraK camera preview in `CameraScreen.kt`
- Set up Google Document AI in GCP (create custom extractor, label training data, deploy)
- Fill `local.properties` with real Document AI credentials
- Test on real Android device
- Phase 8: iOS implementation (requires Mac hardware)
