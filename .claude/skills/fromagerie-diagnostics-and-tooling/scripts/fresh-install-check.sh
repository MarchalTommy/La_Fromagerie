#!/usr/bin/env bash
# fresh-install-check.sh — Guard against the stale-APK trap (see
# fromagerie-debugging-playbook and fromagerie-failure-archaeology §6, the
# June-2026 cart-emptying incident). Compares the installed app's
# lastUpdateTime against the latest local commit date and warns loudly if the
# install predates the commit — meaning you would be debugging OLD code while
# believing it's current.
#
# Usage: scripts/fresh-install-check.sh [package-name]
#
# Exit codes:
#   0 = check ran (whether "fresh" or "stale" — read the printed verdict)
#   0 = also used for the "couldn't check" graceful paths (no device, no adb,
#       not a git repo, app not installed) — this script never treats an
#       environment limitation as a hard failure.

set -u

PACKAGE="${1:-com.mtdevelopment.lafromagerie}"

if ! command -v adb >/dev/null 2>&1; then
    echo "[fresh-install-check] adb not found on PATH. Cannot check installed APK state."
    echo "[fresh-install-check] Install Android platform-tools or add it to PATH, then re-run."
    exit 0
fi

if ! command -v git >/dev/null 2>&1 || ! git rev-parse --git-dir >/dev/null 2>&1; then
    echo "[fresh-install-check] Not inside a git repository (or git not on PATH)."
    echo "[fresh-install-check] Cannot compare against a commit date — nothing to check."
    exit 0
fi

DEVICE_LINES="$(adb devices 2>/dev/null | tail -n +2 | sed '/^$/d')"
if [ -z "$DEVICE_LINES" ]; then
    echo "[fresh-install-check] No device or emulator attached (adb devices returned nothing)."
    echo "[fresh-install-check] Connect a device and re-run before trusting any on-device repro."
    exit 0
fi

DUMP="$(adb shell dumpsys package "$PACKAGE" 2>/dev/null)"
if [ -z "$DUMP" ] || ! echo "$DUMP" | grep -q "lastUpdateTime"; then
    echo "[fresh-install-check] '$PACKAGE' is not installed on the attached device."
    echo "[fresh-install-check] Nothing to compare — install it first:"
    echo "[fresh-install-check]   ./gradlew :app:installClientDebug   (or installAdminDebug)"
    exit 0
fi

# lastUpdateTime lines look like: "    lastUpdateTime=2026-07-06 14:32:10"
RAW_UPDATE_LINE="$(echo "$DUMP" | grep "lastUpdateTime" | head -1)"
INSTALL_DATE_STR="$(echo "$RAW_UPDATE_LINE" | sed -E 's/.*lastUpdateTime=//' | awk '{print $1" "$2}')"

if [ -z "$INSTALL_DATE_STR" ]; then
    echo "[fresh-install-check] Could not parse lastUpdateTime from dumpsys output:"
    echo "  $RAW_UPDATE_LINE"
    echo "[fresh-install-check] Falling back: inspect it yourself with:"
    echo "  adb shell dumpsys package $PACKAGE | grep -A2 lastUpdateTime"
    exit 0
fi

# Try GNU date first, then BSD/macOS date, for portability.
INSTALL_EPOCH="$(date -d "$INSTALL_DATE_STR" +%s 2>/dev/null || date -j -f "%Y-%m-%d %H:%M:%S" "$INSTALL_DATE_STR" +%s 2>/dev/null || echo "")"

if [ -z "$INSTALL_EPOCH" ]; then
    echo "[fresh-install-check] Could not parse install date '$INSTALL_DATE_STR' with 'date'."
    echo "[fresh-install-check] Raw dumpsys line: $RAW_UPDATE_LINE"
    exit 0
fi

COMMIT_EPOCH="$(git log -1 --format=%ct)"
COMMIT_HASH="$(git log -1 --format=%h)"
COMMIT_DATE="$(git log -1 --format=%cd --date=iso-strict)"

echo "=== Fresh-install check for $PACKAGE ==="
echo "  Installed APK lastUpdateTime : $INSTALL_DATE_STR"
echo "  Latest local commit          : $COMMIT_HASH  $COMMIT_DATE"

if [ "$INSTALL_EPOCH" -lt "$COMMIT_EPOCH" ]; then
    echo ""
    echo "  *** WARNING: the installed APK predates the latest local commit. ***"
    echo "  *** You are likely testing STALE code — this is the trap that caused ***"
    echo "  *** the June-2026 cart-emptying incident (see fromagerie-failure-archaeology). ***"
    echo "  Reinstall before debugging further:"
    echo "    ./gradlew :app:installClientDebug     (or installAdminDebug)"
else
    echo ""
    echo "  OK: installed APK is at least as new as the latest local commit."
    echo "  (This does not guarantee it was built FROM this exact commit if you have"
    echo "   uncommitted local changes — it only rules out a stale, older install.)"
fi
