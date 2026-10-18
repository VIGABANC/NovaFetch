#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/smoke"
mkdir -p "$OUT"
python3 "$ROOT/tools/test-media-server/server.py" > "$OUT/test-media-server-pipeline.log" 2>&1 &
pid=$!
trap 'kill "$pid" 2>/dev/null || true' EXIT
python3 - <<'PY'
import socket, time
for _ in range(80):
    try:
        with socket.create_connection(("127.0.0.1", 8765), timeout=.1):
            break
    except OSError:
        time.sleep(.05)
else:
    raise SystemExit("test media server did not start")
PY
kotlinc \
  "$ROOT/tools/kotlin-stubs/kotlinx/coroutines/flow/Flow.kt" \
  "$ROOT/common/src/main/kotlin/app/novafetch/common/DownloadId.kt" \
  "$ROOT/common/src/main/kotlin/app/novafetch/common/DownloadState.kt" \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/DownloadPlan.kt" \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/DownloadSnapshot.kt" \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/DownloadRepository.kt" \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/DownloadStateMachine.kt" \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/DownloadTempPathProvider.kt" \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/DirectDownloadRunner.kt" \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/TransferRequest.kt" \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/TransferResult.kt" \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/TransferEngine.kt" \
  "$ROOT/download-core/src/main/kotlin/app/novafetch/download/RangeProbe.kt" \
  "$ROOT/download-android/src/main/kotlin/app/novafetch/download/UrlConnectionRangeProbe.kt" \
  "$ROOT/download-android/src/main/kotlin/app/novafetch/download/UrlConnectionTransferEngine.kt" \
  "$ROOT/library/src/main/kotlin/app/novafetch/library/FinalMediaMetadata.kt" \
  "$ROOT/library/src/main/kotlin/app/novafetch/library/SharedMediaPublisher.kt" \
  "$ROOT/library/src/main/kotlin/app/novafetch/library/BasicFileIntegrityVerifier.kt" \
  "$ROOT/library/src/main/kotlin/app/novafetch/library/MediaItem.kt" \
  "$ROOT/library/src/main/kotlin/app/novafetch/library/MediaLibraryRepository.kt" \
  "$ROOT/library/src/main/kotlin/app/novafetch/library/MediaFinalizer.kt" \
  "$ROOT/app/src/test/kotlin/app/novafetch/DirectDownloadPipelineE2eTest.kt" \
  -include-runtime -d "$OUT/pipeline-e2e.jar"
TEST_MEDIA_BASE_URL=http://127.0.0.1:8765 java -jar "$OUT/pipeline-e2e.jar"
