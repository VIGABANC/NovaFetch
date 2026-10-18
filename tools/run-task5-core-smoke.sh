#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/smoke"
mkdir -p "$OUT"
kotlinc \
  "$ROOT/tools/kotlin-stubs/kotlinx/coroutines/flow/Flow.kt" \
  "$ROOT/common/src/main/kotlin/app/novafetch/common/DownloadId.kt" \
  "$ROOT/common/src/main/kotlin/app/novafetch/common/DownloadState.kt" \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/DownloadPlan.kt" \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/DownloadStateMachine.kt" \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/DownloadSnapshot.kt" \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/DownloadRepository.kt" \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/DownloadScheduler.kt" \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/RetryPolicy.kt" \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/DownloadCoordinator.kt" \
  "$ROOT/download-core/src/test/kotlin/app/novafetch/download/DownloadCoordinatorTest.kt" \
  -include-runtime -d "$OUT/coordinator-test.jar"
java -jar "$OUT/coordinator-test.jar"
