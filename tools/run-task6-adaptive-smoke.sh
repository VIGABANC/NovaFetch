#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/smoke"
mkdir -p "$OUT"
kotlinc \
  "$ROOT/common/src/main/kotlin/app/novafetch/common/DownloadId.kt" \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/DownloadScheduler.kt" \
  "$ROOT/download-android/src/main/kotlin/app/novafetch/download/DownloadSchedulerPolicy.kt" \
  "$ROOT/download-android/src/main/kotlin/app/novafetch/download/AdaptiveDownloadScheduler.kt" \
  "$ROOT/download-android/src/test/kotlin/app/novafetch/download/AdaptiveDownloadSchedulerTest.kt" \
  -include-runtime -d "$OUT/adaptive-scheduler-test.jar"
java -jar "$OUT/adaptive-scheduler-test.jar"
