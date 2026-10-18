# NovaFetch 01 — Foundation & Direct Download Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce the first working NovaFetch vertical slice: direct HTTP media download with persistent state, pause/resume, process recovery, MediaStore finalization, Library listing, and offline playback.

**Architecture:** Start with the downloader and persistence core before the browser. Room is the source of truth; transfer code is isolated behind `TransferEngine`; the UI observes repositories instead of services. Final media moves atomically from app-private temporary storage to MediaStore.

**Tech Stack:** Kotlin, Gradle, Jetpack Compose, Room, OkHttp, WorkManager, MediaStore, Media3/ExoPlayer, JUnit, MockWebServer, Android instrumented tests, Python 3 test server.

**Spec:** `docs/superpowers/specs/2026-10-17-novafetch-android-design.md`

## Global Constraints

- Android-only, local-only, media-first.
- `minSdk = 29`, `compileSdk = 36`, `targetSdk = 36`.
- No `MANAGE_EXTERNAL_STORAGE`.
- Temporary state is app-private; successful final media goes through MediaStore.
- Room is the persistent source of truth for in-progress downloads.
- Default simultaneous downloads = `2`; selectable range later is `1..4`.
- Protected/DRM media is outside this plan and must never be bypassed.
- No cookies, Authorization headers, tokens, or signed URLs in logs.
- A completed job is only `COMPLETED` after integrity checks and successful MediaStore finalization.

## Review Focus

- Server changes ETag during resume → job must refuse unsafe append and transition to a re-resolve/failure path.
- Disk space becomes insufficient after partial download → keep recoverable partial state and surface `WAITING_STORAGE`.
- Process dies between final file creation and Room update → recovery must reconcile without duplicate MediaStore rows.
- Server ignores Range and returns `200` to a resume request → restart safely rather than append duplicate bytes.
- User-paused job survives app restart → it must remain `PAUSED`, not auto-resume.

## File Structure

```text
settings.gradle.kts
build.gradle.kts
gradle/libs.versions.toml
app/
common/
data/
download-core/
download-android/
library/
player/
security/
tools/test-media-server/
```

### Task 1: Project Skeleton and Domain Contracts

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `common/build.gradle.kts`
- Create: `common/src/main/kotlin/app/novafetch/common/DownloadId.kt`
- Create: `common/src/main/kotlin/app/novafetch/common/DownloadState.kt`
- Create: `download-core/src/main/kotlin/app/novafetch/download/DownloadPlan.kt`
- Test: `common/src/test/kotlin/app/novafetch/common/DownloadStateTest.kt`

**Interfaces:**
- Produces: `@JvmInline value class DownloadId(val value: String)`
- Produces: `enum class DownloadState { CREATED, RESOLVING, QUEUED, CONNECTING, DOWNLOADING, VERIFYING, PROCESSING, FINALIZING, COMPLETED, PAUSED, WAITING_NETWORK, WAITING_STORAGE, RETRY_PENDING, FAILED, CANCELLED }`
- Produces: `sealed interface DownloadPlan` with `DirectPlan(sourceUrl: String, expectedMime: String?, suggestedFileName: String)`

- [ ] **Step 1: Write the failing state-transition test**
  - Assert that terminal states are `COMPLETED`, `FAILED`, and `CANCELLED`.
  - Assert that `PAUSED` is not terminal.

- [ ] **Step 2: Run the test**
  - Run: `./gradlew :common:test`
  - Expected: FAIL because domain types do not exist.

- [ ] **Step 3: Add the multi-module skeleton and domain types**
  - Include modules `:app`, `:common`, `:data`, `:download-core`, `:download-android`, `:library`, `:player`, `:security`.
  - Configure Android app with `minSdk = 29`, `compileSdk = 36`, and `targetSdk = 36`.

- [ ] **Step 4: Run the test**
  - Run: `./gradlew :common:test`
  - Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add .
