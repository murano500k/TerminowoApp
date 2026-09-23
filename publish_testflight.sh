#!/usr/bin/env bash
set -euo pipefail

#
# publish_testflight.sh — Build, archive, and upload iOS app to TestFlight.
# Optionally submits the build to the App Store for production release.
#
# Usage:
#   ./publish_testflight.sh                          # auto-increment build, TestFlight only
#   ./publish_testflight.sh --build 5                # set specific build number
#   ./publish_testflight.sh --version 1.1.7          # set specific marketing version
#   ./publish_testflight.sh --skip-framework         # skip Gradle shared framework build
#   ./publish_testflight.sh --publish                # also submit to App Store for review
#   ./publish_testflight.sh --publish-only           # submit existing build to App Store (no rebuild)
#   ./publish_testflight.sh --publish --release-notes "What's new..."
#   ./publish_testflight.sh --publish --release-notes "What's new..." --release-notes-pl "Co nowego..."
#
# Required env vars (loaded from ~/.zshrc):
#   KEYCHAIN_PASSWORD       — for codesigning
#   ASC_API_KEY_ID          — App Store Connect API key ID
#   ASC_API_ISSUER_ID       — App Store Connect issuer ID
#   ASC_API_KEY_PATH        — optional, path to AuthKey_<ID>.p8 (auto-discovered otherwise)
#

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
IOS_PROJECT_DIR="$PROJECT_DIR/iosApp"
SCHEME="iosApp"
INFO_PLIST="$IOS_PROJECT_DIR/iosApp/Info.plist"
EXPORT_OPTIONS="$IOS_PROJECT_DIR/ExportOptions.plist"
ARCHIVE_DIR="$IOS_PROJECT_DIR/build/archive"
EXPORT_DIR="$IOS_PROJECT_DIR/build/export"
APP_BUNDLE_ID="com.stc.terminowoapp"

SKIP_FRAMEWORK=false
CUSTOM_BUILD=""
CUSTOM_VERSION=""
PUBLISH=false
PUBLISH_ONLY=false
RELEASE_NOTES="${ASC_RELEASE_NOTES:-Bug fixes and improvements}"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --build)
            CUSTOM_BUILD="$2"
            shift 2
            ;;
        --version)
            CUSTOM_VERSION="$2"
            shift 2
            ;;
        --skip-framework)
            SKIP_FRAMEWORK=true
            shift
            ;;
        --publish)
            PUBLISH=true
            shift
            ;;
        --publish-only)
            PUBLISH=true
            PUBLISH_ONLY=true
            SKIP_FRAMEWORK=true
            shift
            ;;
        --release-notes)
            RELEASE_NOTES="$2"
            shift 2
            ;;
        --release-notes-pl)
            RELEASE_NOTES_PL="$2"
            shift 2
            ;;
        -h|--help)
            head -28 "$0" | tail -26
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# --- Resolve App Store Connect API key (used by both upload and publish) ---
if [[ -z "${ASC_API_KEY_ID:-}" || -z "${ASC_API_ISSUER_ID:-}" ]]; then
    echo "ERROR: ASC_API_KEY_ID and ASC_API_ISSUER_ID must be set (export them in ~/.zshrc)."
    exit 1
fi
if [[ -n "${ASC_API_KEY_PATH:-}" ]]; then
    ASC_KEY_PATH="$ASC_API_KEY_PATH"
else
    for candidate in \
        "$HOME/.private_keys/AuthKey_${ASC_API_KEY_ID}.p8" \
        "$HOME/.appstoreconnect/private_keys/AuthKey_${ASC_API_KEY_ID}.p8" \
        "$HOME/private_keys/AuthKey_${ASC_API_KEY_ID}.p8"; do
        if [[ -f "$candidate" ]]; then
            ASC_KEY_PATH="$candidate"
            break
        fi
    done
fi
if [[ -z "${ASC_KEY_PATH:-}" || ! -f "$ASC_KEY_PATH" ]]; then
    echo "ERROR: API key AuthKey_${ASC_API_KEY_ID}.p8 not found. Set ASC_API_KEY_PATH or place it in ~/.private_keys/ or ~/.appstoreconnect/private_keys/"
    exit 1
fi
echo "Using App Store Connect API key: $ASC_API_KEY_ID"

