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
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/DownloadSnapshot.kt" \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/DownloadRepository.kt" \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/DownloadStateMachine.kt" \
  "$ROOT/library/src/main/kotlin/app/novafetch/library/FinalMediaMetadata.kt" \
  "$ROOT/library/src/main/kotlin/app/novafetch/library/MediaItem.kt" \
  "$ROOT/library/src/main/kotlin/app/novafetch/library/MediaLibraryRepository.kt" \
  "$ROOT/library/src/main/kotlin/app/novafetch/library/SharedMediaPublisher.kt" \
  "$ROOT/library/src/main/kotlin/app/novafetch/library/BasicFileIntegrityVerifier.kt" \
  "$ROOT/library/src/main/kotlin/app/novafetch/library/MediaFinalizer.kt" \
  "$ROOT/library/src/test/kotlin/app/novafetch/library/MediaFinalizerTest.kt" \
  -include-runtime -d "$OUT/media-finalizer-test.jar"
java -jar "$OUT/media-finalizer-test.jar"
