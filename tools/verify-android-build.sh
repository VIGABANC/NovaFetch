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

"${GRADLE[@]}" \
  :app:assembleDebug \
  :app:compileDebugAndroidTestKotlin \
  :data:compileDebugAndroidTestKotlin \
  :library:compileDebugAndroidTestKotlin \
  :download-android:compileDebugAndroidTestKotlin \
  :browser-core:compileDebugAndroidTestKotlin
