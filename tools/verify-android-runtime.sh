#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -x ./gradlew ]]; then
  GRADLE=(./gradlew)
elif command -v gradle >/dev/null 2>&1; then
  GRADLE=(gradle)
else
  echo "ERROR: Gradle is unavailable. Install Android Studio/Gradle or restore the Gradle wrapper JAR/scripts." >&2
  exit 2
fi

if ! command -v adb >/dev/null 2>&1; then
  echo "ERROR: adb is unavailable. Start an Android emulator/device from Android Studio." >&2
  exit 2
fi

if ! adb get-state >/dev/null 2>&1; then
  echo "ERROR: no Android device/emulator is connected." >&2
  exit 2
fi

"${GRADLE[@]}" \
  :data:connectedDebugAndroidTest \
  :library:connectedDebugAndroidTest \
  :download-android:connectedDebugAndroidTest