git commit -m "chore: scaffold novafetch modules and download domain"
```

### Task 2: Room Download Persistence and State Machine

**Files:**
- Create: `data/src/main/kotlin/app/novafetch/data/DownloadJobEntity.kt`
- Create: `data/src/main/kotlin/app/novafetch/data/DownloadPartEntity.kt`
- Create: `data/src/main/kotlin/app/novafetch/data/DownloadDao.kt`
- Create: `data/src/main/kotlin/app/novafetch/data/NovaFetchDatabase.kt`
- Create: `download-core/src/main/kotlin/app/novafetch/download/DownloadStateMachine.kt`
- Test: `download-core/src/test/kotlin/app/novafetch/download/DownloadStateMachineTest.kt`
- Test: `data/src/androidTest/kotlin/app/novafetch/data/DownloadDaoTest.kt`

**Interfaces:**
- Consumes: `DownloadId`, `DownloadState`.
- Produces: `fun DownloadStateMachine.canTransition(from: DownloadState, to: DownloadState): Boolean`
- Produces DAO functions:
  - `suspend fun upsertJob(job: DownloadJobEntity)`
  - `fun observeJob(id: String): Flow<DownloadJobEntity?>`
  - `suspend fun unfinishedJobs(): List<DownloadJobEntity>`
  - `suspend fun updateProgress(id: String, downloadedBytes: Long, state: DownloadState)`

- [ ] **Step 1: Write failing transition tests**
  - `DOWNLOADING -> PAUSED` is allowed.
  - `PAUSED -> CONNECTING` is allowed.
  - `COMPLETED -> DOWNLOADING` is rejected.
  - `FAILED -> DOWNLOADING` is rejected unless a new job/retry transition is explicitly created.

- [ ] **Step 2: Run tests**
  - Run: `./gradlew :download-core:test :data:connectedAndroidTest`
  - Expected: FAIL.

- [ ] **Step 3: Implement entities, DAO, database, and state machine**
  - Persist source metadata, byte counters, ETag, Last-Modified, temp path, output URI, retry fields, timestamps, adapter metadata.
  - Persist parts/segments separately.

- [ ] **Step 4: Run tests**
  - Run: `./gradlew :download-core:test :data:connectedAndroidTest`
  - Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add data download-core
git commit -m "feat: persist downloads and enforce state transitions"
```

### Task 3: Deterministic Test Media Server

**Files:**
- Create: `tools/test-media-server/server.py`
- Create: `tools/test-media-server/README.md`
- Create: `tools/test-media-server/fixtures/direct.mp4`
- Test: `tools/test-media-server/test_server.py`

**Interfaces:**
- Produces HTTP endpoints:
  - `/direct.mp4`
  - `/range.mp4`
  - `/no-range.mp4`
  - `/redirect.mp4`
  - `/error-503`
  - `/disconnect-mid-download`
  - `/expired-url`

- [ ] **Step 1: Write failing server tests**
  - Assert `/range.mp4` returns `206` with a valid `Content-Range` for `Range: bytes=0-9`.
  - Assert `/no-range.mp4` returns full `200`.
  - Assert `/redirect.mp4` returns a redirect.
  - Assert `/error-503` returns `503`.

- [ ] **Step 2: Run tests**
  - Run: `python3 -m unittest tools/test-media-server/test_server.py -v`
  - Expected: FAIL.

- [ ] **Step 3: Implement the standard-library HTTP server**
  - Keep behavior deterministic and fixture-backed.

- [ ] **Step 4: Run tests**
  - Run: `python3 -m unittest tools/test-media-server/test_server.py -v`
  - Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add tools/test-media-server
git commit -m "test: add deterministic media transfer server"
```

### Task 4: Direct HTTP Transfer Engine with Safe Resume

**Files:**
- Create: `download-core/src/main/kotlin/app/novafetch/download/TransferEngine.kt`
- Create: `download-core/src/main/kotlin/app/novafetch/download/TransferRequest.kt`
- Create: `download-core/src/main/kotlin/app/novafetch/download/TransferResult.kt`
- Create: `download-android/src/main/kotlin/app/novafetch/download/OkHttpTransferEngine.kt`
- Create: `download-android/src/main/kotlin/app/novafetch/download/RangeProbe.kt`
- Test: `download-android/src/test/kotlin/app/novafetch/download/OkHttpTransferEngineTest.kt`

**Interfaces:**
- Produces: `suspend fun TransferEngine.transfer(request: TransferRequest, onProgress: suspend (Long) -> Unit): TransferResult`
- Produces: `suspend fun RangeProbe.probe(url: String, headers: Map<String,String>): RangeCapability`
- `TransferRequest` includes `url`, `destination`, `resumeOffset`, `etag`, `lastModified`, `headers`.

- [ ] **Step 1: Write failing direct-download and resume tests**
  - Download `/direct.mp4` to temp file and assert byte equality.
  - Interrupt `/range.mp4`, resume from saved offset, assert final byte equality.
  - Resume against `/no-range.mp4`; assert implementation restarts safely rather than appending.
  - Change ETag between partial and resume; assert unsafe append is rejected.

- [ ] **Step 2: Run test**
  - Run: `./gradlew :download-android:test`
  - Expected: FAIL.

- [ ] **Step 3: Implement `OkHttpTransferEngine` and `RangeProbe`**
  - Stream bytes to disk; never buffer whole media in memory.
  - Redact sensitive request data in thrown/logged diagnostics.

- [ ] **Step 4: Run test**
  - Run: `./gradlew :download-android:test`
  - Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add download-core download-android
git commit -m "feat: add resumable direct http transfer engine"
```

