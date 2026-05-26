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
./run_android.sh                                       # Build + install + launch Android (checks for connected device)
./run_ios.sh                                           # Build + install + launch iOS (checks for connected iPhone/iPad)
./release_android.sh                                   # Bump version + build signed release AAB for Google Play
./release_android.sh 1.2.0                             # Same, but with explicit versionName
./publish_testflight.sh                                # iOS: auto-bump build + upload to TestFlight
./publish_testflight.sh --version 1.2.0 --publish      # iOS: full build + upload + submit to App Store (auto-release on approval)
./publish_testflight.sh --publish-only                 # iOS: re-submit existing build (skips rebuild)
```

**Manual testing (Android)**: Use `./run_android.sh` to deploy and test on a connected Android device. Builds debug APK, installs, and launches.

**Manual testing (iOS)**: Use `./run_ios.sh` to deploy and test on a connected iOS device. Builds shared framework (`linkDebugFrameworkIosArm64` + `iosArm64AggregateResources`), then builds the Xcode project and installs via `devicectl`.

**Release (Android)**: Use `./release_android.sh` to build a signed AAB for Google Play. Auto-bumps `versionCode` and patches `versionName` (or pass explicit version as argument). Output: `androidApp/build/outputs/bundle/release/androidApp-release.aab`.

**Release (iOS)**: Use `./publish_testflight.sh` to build a signed release IPA, archive it, and upload to App Store Connect.

- Default: TestFlight only. Auto-increments `CFBundleVersion` (build number) in `iosApp/iosApp/Info.plist`.
- `--version X.Y.Z`: also bump `CFBundleShortVersionString` (marketing version).
- `--build N`: pin a specific build number instead of auto-increment.
- `--skip-framework`: skip the Gradle shared-framework build (use existing `.framework`).
- `--publish`: after the TestFlight upload, run `fastlane publish` (lane in `iosApp/fastlane/Fastfile`) to create the new App Store version, attach the build, upload release notes, and submit for review with auto-release on approval.
- `--publish-only`: skip rebuild/upload — only submit the already-uploaded build to App Store. Useful for retrying the submit step.
- `--release-notes "..."`: text used for both `en-US` and `pl` release notes. Defaults to "Bug fixes and improvements".

Requires `xcodegen`, `fastlane`, and `jq` installed (`brew install xcodegen fastlane jq`). If `iosApp/project.yml` changes (team, bundle id, SDK deps), run `cd iosApp && xcodegen generate` to regenerate the xcodeproj.

iOS signing team: **D SP Z O O** (`DMPH957CKJ`), bundle ID **`com.stc.terminowoapp`**, signed with Mikhail Dzenisiuk's Apple Development cert (cloud signing handled by the ASC API key during upload).

## Secrets & Credentials

All secrets are kept out of git. The build scripts read them from these locations:

**Shell env vars (`~/.zshrc`):**

```bash
export KEYCHAIN_PASSWORD="…"               # Login keychain password — used to unlock keychain for codesigning.
export ASC_API_KEY_ID="PJQKQBS42M"         # App Store Connect API key ID (must have App Manager role).
export ASC_API_ISSUER_ID="ef1b3dc3-…"      # ASC API issuer ID (team-wide).
# Optional:
export ASC_API_KEY_PATH="…/AuthKey_X.p8"   # Override .p8 location; auto-discovered otherwise.
export ASC_RELEASE_NOTES="…"               # Default release notes if --release-notes not passed.
```

**Files on disk (gitignored):**

| File | Purpose |
|---|---|
| `~/.private_keys/AuthKey_PJQKQBS42M.p8` | App Store Connect API key. The script also searches `~/.appstoreconnect/private_keys/` and `~/private_keys/`. **Use the `PJQKQBS42M` key for uploads**, not `R5FLYTK346` — the latter authenticates but lacks cert-management role so distribution fails. |
| `iosApp/Secrets.xcconfig` | iOS build-time secrets injected into `Info.plist`: `PROXY_URL`, `PROXY_API_KEY`, `FB_APP_ID`, `FB_CLIENT_TOKEN`. Copy from `iosApp/Secrets.xcconfig.template` and fill in. |
| `androidApp/google-services.json` (or similar) | Android Firebase / Google config (if present). |
| `*.jks` keystores | Android signing keystore for release builds (gitignored). |
| `client_secret*.json`, `client_*.plist`, `service-account*.json` | OAuth / GCP service-account credentials. |

**Codesigning identity (macOS Keychain):** "Apple Development: Mikhail Dzenisiuk" cert must be installed in `login.keychain-db` for iOS signing to work. App Store distribution uses cloud signing via the ASC API key — no local Apple Distribution cert needed.

**Rotation:** if any of these change, update both `~/.zshrc` (env vars) and the relevant file on disk; no code changes are needed because the scripts read from these locations dynamically.

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

## UI / Theming

- **All colors must be theme-aware.** Never use hardcoded colors like `Color.White`, `Color.Black`, `Color(0xFFF5F5F5)`, etc. for backgrounds, surfaces, borders, or text. Always use `MaterialTheme.colorScheme` tokens (e.g., `surface`, `onSurface`, `outlineVariant`, `surfaceContainerLow`). Exception: semantic status colors (red/orange/green indicators) and intentionally dark components (e.g., fullscreen image viewer) may remain hardcoded.

## Gotchas

- **Clock conflict**: `kotlinx.datetime.Clock` conflicts with `kotlin.time.Clock` in Kotlin 2.1.x. Always use `import kotlinx.datetime.Clock as DateTimeClock`.
- **KMP source layout**: Android sources go in `androidMain/` not `main/` — applies to both `:shared` and `:androidApp` modules.
- **Compiler flags**: `-Xexpect-actual-classes` is set in shared/build.gradle.kts. Opt-ins: `ExperimentalTime`, `ExperimentalUuidApi`.
- **CameraK requires compileSdk 36**: Transitively pulls `activity-compose:1.11.0`.
- **iOS targets**: Configured in shared/build.gradle.kts but only compile on macOS (skipped on Linux).
- **Alarmee removed**: Required Kotlin 2.2.20+. Notifications use native AlarmManager via expect/actual instead.
- **ProGuard**: Release builds have minification enabled. Rules keep Ktor, kotlinx-serialization, and serializer classes (`androidApp/proguard-rules.pro`). **Bug**: ProGuard rules still reference `com.docscanner.**` instead of `com.stc.terminowo.**` — needs fixing before release builds work correctly.

## What's Not Done Yet

- iOS polish and feature parity with Android
