# Architecture

## Overview

Terminowo is a mobile app that scans insurance and other documents, extracts expiry dates via Google Document AI OCR, stores documents locally with SQLDelight, and sends reminder notifications before expiry. No manual date entry — OCR handles it.

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Kotlin Multiplatform | 2.1.10 | Shared business logic + UI |
| Compose Multiplatform | 1.7.3 | Declarative UI (Android + iOS) |
| Gradle | 8.11.1 | Build system |
| AGP | 8.9.1 | Android Gradle Plugin |
| Ktor | 3.1.1 | HTTP client (Document AI API) |
| SQLDelight | 2.0.2 | Local database (type-safe SQL) |
| Koin | 4.0.2 (BOM) | Dependency injection |
| CameraK | 0.2.0 | Camera capture (KMP) |
| Navigation Compose | 2.8.0-alpha10 | Screen navigation |
| kotlinx-datetime | 0.6.2 | Date/time handling |

Android targets: compileSdk 36, minSdk 26, JVM 17. Versions managed in `gradle/libs.versions.toml`.

## Module Structure

```
TerminowoApp/
├── :shared               KMP module — all business logic + UI
│   ├── commonMain/       Shared Kotlin code (domain, data, presentation, DI)
│   ├── androidMain/      Android platform actuals
│   └── iosMain/          iOS platform actuals (compiles on macOS only)
├── :androidApp           Android entry point (Application, Activity)
└── iosApp/               Xcode project shell (not yet wired)
```

Package: `com.stc.terminowo` (shared and androidApp).

## Architecture Layers

The `:shared` module follows Clean Architecture with four layers:

```
┌─────────────────────────────────────────────────────┐
│  presentation/          UI + ViewModels             │
│    screens, components, navigation, theme           │
├─────────────────────────────────────────────────────┤
│  domain/                Pure business logic          │
│    models, repository interfaces, use cases         │
├─────────────────────────────────────────────────────┤
│  data/                  Implementations              │
│    repository impls, SQLDelight, Ktor services, DTOs│
├─────────────────────────────────────────────────────┤
│  platform/              Expect/actual abstractions   │
│    image I/O, notifications, auth, decoding         │
├─────────────────────────────────────────────────────┤
│  di/                    Koin module wiring            │
│    DataModule, DomainModule, PresentationModule,    │
│    PlatformModule (expect/actual)                   │
└─────────────────────────────────────────────────────┘
```

### Domain Layer

Models:

| Class | Key Fields |
|---|---|
| `Document` | `id`, `name`, `imagePath`, `thumbnailPath`, `expiryDate: LocalDate?`, `confidence: Float?`, `reminderDays: List<Int>`, `category: DocumentCategory`, `reminderTime: LocalTime`, `createdAt` |
| `ScanResult` | `extractedName`, `expiryDate`, `confidence`, `fullText`, `rawResponse`, `detectedCategory` |
| `DocumentCategory` | Enum: `INSURANCE`, `PAYMENT`, `AGREEMENT`, `DRIVER_LICENSE`, `TECHNICAL_INSPECTION`, `OTHER` (default). Stored by `key` field (e.g. `"insurance"`). |
| `ReminderInterval` | Enum: `FOURTEEN_DAYS(14)`, `SEVEN_DAYS(7)`, `ONE_DAY(1)`, `DAY_OF(0)` |

Repository interfaces:
- `DocumentRepository` — CRUD + `getAllDocuments(): Flow<List<Document>>`
- `OcrRepository` — `processDocument(imageBytes, mimeType): ScanResult`

Use cases (each single-responsibility, invoked via `operator fun invoke`):
- `GetDocumentsUseCase`, `SaveDocumentUseCase`, `UpdateDocumentUseCase`, `DeleteDocumentUseCase`
- `ScanDocumentUseCase` — calls OCR, returns `Result<ScanResult>`
- `ScheduleRemindersUseCase` — cancels existing alarms, schedules per `reminderDays`

### Data Layer

- `DocumentRepositoryImpl` — SQLDelight queries, maps `DocumentEntity` → `Document`. Dates as ISO 8601 text, reminders as CSV, category as text key.
- `OcrRepositoryImpl` — calls `DocumentAiService`, passes response to `DocumentAiMapper`
- `DocumentAiService` — Ktor HTTP client, Base64-encodes image, POSTs to backend proxy
- `DocumentAiMapper` — extracts date, name, and category from OCR response
- `DatabaseDriverFactory` — expect/actual for SQLite driver creation
- DTOs: `ProcessRequest`, `ProcessResponse`, `DocumentAiDocument`, `DocumentAiEntity`, `NormalizedValue`, `DateValue`