### Task 5: DownloadCoordinator, Pause/Resume, and Recovery

**Files:**
- Create: `download-core/src/main/kotlin/app/novafetch/download/DownloadCoordinator.kt`
- Create: `download-core/src/main/kotlin/app/novafetch/download/DownloadRepository.kt`
- Create: `download-android/src/main/kotlin/app/novafetch/download/DownloadScheduler.kt`
- Create: `download-android/src/main/kotlin/app/novafetch/download/RecoveryWorker.kt`
- Test: `download-core/src/test/kotlin/app/novafetch/download/DownloadCoordinatorTest.kt`
- Test: `download-android/src/androidTest/kotlin/app/novafetch/download/RecoveryWorkerTest.kt`

**Interfaces:**
- Produces:
  - `suspend fun DownloadCoordinator.enqueue(plan: DownloadPlan): DownloadId`
  - `suspend fun DownloadCoordinator.pause(id: DownloadId)`
  - `suspend fun DownloadCoordinator.resume(id: DownloadId)`
  - `suspend fun DownloadCoordinator.cancel(id: DownloadId, deletePartial: Boolean)`
  - `fun DownloadRepository.observe(id: DownloadId): Flow<DownloadSnapshot>`

- [ ] **Step 1: Write failing coordinator tests**
  - User pause persists `PAUSED`.
  - Recovery restarts interrupted `DOWNLOADING` jobs but leaves `PAUSED` jobs untouched.
  - Network loss maps to `WAITING_NETWORK`, then returns to `CONNECTING`.
  - Retryable `503` uses bounded backoff; non-retryable `404` fails.

- [ ] **Step 2: Run tests**
  - Run: `./gradlew :download-core:test :download-android:connectedAndroidTest`
  - Expected: FAIL.

- [ ] **Step 3: Implement coordinator, scheduler abstraction, and recovery worker**
  - Keep Android-specific scheduling behind `DownloadScheduler`.
  - Add UIDT implementation later in the Android scheduling task if toolchain/API supports it cleanly; keep WorkManager recovery path mandatory.

- [ ] **Step 4: Run tests**
  - Run: `./gradlew :download-core:test :download-android:connectedAndroidTest`
  - Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add download-core download-android
git commit -m "feat: coordinate downloads and recover interrupted jobs"
```


### Task 6: Android Transfer Scheduling — UIDT on API 34+

**Files:**
- Create: `download-android/src/main/kotlin/app/novafetch/download/UidtDownloadScheduler.kt`
- Create: `download-android/src/main/kotlin/app/novafetch/download/WorkManagerDownloadScheduler.kt`
- Create: `download-android/src/main/kotlin/app/novafetch/download/DownloadJobService.kt`
- Modify: `download-android/src/main/AndroidManifest.xml`
- Test: `download-android/src/androidTest/kotlin/app/novafetch/download/DownloadSchedulerTest.kt`

**Interfaces:**
- Produces: `interface DownloadScheduler { suspend fun schedule(id: DownloadId, estimatedBytes: Long?): ScheduleResult }`
- API 34+ long user-started transfers use UIDT through `JobScheduler` with `setUserInitiated(true)`.
- API 29–33 use the WorkManager/foreground-compatible path.
- Manifest declares `android.permission.RUN_USER_INITIATED_JOBS` and a non-exported `JobService` protected by `android.permission.BIND_JOB_SERVICE`.

- [ ] **Step 1: Write failing scheduler selection tests**
  - On API 34+, a long user-started transfer selects `UidtDownloadScheduler`.
  - On API 33 and lower, it selects `WorkManagerDownloadScheduler`.
  - User-paused jobs are never auto-scheduled.

- [ ] **Step 2: Run tests**
  - Run: `./gradlew :download-android:connectedAndroidTest`
  - Expected: FAIL.

- [ ] **Step 3: Implement UIDT and fallback scheduling**
  - Supply the estimated network payload when known.
  - Ensure the UIDT job presents the required notification and is scheduled only from a user-initiated path.

- [ ] **Step 4: Run tests**
  - Run: `./gradlew :download-android:connectedAndroidTest`
  - Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add download-android
git commit -m "feat: schedule long user downloads with uidt"
```

