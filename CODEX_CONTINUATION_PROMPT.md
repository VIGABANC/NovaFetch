# MASTER CODEX CONTINUATION PROMPT — NOVAFETCH ANDROID

> Copy this entire document into a Codex task **after giving Codex the actual NovaFetch source repository or the Git bundle**. This is an engineering execution brief, not a request to start a new project.

## 1. Role, mission and working method

Act as the **lead Android/Kotlin engineer, GeckoView integration specialist, media-pipeline engineer, reliability/security reviewer, and test automation engineer**. Use the Superpowers methodology where available: inspect → plan the next small vertical slice → test RED → implement GREEN → refactor → verify → commit → continue. Work directly on the existing **NovaFetch** codebase. Do not rewrite the project from zero, replace the approved architecture with a different app, or claim that source files constitute a working APK.

**Primary objective:** deliver a **working, installable Android media-first browser** with reliable direct media detection, user-authorized downloads, offline library and playback. Restore Android build/runtime verification first, then expand HLS VOD/DASH VOD support and app reliability. Prefer functionality and correctness over UI polish.

**User preference:** native execution; progress task-by-task without repeated approval requests for reversible code changes. Maintain a persistent progress ledger, concise status summaries, small coherent commits, and an evidence-based final handoff. Never push or merge across a privacy-sensitive boundary without authorization.

## 2. GitHub repository / source initialization — critical

- Intended remote: `https://github.com/VIGABANC/NovaFetch`.
- The GitHub repository was **PUBLIC and EMPTY at last verified check on 2026-10-18**. Re-check before acting; do not assume it already contains project files.
- The original request specified **PRIVATE** visibility. **Do not publish NovaFetch source to a PUBLIC repository without an unambiguous authorization to do so.** If still public, work locally/on an isolated branch and produce a ready-to-push commit/bundle; request the one necessary privacy decision before pushing.
- The actual project is present in the supplied NovaFetch ZIP or Git bundle, not in the empty remote. **If the source has not been attached/imported, STOP and report the missing source; do not invent an equivalent application.**
- Historical local repository `main` and `feature/generic-media-detection` pointed to commit `f88b7b0` at the last handoff. This continuation prompt is a new documentation commit on a separate `docs/codex-continuation` branch; inspect the actual Git history rather than assuming a specific HEAD.
- Keep history. Prefer a feature branch such as `codex/android-runtime-hls`; never force-push, delete or rewrite history. Do not overwrite an existing remote branch. Open a pull request when a remote, privacy and CI gate are ready.
- To import source from a bundle on a machine with Git:

```bash
git clone /path/to/novafetch-codex-handoff.bundle NovaFetch
cd NovaFetch
git switch main
# Check repo state, then create a feature branch:
git switch -c codex/android-runtime-hls
```

If starting from the ZIP, extract it into a normal working directory; the ZIP may not include `.git`, so use the Git bundle for history whenever possible.

## 3. Read these exact project documents FIRST

1. `README.md`
2. `AGENTS.md` (if present)
3. `NATIVE_IMPLEMENTATION_STATUS.md`
4. `PLAN02_SOURCE_STATUS.md`
5. `PLAN03_PROGRESS.md`
6. `docs/superpowers/specs/2026-10-17-novafetch-android-design.md`
7. `docs/superpowers/plans/2026-10-17-novafetch-implementation-index.md`
8. `docs/superpowers/plans/2026-10-17-novafetch-01-foundation-direct-download.md`
9. `docs/superpowers/plans/2026-10-17-novafetch-02-browser-generic-detection.md`

**Important repository gap:** the implementation index describes Plans 03–06, but those plan documents were NOT all present in the last examined source tree. Do not pretend they exist. Write missing incremental plans only when needed, drawing from the approved spec and code state, and save them under `docs/superpowers/plans/`.

Read the actual source tree and Gradle catalog before naming APIs or dependencies. Treat status documents as claims to verify, not as proof of Android build success.

## 4. Approved product constraints — non-negotiable