### Presentation Layer

ViewModels:
- `CategoryListViewModel` — groups documents by category, exposes `CategoryListUiState`
- `DocumentListViewModel` — filters documents by category, handles delete confirmation
- `DetailViewModel` — new/edit dual-mode, saves/updates/deletes documents, schedules reminders
- `AuthViewModel` — Google OAuth login/logout (dormant, behind `FeatureFlags.GOOGLE_SIGN_IN_ENABLED`)

Screens: `CategoryListScreen`, `DocumentListScreen`, `CameraScreen` (placeholder), `ImagePreviewScreen`, `DetailScreen`

Components: `DocumentListItem`, `ReminderChips`, `LoadingOverlay`, `AccountIconButton`

Theme: `TerminowoTheme` (Material3)

## Data Flow

### Document Scan Flow

```
Camera ──capture──► ImagePreview ──"Analyze"──► DocumentAiService
                         │                          │
                    save image                 POST to proxy
                    + thumbnail                (Base64 + API key)
                         │                          │
                         │                          ▼
                         │                    DocumentAiMapper
                         │                    ├─ extractDate()
                         │                    ├─ extractName()
                         │                    └─ extractCategory()
                         │                          │
                         │                      ScanResult
                         │                          │
                         ▼                          ▼
                    DetailNew(imagePath, ────── pre-filled fields)
                         │
                    user edits ──► Save
                         │
                    ┌────┴────┐
                    │         │
              DocumentRepo  ScheduleReminders
              (SQLDelight)  (AlarmManager)
                    │
                    ▼
               Categories
```

### Document View Flow

```
Categories ──tap category──► DocumentList(categoryKey)
                                    │
                              tap document
                                    │
                                    ▼
                            DetailEdit(documentId)
                                    │
                              load from DB
                              edit fields
                                    │
                              Save / Delete
                                    │
                                    ▼
                                  back
```

## Navigation Graph

Start destination: `Screen.Categories`

```
┌────────────┐    ┌──────────────────┐    ┌──────────────┐
│ Categories │───►│ DocumentList     │───►│ DetailEdit   │
│            │    │ (categoryKey?)   │    │ (documentId) │
└─────┬──────┘    └──────────────────┘    └──────────────┘
      │
      │ FAB
      ▼
┌────────────┐    ┌──────────────────┐    ┌──────────────┐
│   Camera   │───►│ ImagePreview     │───►│ DetailNew    │──► Categories
│            │    │ (imagePath)      │    │ (scan data)  │    (popUpTo)
└────────────┘    └──────────────────┘    └──────────────┘
```

Routes (all `@Serializable`):

| Route | Type | Parameters |
|---|---|---|
| `Screen.Categories` | `data object` | — |
| `Screen.DocumentList` | `data class` | `categoryKey: String?` |
| `Screen.Camera` | `data object` | — |
| `Screen.ImagePreview` | `data class` | `imagePath: String` |
| `Screen.DetailNew` | `data class` | `name`, `expiryDate`, `confidence`, `imagePath`, `thumbnailPath`, `rawOcrResponse`, `documentId`, `category` |
| `Screen.DetailEdit` | `data class` | `documentId: String` |

## Database Schema

SQLDelight schema at `shared/src/commonMain/sqldelight/com/stc/terminowo/data/local/db/Document.sq`.

```sql
CREATE TABLE DocumentEntity (
    id            TEXT NOT NULL PRIMARY KEY,
    name          TEXT NOT NULL,
    imagePath     TEXT NOT NULL,
    thumbnailPath TEXT NOT NULL,
    expiryDate    TEXT,                          -- ISO 8601 (nullable)
    rawOcrResponse TEXT,
    confidence    REAL,
    reminderDays  TEXT NOT NULL DEFAULT '14,7,1,0',  -- CSV of ints
    category      TEXT NOT NULL DEFAULT 'other',     -- DocumentCategory.key
    createdAt     TEXT NOT NULL,
    reminderTime  TEXT NOT NULL DEFAULT '09:00'      -- HH:MM format
);
```

Generated class: `DocumentDatabase`. Current schema version: 3 (initial + 2 migrations).

