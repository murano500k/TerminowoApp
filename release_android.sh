#!/usr/bin/env bash
set -euo pipefail

GRADLE_FILE="androidApp/build.gradle.kts"

# Extract current versionCode and versionName
# (perl instead of grep -P / sed -i so this works with macOS BSD tools)
CURRENT_CODE=$(perl -ne 'print $1 if /versionCode\s*=\s*(\d+)/' "$GRADLE_FILE")
CURRENT_NAME=$(perl -ne 'print $1 if /versionName\s*=\s*"([^"]+)"/' "$GRADLE_FILE")

echo "=== Current version: $CURRENT_NAME (code $CURRENT_CODE) ==="

# Bump versionCode
NEW_CODE=$((CURRENT_CODE + 1))

# Parse semver and bump patch by default
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_NAME"
NEW_PATCH=$((PATCH + 1))
DEFAULT_NAME="$MAJOR.$MINOR.$NEW_PATCH"

# Allow override: ./release_android.sh 1.2.0
if [ $# -ge 1 ]; then
    NEW_NAME="$1"
else
    read -rp "New versionName [$DEFAULT_NAME]: " INPUT_NAME
    NEW_NAME="${INPUT_NAME:-$DEFAULT_NAME}"
fi

echo "=== Bumping to: $NEW_NAME (code $NEW_CODE) ==="

# Update build.gradle.kts
perl -pi -e "s/versionCode\\s*=\\s*$CURRENT_CODE\\b/versionCode = $NEW_CODE/" "$GRADLE_FILE"
perl -pi -e "s/versionName\\s*=\\s*\"\\Q$CURRENT_NAME\\E\"/versionName = \"$NEW_NAME\"/" "$GRADLE_FILE"

echo "=== Building release bundle ==="
./gradlew :androidApp:bundleRelease

AAB_PATH="androidApp/build/outputs/bundle/release/androidApp-release.aab"
SIZE=$(du -h "$AAB_PATH" | cut -f1)

echo ""
echo "=== Release bundle ready ==="
echo "  Version: $NEW_NAME (code $NEW_CODE)"
echo "  File:    $AAB_PATH ($SIZE)"
echo ""
echo "Upload to Google Play Console:"
echo "  https://play.google.com/console"
