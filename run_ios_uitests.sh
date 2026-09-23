#!/usr/bin/env bash
# Run iOS UI tests (iosAppUITests) on the connected iPhone/iPad.
# Usage: ./run_ios_uitests.sh [TestClass[/testMethod]]
# Requires Settings > Developer > Enable UI Automation on the device, and the device unlocked.
set -euo pipefail

SCHEME="iosApp"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
IOS_PROJECT_DIR="$PROJECT_DIR/iosApp"
ONLY_TESTING="iosAppUITests${1:+/$1}"

echo "=== Checking for connected iOS device ==="
device_id=$(xcrun xctrace list devices 2>/dev/null | grep -v "Simulator" | grep "iPhone\|iPad" | head -1 | sed 's/.*(\(.*\))/\1/' | tr -d '[:space:]')
if [ -z "$device_id" ]; then
    echo "ERROR: No physical iOS device found. Connect an iPhone/iPad via USB."
    exit 1
fi
echo "Found device: $device_id"

echo ""
echo "=== Unlocking keychain for codesigning ==="
security unlock-keychain -p "$KEYCHAIN_PASSWORD" ~/Library/Keychains/login.keychain-db

echo ""
echo "=== Running $ONLY_TESTING ==="
xcodebuild test \
    -project "$IOS_PROJECT_DIR/iosApp.xcodeproj" \
    -scheme "$SCHEME" \
    -destination "id=$device_id" \
    -derivedDataPath "$IOS_PROJECT_DIR/build" \
    -allowProvisioningUpdates \
    -only-testing:"$ONLY_TESTING" 2>&1 | grep -E "Test Case|error:|\*\* TEST"