- **Platform:** Android only, `minSdk 29` (Android 10). Test support across Android 10–16 as practical.
- **UX:** simple, light/minimal, blue accents, bottom destinations **Home, Browser, Downloads, Library**.
- **Core:** privacy-minded GeckoView browser + media detection + download choice/quality + reliable local transfer/processing + Library + offline Media3 playback.
- **Local-first:** no account, remote backend, compulsory telemetry or cloud sync required for basic functionality.
- **Delivery:** direct APK/website distribution for v1; no assumption of Play Store approval.
- **Media:** direct HTTP MP4/MP3/WebM and other permitted file types; HLS VOD `.m3u8`; DASH VOD `.mpd`; media hints from HTML5 DOM/resource information where supported; optional compiled-in site adapters for legally accessible content.
- **Downloads:** queue, pause/resume, retries, process/network interruption recovery, safe Range checkpointing, source-identity verification on refreshed links, quality selection, optional separate audio/video mux, user-visible progress.
- **Storage:** Room source of truth for in-flight jobs, app-private temp/parts, MediaStore finalization, no `MANAGE_EXTERNAL_STORAGE` permission.
- **Safety/compliance:** absolutely no DRM/Widevine circumvention, key/license extraction, authentication bypass, paywall bypass, interception of other apps, login-token theft, illegal copying or distribution, proprietary code/assets, or remote executable site adapters. Do not describe every signed URL as DRM, but do not bypass access restrictions.
- **Privacy/security:** bounded parsing; validate all untrusted WebExtension messages, origins, redirects, MIME/filenames and URLs; do not expose full cookies or write credentials/full signed media URLs to logs. HTTPS first, allow local cleartext only in **debug test configuration** as needed.
- **No false guarantees:** Android force-stop can prevent background execution until the user launches the app again; document and test real OS behaviors.

## 5. What is already implemented — preserve it

Examine before changing:

- `common`, `download-core`, `download-android`: domain states, coordinator, resumable HTTP transfer, partial checkpoint metadata, retry/recovery policies, Android scheduler source.
- `data`, `library`: Room entities/DAO/repository, integrity/finalization flow, exact persisted MediaStore content URI recovery for each download job, local media catalog.
- `app`, `player`: initial Android application graph, Compose shell, Media3/ExoPlayer local playback code.
- `browser-core`, `browser-ui`, `media-detection`, `media-resolver`, `security`: GeckoView browser prototype, built-in WebExtension and DOM detector, native message validation, candidate dedup/URL policy, basic media sheet, early HLS playlist parser.
- `tools/test-media-server`: deterministic HTTP fixtures for Range, no-Range, redirects, errors, disconnect, expiration, playable MP4, browser media fixtures.
- `tools/verify-android-build.sh`, `tools/verify-android-runtime.sh`: intended build/device verification entrypoints.

**Known limits:** The previous environment had no usable Android SDK/Gradle; there is **no verified compiled/installable APK** from these snapshots. GeckoView/Room/WorkManager/UIDT/MediaStore/Media3 Android behavior has not been proven on a device. HLS parser is preliminary only; **HLS segment transfer, DASH, and mux/remux are not implemented**. Browser integration is a source prototype and must be verified end-to-end on Android.

Portable checks freshly verified on 2026-10-18 for the source checkpoint: `node --test tools/media-detector-tests/*.test.cjs` **11/11 pass**; `python3 -m unittest discover -s tools/test-media-server -p 'test*.py' -q` **11/11 pass**; `bash tools/run-hls-parser-smoke.sh` **pass**. Re-run them yourself. **These are not substitutes for Gradle/Android tests.**

## 6. Execution order — perform this without starting over

### PHASE 0 — Inventory, isolate and establish real build evidence

1. Confirm repository state (`git status`, HEAD, branch, remotes, private/public visibility if pushing). Create/continue a non-main work branch and progress ledger `docs/implementation/STATUS.md`. Record exactly which tests are runnable in this environment.
2. Inventory all Gradle modules and dependencies (`settings.gradle.kts`, `gradle/libs.versions.toml`, module `build.gradle.kts`, manifests). Check dependencies against official current compatibility rules (AGP, Gradle, Kotlin compiler built into AGP 9+, KSP, Compose compiler, Room, Media3 and GeckoView). Some previously pinned versions may be inconsistent or unavailable; **verify and fix, do not blindly trust version strings**.
3. Restore a **working Gradle wrapper**, including scripts and wrapper JAR, with a documented/checksummed distribution. Never download arbitrary binary archives from unverified mirrors. Ensure JDK and SDK/toolchain compatibility.
4. Run `./tools/verify-android-build.sh`. Fix the FIRST real failure based on diagnostic evidence, adding a regression test where feasible. Repeat until `:app:assembleDebug` and Android test source compilation pass. Do not suppress Kotlin/compiler errors with broad exclusions.
5. If a device/emulator is available, run `./tools/verify-android-runtime.sh` and targeted `connectedDebugAndroidTest`, verifying Room/MediaStore/WorkManager/GeckoView behavior. If unavailable, explicitly mark runtime gate BLOCKED and keep working on portable components with tests; do not assert device success.
6. Add a minimal reproducible Android CI workflow if authorized and secrets-free: Gradle setup, cache, JVM tests, Android build, artifact archive; device tests run on an emulator-capable runner or documented manual gate. Ensure GitHub Actions permissions are least-privilege.

