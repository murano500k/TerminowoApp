#!/usr/bin/env bash
set -euo pipefail

SCHEME="iosApp"
BUNDLE_ID="com.stc.terminowoapp"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
IOS_PROJECT_DIR="$PROJECT_DIR/iosApp"

echo "=== Checking for connected iOS device ==="
device_id=$(xcrun xctrace list devices 2>/dev/null | grep -v "Simulator" | grep "iPhone\|iPad" | head -1 | sed 's/.*(\(.*\))/\1/' | tr -d '[:space:]')
if [ -z "$device_id" ]; then
    echo "ERROR: No physical iOS device found. Connect an iPhone/iPad via USB."
    echo ""
    echo "Available devices:"
    xcrun xctrace list devices 2>/dev/null | head -20
    exit 1
fi
device_name=$(xcrun xctrace list devices 2>/dev/null | grep -v "Simulator" | grep "iPhone\|iPad" | head -1 | sed 's/ (.*//')
echo "Found device: $device_name ($device_id)"

echo ""
echo "=== Building shared framework ==="
"$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" :shared:linkDebugFrameworkIosArm64 :shared:iosArm64AggregateResources

echo ""
echo "=== Unlocking keychain for codesigning ==="
security unlock-keychain -p "$KEYCHAIN_PASSWORD" ~/Library/Keychains/login.keychain-db

echo ""
echo "=== Building iOS app ==="
xcodebuild \
    -project "$IOS_PROJECT_DIR/iosApp.xcodeproj" \
    -scheme "$SCHEME" \
    -configuration Debug \
    -destination "id=$device_id" \
    -derivedDataPath "$IOS_PROJECT_DIR/build" \
    build 2>&1 | tail -20

echo ""
echo "=== Installing and launching on device ==="
app_path=$(find "$IOS_PROJECT_DIR/build/Build/Products/Debug-iphoneos" -name "*.app" -maxdepth 1 | head -1)
if [ -z "$app_path" ]; then
    echo "ERROR: Could not find built .app bundle"
    exit 1
fi

xcrun devicectl device install app --device "$device_id" "$app_path" 2>&1 | tail -5
echo ""
xcrun devicectl device process launch --device "$device_id" "$BUNDLE_ID" 2>&1 | tail -5

echo ""
echo "=== Done! ==="
