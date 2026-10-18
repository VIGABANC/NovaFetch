# NovaFetch Native Implementation Status

**Branch:** `feature/foundation-direct-download`  
**Plan:** `docs/superpowers/plans/2026-10-17-novafetch-01-foundation-direct-download.md`

## Implemented and locally verified

- Kotlin download domain (`DownloadId`, `DownloadState`, `DownloadPlan`).
- Explicit download state machine.
- Deterministic Python media server with direct, Range, no-Range, redirect, 503, disconnect, expiring URL, ETag-change, and playable MP4 fixtures.
- Streaming direct HTTP transfer engine.
- Byte-range resume with safe no-Range restart.
- ETag/Last-Modified source-change protection.
- Persistent resume metadata model.
- Download coordinator core: enqueue, pause, resume, cancel state, network waiting, retry policy, and interrupted-job recovery semantics.
- Room download entities/DAO/database/repository source and portable mapping tests.
- DirectDownloadRunner linking persistent checkpoints to the transfer engine.
- Integrity checking before completion.
- Idempotent finalization orchestration, including crash recovery from `FINALIZING`.
- Persistent local media library entity/DAO/repository source.
- Playable MP4 end-to-end test: download -> verify -> finalize -> library -> `COMPLETED`; `ffprobe` confirms H.264 video and AAC audio.

## Fresh portable verification commands

```bash
./tools/run-task1-smoke.sh
./tools/run-task2a-smoke.sh
./tools/run-task2b-smoke.sh
./tools/run-library-room-smoke.sh
./tools/run-task5-core-smoke.sh
./tools/run-task4-smoke.sh
./tools/run-direct-runner-smoke.sh
./tools/run-finalizer-smoke.sh
./tools/run-pipeline-e2e-smoke.sh
(cd tools/test-media-server && python3 -m unittest test_server.py -v)
```

These suites are designed to run without an Android SDK where possible.

## Android-runtime work still pending

The current execution container does not include Gradle or the Android SDK, so these pieces are not claimed as Android-verified yet:

- Full Gradle/AGP build and Room KSP generation.
- Real Room SQLite instrumentation tests.
- WorkManager recovery adapter.
- Android 14+ User-Initiated Data Transfer scheduling adapter.
- Actual MediaStore publisher/ContentResolver integration.
- Jetpack Compose Downloads/Library screens.
- Media3/ExoPlayer offline playback on Android.
- Android process-death/reboot instrumentation tests.

Do not mark Plan 01 complete until those Android-specific gates pass in an SDK-equipped environment.

## Environment expected for Android verification

- JDK 17 or newer.
- Android SDK with API 36 platform/build tools.
- Gradle/Android Studio capable of using the versions pinned in `gradle/libs.versions.toml` and `gradle/wrapper/gradle-wrapper.properties`.

## Resume point

Start with the Android adapters for the already-tested interfaces rather than rewriting the core:

1. Implement/test WorkManager + UIDT `DownloadScheduler` adapters.
2. Implement/test Android MediaStore `SharedMediaPublisher`.
3. Run real Room instrumentation/KSP build.
4. Add Compose Downloads/Library UI.
5. Add Media3 player and complete the Android E2E gate.
