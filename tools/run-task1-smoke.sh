#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/smoke"
mkdir -p "$OUT"

cat > "$OUT/DownloadIdRunner.kt" <<'KOT'
import app.novafetch.common.testDownloadId
fun main() { testDownloadId(); println("DownloadIdTest: PASS") }
KOT

kotlinc "$ROOT/common/src/main/kotlin/app/novafetch/common/DownloadState.kt" \
  "$ROOT/common/src/test/kotlin/app/novafetch/common/DownloadStateTest.kt" \
  -include-runtime -d "$OUT/download-state-test.jar"
java -jar "$OUT/download-state-test.jar"

kotlinc "$ROOT/common/src/main/kotlin/app/novafetch/common/DownloadId.kt" \
  "$ROOT/common/src/test/kotlin/app/novafetch/common/DownloadIdTest.kt" \
  "$OUT/DownloadIdRunner.kt" \
  -include-runtime -d "$OUT/download-id-test.jar"
java -jar "$OUT/download-id-test.jar"

kotlinc "$ROOT/download-core/src/main/kotlin/app/novafetch/download/DownloadPlan.kt" \
  "$ROOT/download-core/src/test/kotlin/app/novafetch/download/DownloadPlanTest.kt" \
  -include-runtime -d "$OUT/download-plan-test.jar"
java -jar "$OUT/download-plan-test.jar"
