# NovaFetch — Codex Continuation Ledger

## 2026-10-18 remote bootstrap

- Repository: official `VIGABANC/NovaFetch`, cloned from GitHub.
- Branch: `codex/android-build-foundation`, based on source commit `830684d`.
- Checkout was clean before toolchain/bootstrap changes.
- JDK 21.0.12.1 and JDK 17.0.20.1 (Temurin) were provisioned under `/tmp` and checksum-verified from Adoptium metadata.
- Gradle 9.6.0 distribution was provisioned from `services.gradle.org`; SHA-256 verified as `bbaeb2fef8710818cf0e261201dab964c572f92b942812df0c3620d62a529a01`.
- Official Kotlin compiler 2.4.20 was provisioned from its GitHub release; SHA-256 verified against the release checksum.
- Restored `gradlew`, `gradlew.bat`, and `gradle-wrapper.jar`; wrapper distribution checksum is pinned in `gradle-wrapper.properties`.
- Android command-line tools 13114758 were downloaded from `dl.google.com` and verified against the checksum in Google's repository metadata. Platform/build-tools installation is pending SDK license acceptance.

### Baseline evidence

- `node --test tools/media-detector-tests/*.test.cjs` — PASS, 11/11.
- `python3 -m unittest discover -s tools/test-media-server -p 'test*.py' -q` — PASS, 11/11.
- `bash tools/run-hls-parser-smoke.sh` with Kotlin 2.4.20 on PATH — PASS (`HLS parser portable tests PASS`).
- Initial `bash tools/verify-android-build.sh` before wrapper restoration — BLOCKED, exit 2: Gradle unavailable.
- Post-wrapper `bash tools/verify-android-build.sh` with Gradle 9.6.0/JDK 21 — BLOCKED before compilation: API 36 platform and Build Tools licenses not accepted; no Android source compile has run.
- Initial Gradle wrapper `:common:test :download-core:test :media-resolver:test` — FAIL. Existing `DownloadCoordinatorTest.kt` had an incomplete `Flow` test double; `common:test` also finds no JUnit-platform tests because current portable tests use `main()` entry points.
- Fixed the test double to use `emptyFlow()` and updated the portable coroutine stub. RED: `:download-core:compileTestKotlin` failed with `ABSTRACT_MEMBER_NOT_IMPLEMENTED`; GREEN: `:download-core:compileTestKotlin` passed and `bash tools/run-task5-core-smoke.sh` printed `DownloadCoordinatorTest: PASS`.
- Retried `:common:test :download-core:test :media-resolver:test` after provisioning JDK 17 — BLOCKED before unit compilation because Android SDK `platforms;android-36` and `build-tools;36.0.0` licenses have not been accepted/installed.
- `tools/verify-android-build.sh` and device runtime verification have not passed. No APK is claimed.

## 2026-10-18 — Android build and direct transfer hardening

- The user authorized continuing after the SDK license prompt. Android SDK licenses were accepted; `platforms;android-36`, `build-tools;36.0.0`, and platform tools are installed.
- Resolved Android source compile failures in media candidate validation, GeckoView extension nullability, foreground service imports, Room runtime exposure, and direct download filename display.
- Pinned Compose 1.11.0, Activity Compose 1.12.4, Material 3 1.3.2, WorkManager 2.10.5, and GeckoView 152.0.20260713164047 so the dependency graph meets the repository's planned compileSdk 36. Newer selected artifacts required API 37.1; compileSdk remains 36 per the repository plans.
- Added Room schema export output at `data/schemas/app.novafetch.data.NovaFetchDatabase/1.json`.
- Direct resume now validates a `206` response's Content-Range start/end/total, known total length, body length, and response validator before opening the partial file for append. A malformed range fixture proves the partial file remains unchanged.
- `bash tools/verify-android-build.sh` — PASS, `BUILD SUCCESSFUL`; debug APK generated at `app/build/outputs/apk/debug/app-debug.apk` (approximately 536 MB, includes GeckoView native libraries).
- `node --test tools/media-detector-tests/*.test.cjs` — PASS, 11/11.
- `python3 -m unittest discover -s tools/test-media-server -p 'test*.py' -q` — PASS, 11/11.
- `bash tools/run-hls-parser-smoke.sh` — PASS.
- `bash tools/run-generic-core-smoke.sh` — PASS (MediaCatalogTest, UrlPolicyTest, media detector 11/11).
- `bash tools/run-task5-core-smoke.sh` — PASS (DownloadCoordinatorTest).
- `bash tools/run-direct-runner-smoke.sh` — PASS (DirectDownloadRunnerTest and UrlConnectionTransferEngineTest, including safe resume, restart-on-ignored-Range, changed ETag, and malformed Content-Range).
- `tools/test_task8_android_app_contract.py` now asserts that downloads expose pause/resume actions and route them to the coordinator; `python3 -m unittest discover -s tools -p 'test*.py' -q` — PASS, 23 tests.
- Downloads screen now exposes Pause for queued/active/waiting/retry jobs and Resume for paused jobs. The runner checkpoints the last received bytes without overwriting a pause, and stops the canceled transfer; `run-direct-runner-smoke.sh` covers this pause race.
- After the pause/resume UI and runner changes, `bash tools/verify-android-build.sh` — PASS, `BUILD SUCCESSFUL`.
- Gradle `:common:test` still reports no tests discovered because these existing tests are portable `main()` harnesses rather than JUnit tests; this is not recorded as a passing Gradle unit-test suite.
- `adb` is unavailable in the execution environment. No emulator/device runtime test has been performed; the app is built but not installed or exercised on Android.

### Current gate

- Build and portable checks pass. The direct media vertical slice still needs Android runtime verification (including process recovery, MediaStore finalization, library listing, and offline playback) on a device/emulator.
- HLS/DASH playback/download completion, broader browser privacy requirements, and full release/readiness work remain incomplete. No release APK is signed; signing secrets are not present or committed.
- Do not push or merge without checking gates and user authorization.
