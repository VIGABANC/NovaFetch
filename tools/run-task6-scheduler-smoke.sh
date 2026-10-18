#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/smoke"
mkdir -p "$OUT"
kotlinc \
  "$ROOT/download-android/src/main/kotlin/app/novafetch/download/DownloadSchedulerPolicy.kt" \
  "$ROOT/download-android/src/test/kotlin/app/novafetch/download/DownloadSchedulerSelectionTest.kt" \
  -include-runtime -d "$OUT/scheduler-selection-test.jar"
java -jar "$OUT/scheduler-selection-test.jar"
