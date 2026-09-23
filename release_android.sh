#!/usr/bin/env bash
set -euo pipefail

GRADLE_FILE="androidApp/build.gradle.kts"

# Every release AAB is archived on the Linux build box.
RELEASE_HOST="artem@192.168.1.16"
RELEASE_HOST_IP="${RELEASE_HOST#*@}"
RELEASE_DIR="/home/artem/projects/TerminowoApp/google_release"

# True when one of this machine's IPv4 addresses is the release host's.
is_release_host() {
    { ip -4 -o addr show 2>/dev/null | awk '{print $4}'
      ifconfig 2>/dev/null | awk '/inet /{print $2}'
    } | cut -d/ -f1 | grep -qx "$RELEASE_HOST_IP"
}

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
ARCHIVE_NAME="terminowo-$NEW_NAME-$NEW_CODE.aab"

echo ""
echo "=== Archiving to $RELEASE_HOST:$RELEASE_DIR ==="
if is_release_host; then
    mkdir -p "$RELEASE_DIR"
    cp "$AAB_PATH" "$RELEASE_DIR/$ARCHIVE_NAME"
    ARCHIVED="$RELEASE_DIR/$ARCHIVE_NAME (local copy)"
elif ssh -o ConnectTimeout=10 "$RELEASE_HOST" "mkdir -p '$RELEASE_DIR'" &&
     scp -q "$AAB_PATH" "$RELEASE_HOST:$RELEASE_DIR/$ARCHIVE_NAME"; then
    ARCHIVED="$RELEASE_HOST:$RELEASE_DIR/$ARCHIVE_NAME"
else
    # The bundle is still usable locally, so don't fail the release over it.
    ARCHIVED="FAILED - copy it manually: scp $AAB_PATH $RELEASE_HOST:$RELEASE_DIR/$ARCHIVE_NAME"
fi

echo ""
echo "=== Release bundle ready ==="
echo "  Version:  $NEW_NAME (code $NEW_CODE)"
echo "  File:     $AAB_PATH ($SIZE)"
echo "  Archived: $ARCHIVED"
echo ""
echo "Upload to Google Play Console:"
echo "  https://play.google.com/console"
