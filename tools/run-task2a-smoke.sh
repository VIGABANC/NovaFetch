#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/smoke"
mkdir -p "$OUT"
kotlinc \
  "$ROOT/common/src/main/kotlin/app/novafetch/common/DownloadState.kt" \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/DownloadStateMachine.kt" \
  "$ROOT/download-core/src/test/kotlin/app/novafetch/download/DownloadStateMachineTest.kt" \
  -include-runtime -d "$OUT/state-machine.jar"
java -jar "$OUT/state-machine.jar"
