#!/usr/bin/env bash
# device-state.sh — Dump the installed LaFromagerie app's version info and DataStore
# contents for whichever flavor (client/admin) is currently installed on the
# connected device/emulator.
#
# Usage: scripts/device-state.sh [package-name]
#   package-name defaults to com.mtdevelopment.lafromagerie (the only applicationId
#   this repo ships — client and admin share it, see fromagerie-run-and-operate).
#
# Fails gracefully (clear message, exit 0) when adb is missing, no device is
# attached, or the app isn't installed — this is meant to be safe to run from a
# zero-context session without a device around.

set -u

PACKAGE="${1:-com.mtdevelopment.lafromagerie}"

if ! command -v adb >/dev/null 2>&1; then
    echo "[device-state] adb not found on PATH. Install Android platform-tools or add it to PATH."
    echo "[device-state] Nothing to inspect without adb — stopping here."
    exit 0
fi

DEVICE_LINES="$(adb devices 2>/dev/null | tail -n +2 | sed '/^$/d')"

if [ -z "$DEVICE_LINES" ]; then
    echo "[device-state] No device or emulator attached (adb devices returned nothing)."
    echo "[device-state] Connect a device (or start an emulator) and re-run this script."
    exit 0
fi

DEVICE_COUNT="$(echo "$DEVICE_LINES" | wc -l | tr -d ' ')"
if [ "$DEVICE_COUNT" -gt 1 ]; then
    echo "[device-state] Warning: $DEVICE_COUNT devices attached; adb will pick its default target."
    echo "[device-state] Set ANDROID_SERIAL or pass -s <serial> to adb yourself if that's wrong."
fi

echo "=== Package info: $PACKAGE ==="
if ! adb shell dumpsys package "$PACKAGE" >/tmp/device-state-dumpsys.$$ 2>&1; then
    echo "[device-state] 'adb shell dumpsys package $PACKAGE' failed. Is a device really connected?"
    rm -f /tmp/device-state-dumpsys.$$
    exit 0
fi

if ! grep -q "Package \[$PACKAGE\]" /tmp/device-state-dumpsys.$$ 2>/dev/null && \
   ! grep -q "versionName" /tmp/device-state-dumpsys.$$ 2>/dev/null; then
    echo "[device-state] '$PACKAGE' does not appear to be installed on this device."
    echo "[device-state] Install it first: ./gradlew :app:installClientDebug (or installAdminDebug)."
    rm -f /tmp/device-state-dumpsys.$$
    exit 0
fi

grep -E "versionName|versionCode|firstInstallTime|lastUpdateTime" /tmp/device-state-dumpsys.$$ | sed 's/^[[:space:]]*/  /'
rm -f /tmp/device-state-dumpsys.$$

echo ""
echo "=== Current HEAD commit (for stale-APK comparison) ==="
if command -v git >/dev/null 2>&1 && git rev-parse --git-dir >/dev/null 2>&1; then
    git log -1 --format='  commit %h  %cd  %s' --date=iso-strict
    echo "[device-state] Compare 'lastUpdateTime' above against this commit date."
    echo "[device-state] If lastUpdateTime is older than a commit you expect to be testing,"
    echo "[device-state] the installed APK is STALE — reinstall before drawing conclusions."
    echo "[device-state] (See fromagerie-debugging-playbook: stale-APK trap.)"
else
    echo "  (not inside a git repo, or git not on PATH — skipping HEAD comparison)"
fi

echo ""
echo "=== DataStore files (debug builds only — requires run-as access) ==="
for ds in shared_settings checkout_settings admin_data; do
    echo "--- ${ds}.preferences_pb ---"
    if adb shell run-as "$PACKAGE" cat "files/datastore/${ds}.preferences_pb" 2>/tmp/device-state-ds.$$ | strings; then
        :
    else
        REASON="$(cat /tmp/device-state-ds.$$ 2>/dev/null)"
        if echo "$REASON" | grep -qi "run-as: Package .* is unknown\|not debuggable"; then
            echo "  (skipped: app is not debuggable, or run-as is not permitted for this package)"
        elif echo "$REASON" | grep -qi "No such file"; then
            echo "  (file does not exist yet — this DataStore key has never been written, e.g. empty cart)"
        else
            echo "  (skipped: $REASON)"
        fi
    fi
    rm -f /tmp/device-state-ds.$$
done

echo ""
echo "[device-state] Done."
