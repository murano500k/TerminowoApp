# Changelog

## iOS 1.1.8 (14) — 2026-09-23

Changes since iOS 1.1.7 (13), released 2026-05-26. The Android changes are on `main` but haven't shipped yet; they go into the next Android release.

### Both platforms
- **Pick images from the gallery for OCR, converted to JPEG** — any image picked from the gallery or through "Choose File" is converted to JPEG before OCR. HEIC photos (the default camera format on iPhone, Pixel and Samsung) used to fail because Document AI doesn't accept HEIC. (`5635f85`)
- **Reminder notifications in the device language** — reminders follow the phone's current language. On iOS, all future reminders are re-scheduled at app launch, so reminders scheduled before a language change or by an older version are updated too. (`b37aadb`)
- **"Expires in N days" reminder text fixed** — on iOS the number of days was missing from the notification. (`b37aadb`)

### iOS
- Gallery picker switched to the system photo picker (`PHPickerViewController`). Presenting pickers and the camera no longer uses the deprecated `keyWindow` API, and the "Choose File" picker's delegate is retained while it's shown. (`5635f85`)

### Android
- **New: "Select Photo from Gallery"** in the add-document menu, using the system Photo Picker (no storage permission needed). Before this, gallery photos could only be added through "Choose File". (`5635f85`)
- Target API 36 (Android 16). (`f8c01c2`)
- Edge-to-edge setup without APIs deprecated in Android 15. (`97a8276`)
- Notification strings for all languages ship in the base APK (language splits disabled), and the reminder receiver creates the notification channel itself, so reminders are no longer dropped after a fresh process start. (`b37aadb`)

### Development
- iOS UI tests (`iosAppUITests`, `./run_ios_uitests.sh`) for the add-document flow on a connected device. (`4131f13`)
- Fixed iOS simulator builds that linked the device framework. (`4131f13`)
- fastlane App Store publishing (`./publish_testflight.sh --publish`), with separate Polish release notes (`--release-notes-pl`). (`5c0d416`)
- `run_ios.sh` allows provisioning updates. (`8d7894d`)
