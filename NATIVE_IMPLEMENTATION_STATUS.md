# NovaFetch Native Implementation Status

**Branch:** `feature/foundation-direct-download`
**Current phase:** Plan 01 — Foundation & Direct Download
**Status:** core/portable implementation verified; Android source adapters and instrumentation tests implemented; Android SDK/device verification still open.

## Implemented and portable-verified

- Kotlin download domain (`DownloadId`, `DownloadState`, `DownloadPlan`).
- Explicit download state machine.
- Deterministic Python media server with direct, Range, no-Range, redirect, 503, mid-transfer disconnect, expiring URL, ETag-change, and playable MP4 fixtures.
- Streaming direct HTTP transfer engine.
- Byte-range resume and safe restart when Range is unavailable.
- ETag/Last-Modified source-change protection.
- Persistent resume metadata (`totalBytes`, `downloadedBytes`, `tempPath`, `ETag`, `Last-Modified`, `supportsResume`, `outputUri`).
- Download coordinator: enqueue, pause, resume, cancel state, network waiting, retry policy, and interrupted-job recovery.
- Recovery routes are separated correctly: fresh user actions can select UIDT; automatic recovery uses WorkManager.
- Room entities/DAO/database/repository source plus portable entity/repository mapping tests.
- `DirectDownloadRunner` linking persisted checkpoints to the transfer engine.
- Integrity verification before finalization.
- Crash-safe finalization.
- MediaStore recovery now persists and reuses the **exact content URI per download job**; it does not recover by filename, so two jobs with the same display name cannot be conflated.
- Persistent local Library entity/DAO/repository.
- Playable MP4 end-to-end test: HTTP download → verify → finalize → Library → `COMPLETED`; `ffprobe` confirms video + audio streams.

## Android source now implemented

- Android 14+ UIDT `JobScheduler` adapter with user-initiated flag, network requirement, estimate support, and JobService notification.
- WorkManager recovery scheduler/worker for non-user-initiated and pre-Android-14 flows.
- Android scheduler policy + portable unit tests.
- Android Room database factory and observable download list repository.
- Real Room instrumentation test source using an in-memory Android Room database.
- `AndroidSharedMediaStore` using `IS_PENDING`, `RELATIVE_PATH`, and exact-URI recovery.
- Real MediaStore instrumentation test source.
- WorkManager instrumentation test source using `WorkManagerTestInitHelper`.
- `NovaFetchApplication` + `AppGraph` wiring Room, scheduler, transfer, finalizer, and Library.
- Runnable Compose shell with Downloads and Library destinations.
- Media3/ExoPlayer local-media player screen.
- Android manifests for launcher app, UIDT JobService, required foreground-service permissions, and test WorkManager initialization.

## Fresh portable verification commands

```bash
./tools/run-task1-smoke.sh
./tools/run-task2a-smoke.sh
./tools/run-task2b-smoke.sh
./tools/run-library-room-smoke.sh
./tools/run-task5-core-smoke.sh
./tools/run-task4-smoke.sh
./tools/run-direct-runner-smoke.sh
./tools/run-task6-scheduler-smoke.sh
./tools/run-task6-adaptive-smoke.sh
./tools/run-task7-mediastore-core-smoke.sh
./tools/run-finalizer-smoke.sh
./tools/run-task8-metadata-smoke.sh
./tools/run-task8-host-smoke.sh
./tools/run-pipeline-e2e-smoke.sh
python3 -m unittest \
  tools/test_task6_android_contract.py \
  tools/test_task7_android_mediastore_contract.py \
  tools/test_task8_android_app_contract.py \
  tools/test_android_instrumentation_contract.py -v
(cd tools/test-media-server && python3 -m unittest test_server.py -v)
```

## Android verification gate still open

This execution container has Java/Kotlin but **no usable Gradle installation or Android SDK**, and outbound DNS is blocked, so the following claims are intentionally not made yet:

- Successful AGP/Gradle project sync/build.
- Room KSP generation against the Android toolchain.
- `connectedDebugAndroidTest` on emulator/device.
- Runtime behavior of UIDT JobService on Android 14+.
- Runtime WorkManager recovery after process death/reboot.
- Real ContentResolver/MediaStore behavior on Android 10–16.
- Compose UI rendering on-device.
- Media3 playback on-device.

The project pins the intended toolchain/dependency versions in `gradle/libs.versions.toml` and `gradle/wrapper/gradle-wrapper.properties`, but the wrapper JAR/scripts are not available in this sandbox snapshot because Gradle could not be downloaded.

## Run when Android Studio/SDK is available

Compile the APK and Android test sources:

```bash
./tools/verify-android-build.sh
```

With an emulator/device connected:

```bash
./tools/verify-android-runtime.sh
```

Do **not** mark Plan 01 complete until those commands pass in an SDK-equipped environment and the direct-download Android E2E path is exercised on-device.

## Next implementation step after the gate

Once Plan 01 Android runtime verification is green, move to Plan 02:

`GeckoView browser shell → built-in WebExtension → generic DOM/direct media detection → candidate validation/deduplication → Media Sheet → handoff to the existing download engine.`
