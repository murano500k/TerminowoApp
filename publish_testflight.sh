#!/usr/bin/env bash
set -euo pipefail

#
# publish_testflight.sh — Build, archive, and upload iOS app to TestFlight.
#
# Usage:
#   ./publish_testflight.sh                  # auto-increment build number
#   ./publish_testflight.sh --build 5        # set specific build number
#   ./publish_testflight.sh --skip-framework # skip Gradle shared framework build
#

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
IOS_PROJECT_DIR="$PROJECT_DIR/iosApp"
SCHEME="iosApp"
INFO_PLIST="$IOS_PROJECT_DIR/iosApp/Info.plist"
EXPORT_OPTIONS="$IOS_PROJECT_DIR/ExportOptions.plist"
ARCHIVE_DIR="$IOS_PROJECT_DIR/build/archive"
EXPORT_DIR="$IOS_PROJECT_DIR/build/export"

SKIP_FRAMEWORK=false
CUSTOM_BUILD=""

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --build)
            CUSTOM_BUILD="$2"
            shift 2
            ;;
        --skip-framework)
            SKIP_FRAMEWORK=true
            shift
            ;;
        -h|--help)
            head -16 "$0" | tail -14
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# --- Step 1: Increment build number ---
CURRENT_BUILD=$(/usr/libexec/PlistBuddy -c "Print CFBundleVersion" "$INFO_PLIST")
MARKETING_VERSION=$(/usr/libexec/PlistBuddy -c "Print CFBundleShortVersionString" "$INFO_PLIST")

if [[ -n "$CUSTOM_BUILD" ]]; then
    NEW_BUILD="$CUSTOM_BUILD"
else
    NEW_BUILD=$((CURRENT_BUILD + 1))
fi

/usr/libexec/PlistBuddy -c "Set :CFBundleVersion $NEW_BUILD" "$INFO_PLIST"
echo "=== Version: $MARKETING_VERSION ($NEW_BUILD) ==="

# --- Step 2: Build shared KMP framework ---
if [[ "$SKIP_FRAMEWORK" == false ]]; then
    echo ""
    echo "=== Building shared release framework ==="
    "$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" \
        :shared:linkReleaseFrameworkIosArm64 \
        :shared:iosArm64AggregateResources
else
    echo ""
    echo "=== Skipping shared framework build ==="
fi

# --- Step 3: Unlock keychain ---
echo ""
echo "=== Unlocking keychain for codesigning ==="
security unlock-keychain -p "$KEYCHAIN_PASSWORD" ~/Library/Keychains/login.keychain-db

# --- Step 4: Archive ---
echo ""
echo "=== Archiving $SCHEME (Release) ==="
ARCHIVE_PATH="$ARCHIVE_DIR/iosApp-${MARKETING_VERSION}-${NEW_BUILD}.xcarchive"

xcodebuild archive \
    -project "$IOS_PROJECT_DIR/iosApp.xcodeproj" \
    -scheme "$SCHEME" \
    -configuration Release \
    -archivePath "$ARCHIVE_PATH" \
    -destination "generic/platform=iOS" \
    CODE_SIGN_STYLE=Automatic \
    2>&1 | tail -30

if [[ ! -d "$ARCHIVE_PATH" ]]; then
    echo "ERROR: Archive failed — $ARCHIVE_PATH not found"
    exit 1
fi
echo "Archive created: $ARCHIVE_PATH"

# --- Step 5: Export and upload ---
#
# ExportOptions.plist has destination=upload, so xcodebuild -exportArchive
# uploads directly to App Store Connect during export. No separate altool step needed.
#
echo ""
echo "=== Exporting and uploading to App Store Connect ==="
rm -rf "$EXPORT_DIR"

xcodebuild -exportArchive \
    -archivePath "$ARCHIVE_PATH" \
    -exportPath "$EXPORT_DIR" \
    -exportOptionsPlist "$EXPORT_OPTIONS" \
    -allowProvisioningUpdates \
    2>&1 | tail -30

if [[ ${PIPESTATUS[0]} -ne 0 ]]; then
    echo "ERROR: Export/upload failed"
    exit 1
fi

echo ""
echo "=== Done! Version $MARKETING_VERSION ($NEW_BUILD) uploaded to TestFlight ==="
echo "It will appear in App Store Connect after processing (usually 5-15 minutes)."
