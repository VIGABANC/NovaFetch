#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/smoke"
mkdir -p "$OUT"
kotlinc \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/DownloadPlan.kt" \
  "$ROOT/library/src/main/kotlin/app/novafetch/library/FinalMediaMetadata.kt" \
  "$ROOT/app/src/main/kotlin/app/novafetch/FinalMediaMetadataFactory.kt" \
  "$ROOT/app/src/test/kotlin/app/novafetch/FinalMediaMetadataFactoryTest.kt" \
  -include-runtime -d "$OUT/metadata-factory-test.jar"
java -jar "$OUT/metadata-factory-test.jar"