**Exit gate:** real `assembleDebug` success and Android test-source compilation. Device gate remains open until verified on an emulator/device.

### PHASE 1 — Complete direct download Android end-to-end (Plans 01/02 gates)

1. Browser opens a deterministic fixture page in GeckoView (`http://10.0.2.2:8765/media-page.html` for emulator/debug).
2. Built-in WebExtension reports HTML5 media DOM hints through verified native messaging, with dynamic insertion and multiple tabs. Verify session/frame/extension sender identity and content-script manifest compatibility with the GeckoView version actually installed.
3. `MediaCatalog` validates and deduplicates candidates; candidate list/quality UI clearly distinguishes direct-downloadable media, manifests needing processing, `blob:` hints, and protected/unavailable media. Avoid showing metadata-only hints as fully downloadable.
4. Selected direct file enters existing `DownloadCoordinator` and persists in Room. User can pause/resume; verify `ETag`/`Last-Modified`, incorrect Range response, redirect and source change, transient 503, network loss, disk pressure and process recreation.
5. Correctly separate **user-initiated UIDT** (Android 14+ where appropriate) from recovery WorkManager; obey foreground service and notification restrictions.
6. Finalize through `IS_PENDING` MediaStore and exact persisted content URI; avoid duplicate files on crash recovery; record Library item and play using Media3 offline.
7. Add/repair a deterministic Android E2E test. Verify state transitions; a job becomes `COMPLETED` only after validated finalization.

**Exit gate:** on-device `Browse → Detect → Select direct media → Download → Pause → Resume → MediaStore → Library → Offline playback`, including at least one interruption/recovery case.

### PHASE 2 — Implement HLS VOD fully (Plan 03)

1. Extend `media-resolver/.../HlsPlaylistParser.kt` with bounded, standards-aware HLS master/media playlist parsing. Test `#EXT-X-STREAM-INF`, `#EXT-X-MEDIA` alternate audio, codecs, BANDWIDTH/AVERAGE-BANDWIDTH, RESOLUTION, URI quoting/commas, BYTERANGE, MAP/init segments, discontinuities and relative URLs/redirects.
2. Classify protected/encrypted playlists cleanly and reject unsupported encrypted media without attempting decryption/bypass. Do not silently treat HLS as a monolithic HTTP file. Distinguish VOD (`ENDLIST`) from live; defer live recording.
3. Add `HlsPlanResolver` returning deterministic options labeled with quality, codec and audio options. Avoid hardcoded quality strings when manifest metadata is absent.
4. Add `SegmentTransferEngine` backed by existing transfer/retry and Room `download_parts` checkpoints. Atomic per-segment completion, bounded concurrency/memory, segment index/order verification, safe redirection and host restrictions, cancellation/pause/resume, and recovery after interrupted segment/partial write.
5. Assemble/remux verified segments using supported Media3/Android mux facilities or a reviewed dependency with correct container support; do not assume simple concatenation works for all fMP4/TS variants. Fail safely for unsupported input. Finalize only after playable verification.
6. Extend deterministic HTTP fixtures for HLS 480p/720p/1080p, transient 503, missing segment, unsafe URL, altered manifest, alternate audio, and incomplete stream.
7. Add unit, integration and device E2E tests. Enforce RED→GREEN cycles and checkpoint identity consistency.

**Exit gate:** authorized, unencrypted HLS VOD fixture selects 720p, downloads/resumes reliably and yields a playable offline file. Audio and timing must be checked.

### PHASE 3 — DASH VOD, track pairing and mux/remux (Plan 03 continuation)

1. Add bounded `.mpd` parser/resolver: adaptation sets, representations, codecs, bandwidth, resolution, SegmentTemplate/Timeline, BaseURL, initialization/index segments and proper URL resolution. Explicitly classify ContentProtection as unsupported.
2. Build tested separate video/audio download plans with stable representation pairing, timebase/duration checks and segment checkpoints. No protected streams.
3. Add verified media processing state `PROCESSING`, mux/remux paths, safe temporary files and cleanup, duration/audio-video consistency and playable Media3 verification; fallback if a codec/container is unsupported.
4. Preserve original quality information in UI and provide audio-only output only where technically and legally supported.
5. Write deterministic fixture E2E: `1080p video + audio → interrupt → resume → mux → verified local playback`.

### PHASE 4 — Site adapter SDK and source refresh (Plan 04)

