#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/smoke"
mkdir -p "$OUT"
kotlinc \
  "$ROOT/common/src/main/kotlin/app/novafetch/common/DownloadId.kt" \
  "$ROOT/library/src/main/kotlin/app/novafetch/library/FinalMediaMetadata.kt" \
  "$ROOT/library/src/main/kotlin/app/novafetch/library/SharedMediaPublisher.kt" \
  "$ROOT/library/src/main/kotlin/app/novafetch/library/MediaStoreSharedMediaPublisher.kt" \
  "$ROOT/library/src/test/kotlin/app/novafetch/library/MediaStoreSharedMediaPublisherTest.kt" \
  -include-runtime -d "$OUT/mediastore-publisher-test.jar"
java -jar "$OUT/mediastore-publisher-test.jar"
