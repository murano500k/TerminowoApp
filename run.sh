#!/usr/bin/env bash
set -euo pipefail

PACKAGE="com.stc.terminowo.android"
ACTIVITY="${PACKAGE}.MainActivity"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== Checking for connected device ==="
device_count=$(adb devices | grep -cw 'device' || true)
if [ "$device_count" -eq 0 ]; then
    echo "ERROR: No Android device connected. Connect a device or start an emulator."
    exit 1
fi
adb devices | grep -w 'device' | head -1

echo ""
echo "=== Building debug APK ==="
"$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" :androidApp:assembleDebug

echo ""
echo "=== Installing on device ==="
adb install -r --no-streaming "$PROJECT_DIR/androidApp/build/outputs/apk/debug/androidApp-debug.apk"

echo ""
echo "=== Launching app ==="
adb shell am start -n "${PACKAGE}/${ACTIVITY}"

echo ""
echo "=== Done! ==="