1. Define typed versioned compiled-in `SiteAdapter` contracts: `matches(url)`, `inspect(context)`, capability-limited result/format metadata; no direct filesystem/Room/queue access.
2. Centralize scoped credential use through native `CredentialBroker`. Never expose global cookies or tokens to adapters or logs; deny cross-origin credential forwarding by default.
3. Add adapter registry priority, timeouts, health, error isolation and fixture-backed examples on lawful/public-domain test sources. A failing adapter cannot disable generic detection.
4. Add refreshing of expiring media URLs with strong media-identity checks before resume. Reject replacement content even when name/URL appears similar.

### PHASE 5 — Privacy, library and user experience (Plan 05)

1. Complete normal and isolated private tabs. Private history must not enter normal history. Implement clear browsing data with explicit selectors; don't delete downloaded Library files silently.
2. Add reasonable privacy/site settings and blocking through supported GeckoView APIs; do not claim parity with Brave's Chromium internals.
3. Finish Downloads queue/progress/notifications, Library filters/search/favorites/playlists/continue watching, Media3 background audio/MediaSession, video PiP and error/empty states.
4. Maintain the approved original simple visual direction (light, blue accent, Home/Browser/Downloads/Library). Do not use proprietary Brave/SnapTube icons, logos or layouts as a copied asset set.

### PHASE 6 — Security/reliability/release gate (Plan 06)

1. Test hostile or oversized WebExtension payloads, malformed playlists, redirects to local network or unsafe schemes, filenames/path traversal, credential leakage, insufficient storage, corrupt partial file, duplicate MediaStore identity and Room migrations.
2. Verify supported behavior and document OS-specific limits Android 10–16; don't force unsupported UIDT/foreground scheduling paths.
3. Implement CI build and unsigned/debug APK artifacts first. Configure signing using secrets OUTSIDE Git and document a repeatable signed APK workflow. Do not generate and commit production keys.
4. Require zero S1 issues (security/data loss/crash loops), zero S2 issues (core detection/downloading broken), and passing full critical flow: `Browse → Detect → Choose quality → Download → Pause → Resume → Process kill/reopen → Network loss/recovery → mux → Verify → MediaStore → Library → Offline play`.
5. Summarize exact passed tests, test device/API versions, build commands, produced artifact paths and all remaining blockers. Don't mark release complete until actual device tests pass.

## 7. Testing, design and implementation rules

- Prefer small focused Kotlin files and interfaces consistent with current modules. Inspect implementations before changing signatures used by other tasks.
- Implement one independently testable unit per commit. Name tests by behavior; run a failing test first, implement minimal code, then run focused AND overall relevant suite.
- Use deterministic local media fixtures, not scraping real websites or fetching copyright-protected media as tests.
- Keep memory streaming/bounded and separate networking from parsing, UI and storage; no large in-memory video arrays.
- Where a task requires a platform that is missing, do not fabricate results. Add meaningful JVM/unit tests and Android test source, document the blocked runtime gate, and continue with other non-dependent tasks.
- Resolve compile/API errors against actual dependency versions and official vendor docs, not hypothetical APIs.
- Prefer smallest corrective change; preserve all user-created data, state and previous code. No force-push, destructive repo cleanup, broad rewriting or remote publishing without authorization.
- Commit meaningful milestones; push only when repository visibility/security gate permits. Use a PR and review before merging to `main`.

## 8. Required progress format and deliverables

Maintain `docs/implementation/STATUS.md` after each meaningful milestone, including:

```text
DATE / BRANCH / HEAD:
MILESTONE:
FILES MODIFIED:
TESTS: exact command, passed/failed counts
ANDROID BUILD: PASS / FAIL / BLOCKED (reason)
DEVICE E2E: PASS / FAIL / NOT RUN (reason)
SECURITY/PRIVACY NOTES:
NEXT UNBLOCKED TASK:
```

At the end of the task/run provide:

1. Link to changed branch/PR if pushed, else exact local branch and commit(s).
2. Working features versus source-only features versus not-yet-implemented features.
3. Test results with literal commands and outputs; real APK file path only if generated.
4. Any dependency/toolchain or Android runtime blocker and exact reproduction steps.
5. Highest-priority next task. Continue directly with the next safe task when the environment and task scope allow; don't ask for approval after every small commit.

**Start NOW:** inspect repo/source and its privacy, establish the actual Android Gradle build gate, and fix the first concrete blocker. Do not switch to polishing UI, creating marketing assets, or planning new features until the build and critical direct-download path are addressed. If there is no source in the Codex workspace, request/import the supplied bundle instead of inventing a replacement.