Migrations:
- `1.sqm`: `ALTER TABLE DocumentEntity ADD COLUMN category TEXT NOT NULL DEFAULT 'other'`
- `2.sqm`: `ALTER TABLE DocumentEntity ADD COLUMN reminderTime TEXT NOT NULL DEFAULT '09:00'`

## OCR Pipeline

```
imageBytes ──► DocumentAiService.processDocument()
                  │
                  ├─ Base64-encode image
                  ├─ POST to backend proxy (X-API-Key header)
                  └─ Parse ProcessResponse JSON
                        │
                        ▼
               DocumentAiMapper.mapToScanResult()
                  │
                  ├─ extractDate(entity)     ◄── 3-strategy cascade:
                  │     1. dateValue { year, month, day }
                  │     2. ISO parse from normalizedValue.text
                  │     3. Regex on mentionText (DD/MM/YYYY, YYYY-MM-DD)
                  │
                  ├─ extractName(entity)     ◄── from entity type "document_name"
                  │
                  └─ extractCategory(fullText) ◄── keyword matching
                        Priority: TECHNICAL_INSPECTION > DRIVER_LICENSE
                                  > INSURANCE > AGREEMENT > PAYMENT
                        Languages: PL, RU, UA, EN
                        │
                        ▼
                    ScanResult { extractedName, expiryDate, confidence,
                                 fullText, rawResponse, detectedCategory }
```

Backend proxy: GCP Cloud Function at `europe-central2`. Request: `{"rawDocument":{"content":"<base64>","mimeType":"image/jpeg"}}` + `X-API-Key` header. Returns standard Document AI `ProcessResponse` JSON.

## Dependency Injection

Koin modules, assembled in `AppModule.kt` as `appModules`:

```
appModules = [dataModule, domainModule, presentationModule, platformModule()]
```

| Module | Provides |
|---|---|
| `dataModule` | `Json`, `HttpClient` (Ktor), `DocumentDatabase`, `DocumentAiMapper`, `DocumentAiService`, `DocumentRepositoryImpl`, `OcrRepositoryImpl`, `UserProfileService` |
| `domainModule` | All 6 use cases (as `factory`) |
| `presentationModule` | `DocumentListViewModel`, `CategoryListViewModel`, `DetailViewModel`, `AuthViewModel` (via `viewModelOf`) |
| `platformModule()` | expect/actual — `PlatformContext`, `DatabaseDriverFactory`, `ImageStorage`, `NotificationScheduler`, `NotificationPermissionHandler`, `GoogleAuthProvider` |

`ProxyConfig` is injected separately in `TerminowoApp` (androidApp module) from `BuildConfig.PROXY_URL` + `BuildConfig.PROXY_API_KEY`.

## Platform Abstractions

Expect/actual interfaces bridging shared code to native APIs:

| Interface | Purpose | Android Actual |
|---|---|---|
| `PlatformContext` | Opaque context wrapper | Wraps `android.content.Context` |
| `DatabaseDriverFactory` | SQLite driver creation | `AndroidSqliteDriver`, DB `"document_scanner.db"` |
| `ImageStorage` | Save/read/delete images + thumbnails | File-based in `filesDir/documents/` and `filesDir/thumbnails/`, EXIF normalization, 200px JPEG thumbnails |
| `NotificationScheduler` | Schedule/cancel reminder alarms | `AlarmManager.setExactAndAllowWhileIdle`, `ReminderReceiver` BroadcastReceiver, `"document_reminders"` channel |
| `NotificationPermissionHandler` | Runtime notification permission | `POST_NOTIFICATIONS` on API 33+, launcher via `MainActivity` |
| `GoogleAuthProvider` | OAuth2 token acquisition | Google Play Services Identity, scopes: `openid`, `email`, `profile`; callback bridge via companion properties |
| `ImageDecoder` | `ByteArray` → `ImageBitmap` | `BitmapFactory.decodeByteArray` → `asImageBitmap()` |

## Localization

4 languages via Compose Multiplatform resources:

| Directory | Language |
|---|---|
| `shared/src/commonMain/composeResources/values/strings.xml` | English (default) |
| `shared/src/commonMain/composeResources/values-pl/strings.xml` | Polish |
| `shared/src/commonMain/composeResources/values-ru/strings.xml` | Russian |
| `shared/src/commonMain/composeResources/values-uk/strings.xml` | Ukrainian |

