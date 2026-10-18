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
- Gradle wrapper `:common:test :download-core:test :media-resolver:test` — FAIL. Existing `DownloadCoordinatorTest.kt` has an incomplete `Flow` test double; `common:test` also finds no JUnit-platform tests because current portable tests use `main()` entry points.
- Retried `:common:test :download-core:test :media-resolver:test` after provisioning JDK 17 — BLOCKED before unit compilation because Android SDK `platforms;android-36` and `build-tools;36.0.0` licenses have not been accepted/installed.
- `tools/verify-android-build.sh` and device runtime verification have not passed. No APK is claimed.

### Current gate

- User approval is pending to accept Google's Android SDK License Agreement. Until accepted, SDK platform/build-tools and the Android build remain blocked.
- After license setup: install API 36 and Build Tools 36.0.0; rerun `tools/verify-android-build.sh`; fix actual compile errors test-first; run portable checks again.
- Device/emulator runtime and the full Browser → Download → MediaStore → Library → offline playback flow remain unverified.
- Do not push or merge without checking gates and user authorization.
