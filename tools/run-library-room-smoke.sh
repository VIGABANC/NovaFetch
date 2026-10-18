#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/smoke"
mkdir -p "$OUT"
kotlinc \
  "$ROOT/tools/kotlin-stubs/kotlinx/coroutines/flow/Flow.kt" \
  "$ROOT/tools/kotlin-stubs/androidx/room/RoomStubs.kt" \
  "$ROOT/common/src/main/kotlin/app/novafetch/common/DownloadId.kt" \
  "$ROOT/library/src/main/kotlin/app/novafetch/library/MediaItem.kt" \
  "$ROOT/library/src/main/kotlin/app/novafetch/library/MediaLibraryRepository.kt" \
  "$ROOT/data/src/main/kotlin/app/novafetch/data/MediaItemEntity.kt" \
  "$ROOT/data/src/main/kotlin/app/novafetch/data/MediaLibraryDao.kt" \
  "$ROOT/data/src/main/kotlin/app/novafetch/data/MediaItemMapping.kt" \
  "$ROOT/data/src/main/kotlin/app/novafetch/data/RoomMediaLibraryRepository.kt" \
  "$ROOT/data/src/test/kotlin/app/novafetch/data/RoomMediaLibraryRepositoryTest.kt" \
  -include-runtime -d "$OUT/room-media-library-test.jar"
java -jar "$OUT/room-media-library-test.jar"