## Feature Flags

`config/FeatureFlags.kt`: `GOOGLE_SIGN_IN_ENABLED = false` — disables Google OAuth consent launcher wiring in `MainActivity` and hides login UI. `AuthViewModel`/`GoogleAuthProvider` code is present but dormant.

## File Tree

```
shared/src/commonMain/kotlin/com/stc/terminowo/
├── App.kt                                    App() composable entry point
├── config/
│   └── FeatureFlags.kt                       Feature toggles
├── di/
│   ├── AppModule.kt                          Assembles all Koin modules
│   ├── DataModule.kt                         Data layer bindings
│   ├── DomainModule.kt                       Use case bindings
│   └── PresentationModule.kt                 ViewModel bindings
├── data/
│   ├── local/
│   │   └── DatabaseDriverFactory.kt          expect class
│   ├── remote/
│   │   ├── DocumentAiConfig.kt               ProxyConfig data class
│   │   ├── DocumentAiDto.kt                  Request/response DTOs
│   │   ├── DocumentAiMapper.kt               OCR response → ScanResult
│   │   ├── DocumentAiService.kt              Ktor HTTP client
│   │   └── UserProfileService.kt             Google userinfo API
│   └── repository/
│       ├── DocumentRepositoryImpl.kt          SQLDelight CRUD
│       └── OcrRepositoryImpl.kt               Orchestrates service + mapper
├── domain/
│   ├── model/
│   │   ├── Document.kt
│   │   ├── DocumentCategory.kt
│   │   ├── ReminderInterval.kt
│   │   ├── ScanResult.kt
│   │   └── UserProfile.kt
│   ├── repository/
│   │   ├── DocumentRepository.kt              Interface
│   │   └── OcrRepository.kt                   Interface
│   └── usecase/
│       ├── DeleteDocumentUseCase.kt
│       ├── GetDocumentsUseCase.kt
│       ├── SaveDocumentUseCase.kt
│       ├── ScanDocumentUseCase.kt
│       ├── ScheduleRemindersUseCase.kt
│       └── UpdateDocumentUseCase.kt
├── platform/
│   ├── GoogleAuthProvider.kt                  expect class
│   ├── ImageDecoder.kt                        expect fun
│   ├── ImageStorage.kt                        expect class
│   ├── NotificationPermissionHandler.kt       expect class
│   ├── NotificationScheduler.kt               expect class
│   └── PlatformContext.kt                     expect class
└── presentation/
    ├── navigation/
    │   ├── NavGraph.kt                        Navigation host + routes
    │   └── Screen.kt                          Sealed interface (6 routes)
    ├── categories/
    │   ├── CategoryListScreen.kt
    │   └── CategoryListViewModel.kt
    ├── main/
    │   ├── MainScreen.kt                      DocumentListScreen
    │   └── MainViewModel.kt                   DocumentListViewModel
    ├── camera/
    │   └── CameraScreen.kt                    Placeholder
    ├── preview/
    │   └── ImagePreviewScreen.kt
    ├── detail/
    │   ├── DetailScreen.kt
    │   └── DetailViewModel.kt
    ├── auth/
    │   └── AuthViewModel.kt
    ├── components/
    │   ├── AccountIconButton.kt
    │   ├── DocumentListItem.kt
    │   ├── LoadingOverlay.kt
    │   └── ReminderChips.kt
    └── theme/
        └── Theme.kt                           TerminowoTheme (Material3)

shared/src/androidMain/kotlin/com/stc/terminowo/
├── data/local/
│   └── DatabaseDriverFactory.android.kt       actual — AndroidSqliteDriver
├── platform/
│   ├── PlatformContext.android.kt             actual — wraps Context
│   ├── ImageDecoder.android.kt                actual — BitmapFactory
│   ├── ImageStorage.android.kt                actual — file-based storage
│   ├── NotificationScheduler.android.kt       actual — AlarmManager + ReminderReceiver
│   ├── NotificationPermissionHandler.android.kt actual — POST_NOTIFICATIONS
│   └── GoogleAuthProvider.android.kt          actual — Play Services Identity
└── di/
    └── PlatformModule.android.kt              actual — Android Koin bindings

androidApp/src/androidMain/kotlin/com/stc/terminowo/android/
├── TerminowoApp.kt                            Application class (Koin init)
└── MainActivity.kt                            Activity (permission + auth launchers)
```
