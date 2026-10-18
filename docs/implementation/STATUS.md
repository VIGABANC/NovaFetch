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
- Full portable smoke sweep `for script in tools/run-*.sh; do bash "$script"; done` — PASS, all 18 harnesses. This includes the direct pipeline E2E, finalizer, MediaStore publisher, Room library repository, browser tab registry, validated extension bridge, HLS parser, and API-specific schedulers.
- Gradle `:common:test` still reports no tests discovered because these existing tests are portable `main()` harnesses rather than JUnit tests; this is not recorded as a passing Gradle unit-test suite.
- `adb devices -l` runs but reports no connected target; the Android Emulator package is not installed and `/dev/kvm` is unavailable. No device/emulator runtime test has been performed; the app is built but not installed or exercised on Android.

### Current gate

- Build and portable checks pass. The direct media vertical slice still needs Android runtime verification (including process recovery, MediaStore finalization, library listing, and offline playback) on a device/emulator.
- HLS/DASH playback/download completion, broader browser privacy requirements, and full release/readiness work remain incomplete. No release APK is signed; signing secrets are not present or committed.
- Do not push or merge without checking gates and user authorization.

## 2026-10-18 — CI emulator path, ABI outputs, and direct-media failures

- Published branch `codex/android-build-foundation` to GitHub and opened draft PR #1 against `main`: `https://github.com/VIGABANC/NovaFetch/pull/1`. It remains unmerged.
- Added `.github/workflows/android-ci.yml`: builds with JDK 21 and API 36, runs Node/Python tests, uploads ABI debug APKs, then uses `reactivecircus/android-emulator-runner` to execute `tools/verify-android-runtime.sh` on an API 36 x86_64 Google APIs emulator. GitHub-hosted Ubuntu runners provide the emulator virtualization path; the local workspace still has no `/dev/kvm`.
- ABI splits retain `armeabi-v7a`, `arm64-v8a`, `x86_64`, and the universal APK. A fresh `:app:assembleDebug` produced APKs of about 184 MB (ARMv7), 220 MB (arm64), 239 MB (x86_64), and 536 MB (universal). APK listing attributes most size to GeckoView `libxul.so` (117–165 MB per ABI); the universal package combines the three libraries. No duplicate ABI libraries were found within one split.
- Added `tools/test_apk_abi_splits.py`; it fails without split outputs and passes after assembling the configured APKs.
- Expanded the Android browser instrumentation test to use Compose UI controls, detect the local fixture MP4, start a download, pause/resume during a deliberately slow response, compare MediaStore bytes with the fixture, find it in Library, open the player, and verify ExoPlayer playback from the local MediaStore URI.
- Added a Room instrumentation recovery test that closes and reopens the on-disk database, then verifies an interrupted job's checkpoint and ETag survive and the coordinator reschedules recovery. This is a simulated process restart, not an OS process-kill test.
- The transfer engine now rejects a cleanly truncated HTTP body when it does not match Content-Length. The deterministic server drops one transfer after sending a partial body; the direct-transfer smoke test resumes it with Range and verifies exact bytes.
- Direct transfer failures now persist `RETRY_PENDING` and a retry count, fail after the configured retry bound, and WorkManager maps recoverable I/O errors to `Result.retry()`.
- Media finalization now returns the completed snapshot on repeated calls. `MediaFinalizerTest` verifies the second call creates no additional media row or publish operation.
- The API 36 local Android build and Android instrumentation compilation pass after these changes; `:app:assembleDebugAndroidTest` packages the fixture asset. The hosted emulator workflow has not yet run, so no Android runtime test is marked PASS.
- Current test evidence: `python3 -m unittest discover -s tools -p 'test*.py' -q` — PASS, 27 tests; Node media detector — PASS, 11/11; direct-transfer and finalizer smoke harnesses — PASS; ABI output test — PASS.
- First hosted workflow attempt (run 39172111933) — FAIL during Android SDK setup, before build or runtime tests. `setup-android` attempted to install the retired `tools` SDK package; workflow now requests only `platform-tools` before installing API 36 explicitly. No Android runtime result is available from this run.

### Current gate

- Wait for GitHub Actions to execute the API 36 emulator job. Only a passing run can verify the on-device browser → download → pause/resume → MediaStore → Library → playback sequence.
- The current CI matrix is limited to the deterministic local HTML5 fixture. YouTube, Pornhub, Vimeo, Dailymotion, Reddit, TikTok, Internet Archive, and generic-site compatibility need separate permitted-content coverage.
- HLS/DASH downloading and processing, real OS-level process-kill recovery validation, broader site privacy controls, and release readiness remain incomplete. No release APK is signed; signing secrets are not present or committed.