# --- Step 1: Set version + build number ---
CURRENT_BUILD=$(/usr/libexec/PlistBuddy -c "Print CFBundleVersion" "$INFO_PLIST")
MARKETING_VERSION=$(/usr/libexec/PlistBuddy -c "Print CFBundleShortVersionString" "$INFO_PLIST")

if [[ "$PUBLISH_ONLY" == true ]]; then
    NEW_BUILD="$CURRENT_BUILD"
    echo "=== publish-only mode: using existing version $MARKETING_VERSION ($CURRENT_BUILD) ==="
else
    if [[ -n "$CUSTOM_VERSION" ]]; then
        MARKETING_VERSION="$CUSTOM_VERSION"
        /usr/libexec/PlistBuddy -c "Set :CFBundleShortVersionString $MARKETING_VERSION" "$INFO_PLIST"
    fi
    if [[ -n "$CUSTOM_BUILD" ]]; then
        NEW_BUILD="$CUSTOM_BUILD"
    else
        NEW_BUILD=$((CURRENT_BUILD + 1))
    fi
    /usr/libexec/PlistBuddy -c "Set :CFBundleVersion $NEW_BUILD" "$INFO_PLIST"
    echo "=== Version: $MARKETING_VERSION ($NEW_BUILD) ==="
fi

if [[ "$PUBLISH_ONLY" == false ]]; then
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
    echo ""
    echo "=== Exporting and uploading to App Store Connect ==="
    rm -rf "$EXPORT_DIR"

    xcodebuild -exportArchive \
        -archivePath "$ARCHIVE_PATH" \
        -exportPath "$EXPORT_DIR" \
        -exportOptionsPlist "$EXPORT_OPTIONS" \
        -allowProvisioningUpdates \
        -authenticationKeyPath "$ASC_KEY_PATH" \
        -authenticationKeyID "$ASC_API_KEY_ID" \
        -authenticationKeyIssuerID "$ASC_API_ISSUER_ID" \
        2>&1 | tail -30

    if [[ ${PIPESTATUS[0]} -ne 0 ]]; then
        echo "ERROR: Export/upload failed"
        exit 1
    fi
    echo ""
    echo "=== Uploaded $MARKETING_VERSION ($NEW_BUILD) to TestFlight ==="
fi

# --- Step 6: Submit to App Store via fastlane deliver ---
if [[ "$PUBLISH" == true ]]; then
    echo ""
    echo "=== Submitting $MARKETING_VERSION ($NEW_BUILD) to App Store for review ==="

    if ! command -v fastlane >/dev/null 2>&1; then
        echo "ERROR: fastlane not installed. Run: brew install fastlane"
        exit 1
    fi

    # Ensure metadata directories exist so deliver's metadata upload step doesn't error
    # (the directories are git-tracked as empty so this is normally a no-op).
    mkdir -p "$IOS_PROJECT_DIR/fastlane/metadata/en-US" "$IOS_PROJECT_DIR/fastlane/metadata/pl"

    # Wait for the build to finish processing on App Store Connect.
    # deliver can only attach a build that's already in a 'VALID' state.
    if [[ "$PUBLISH_ONLY" == false ]]; then
        WAIT_SECONDS="${ASC_PROCESS_WAIT:-180}"
        echo "Waiting ${WAIT_SECONDS}s for build to finish processing on App Store Connect..."
        sleep "$WAIT_SECONDS"
    fi

    # Run from iOS project dir so fastlane picks up iosApp/fastlane/Fastfile.
    cd "$IOS_PROJECT_DIR"
    ASC_APP_VERSION="$MARKETING_VERSION" \
    ASC_BUILD_NUMBER="$NEW_BUILD" \
    ASC_RELEASE_NOTES="$RELEASE_NOTES" \
    ASC_RELEASE_NOTES_PL="${RELEASE_NOTES_PL:-$RELEASE_NOTES}" \
    ASC_API_KEY_ID="$ASC_API_KEY_ID" \
    ASC_API_ISSUER_ID="$ASC_API_ISSUER_ID" \
    ASC_API_KEY_PATH="$ASC_KEY_PATH" \
        fastlane publish

    echo ""
    echo "=== Submitted! Version $MARKETING_VERSION ($NEW_BUILD) is in App Review ==="
    echo "Apple review typically takes 24-48 hours. The app will auto-release on approval."
else
    echo "It will appear in App Store Connect after processing (usually 5-15 minutes)."
    echo "To submit this build to the App Store: ./publish_testflight.sh --publish-only"
fi