### Task 7: MediaStore Finalization and Minimal Library

**Files:**
- Create: `library/src/main/kotlin/app/novafetch/library/MediaFinalizer.kt`
- Create: `library/src/main/kotlin/app/novafetch/library/MediaItemEntity.kt`
- Create: `library/src/main/kotlin/app/novafetch/library/MediaLibraryDao.kt`
- Create: `library/src/main/kotlin/app/novafetch/library/MediaLibraryRepository.kt`
- Test: `library/src/androidTest/kotlin/app/novafetch/library/MediaFinalizerTest.kt`

**Interfaces:**
- Produces: `suspend fun MediaFinalizer.finalize(jobId: DownloadId, tempFile: File, metadata: FinalMediaMetadata): Uri`
- Produces: `fun MediaLibraryRepository.observeAll(): Flow<List<MediaItem>>`

- [ ] **Step 1: Write failing finalization tests**
  - Successful finalization creates exactly one MediaStore item and marks job `COMPLETED`.
  - Simulated failure before commit leaves job non-complete and no visible partial final media.
  - Recovery after a crash between MediaStore write and Room update reconciles without creating a duplicate.

- [ ] **Step 2: Run tests**
  - Run: `./gradlew :library:connectedAndroidTest`
  - Expected: FAIL.

- [ ] **Step 3: Implement atomic finalization and Library persistence**
  - Use `IS_PENDING` MediaStore flow where supported.

- [ ] **Step 4: Run tests**
  - Run: `./gradlew :library:connectedAndroidTest`
  - Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add library data
git commit -m "feat: finalize downloads into mediastore library"
```

### Task 8: Minimal Compose Downloads/Library UI and Offline Player

**Files:**
- Create: `app/src/main/kotlin/app/novafetch/MainActivity.kt`
- Create: `app/src/main/kotlin/app/novafetch/DownloadsScreen.kt`
- Create: `app/src/main/kotlin/app/novafetch/LibraryScreen.kt`
- Create: `player/src/main/kotlin/app/novafetch/player/PlayerScreen.kt`
- Create: `player/src/main/kotlin/app/novafetch/player/PlayerFactory.kt`
- Test: `app/src/androidTest/kotlin/app/novafetch/DirectDownloadE2eTest.kt`

**Interfaces:**
- Consumes: `DownloadCoordinator`, `DownloadRepository`, `MediaLibraryRepository`.
- Produces minimal navigation: Downloads → Library → Player.

- [ ] **Step 1: Write failing E2E test**
  - Enqueue `/range.mp4`.
  - Pause after progress > 0.
  - Resume.
  - Wait for completion.
  - Assert one Library item.
  - Open player and assert Media3 reaches `STATE_READY`.

- [ ] **Step 2: Run test**
  - Run: `./gradlew :app:connectedAndroidTest`
  - Expected: FAIL.

- [ ] **Step 3: Implement minimal Compose UI and player**
  - No browser yet.
  - UI may accept a test URL entry field solely for this vertical slice.

- [ ] **Step 4: Run full plan verification**
  - Run: `./gradlew test connectedAndroidTest`
  - Run: `python3 -m unittest tools/test-media-server/test_server.py -v`
  - Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add app player
git commit -m "feat: complete direct download to offline playback slice"
```

## Plan Exit Criterion

Demonstrate: direct MP4 → download → pause → resume → app/process recovery → MediaStore → Library → offline playback.
