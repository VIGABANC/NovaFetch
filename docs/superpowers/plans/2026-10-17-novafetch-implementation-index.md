# NovaFetch Android Implementation Program

> **For agentic workers:** Implement the numbered plans in order. Each plan is independently reviewable and ends in working, testable software. Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` for each plan.

**Goal:** Build the approved NovaFetch Android MVP as a sequence of vertical, testable increments rather than one monolithic implementation.

**Architecture:** Kotlin/Compose Android app using GeckoView for browsing, Room for persistent state, Media3 for playback/processing, WorkManager plus Android 14+ UIDT where appropriate for resilient transfers, MediaStore for final media, and a modular detector/resolver/adapter pipeline. Generic detection remains functional even when site adapters fail.

**Tech Stack:** Kotlin, Gradle, Jetpack Compose, GeckoView, Room, OkHttp, Media3/ExoPlayer, WorkManager, MediaStore, Android instrumented tests, JUnit, MockWebServer, Python 3 standard library test media server.

**Spec:** `docs/superpowers/specs/2026-10-17-novafetch-android-design.md`

## Program Order

1. `2026-10-17-novafetch-01-foundation-direct-download.md`
   - Creates the project, persistence model, deterministic test server, direct HTTP downloader, pause/resume, recovery, MediaStore finalization, minimal Library, and minimal offline playback.
   - Exit criterion: direct MP4 flow works end to end.

2. `2026-10-17-novafetch-02-browser-generic-detection.md`
   - Adds GeckoView browser shell, tabs, built-in WebExtension bridge, generic DOM/direct media detection, candidate validation/deduplication, and the media sheet.
   - Exit criterion: opening a fixture page detects a direct media asset and hands it to the downloader.

3. `2026-10-17-novafetch-03-hls-dash-processing.md`
   - Adds HLS VOD, DASH VOD, segment checkpoints, separate audio/video planning, mux/remux, integrity verification, and audio-only paths.
   - Exit criterion: fixture HLS and DASH pages download and produce playable final media.

4. `2026-10-17-novafetch-04-adapter-sdk-refresh.md`
   - Adds the site-adapter SDK, registry, health tracking, scoped credential broker, source refresh for expiring URLs, and a small set of safe adapter examples.
   - Exit criterion: an expiring fixture source refreshes and resumes without corrupting the partial file.

5. `2026-10-17-novafetch-05-privacy-library-player.md`
   - Adds private tabs, site privacy settings, clear-data behavior, richer Library, background audio, MediaSession, PiP, continue-watching, and local playlists/favorites.
   - Exit criterion: private browsing leaves no normal history and downloaded media plays offline/background as designed.

6. `2026-10-17-novafetch-06-security-release.md`
   - Adds security hardening, hostile-input tests, failure injection, release-gate E2E, Android-version matrix, APK signing/release plumbing, checksum generation, and update metadata.
   - Exit criterion: S1=0, S2=0 and the critical E2E release test passes.

## Global Execution Rules

- Read the spec and the current plan before each task.
- Use TDD: failing test → minimal implementation → passing test → commit.
- Do not implement DRM bypass, Widevine circumvention, paywall bypass, key extraction, or access-control circumvention.
- Keep all core behavior local-only; no account/backend dependency is allowed for MVP functionality.
- `minSdk = 29` (Android 10), `compileSdk = 36`, `targetSdk = 36`; the release matrix must include Android 10 through Android 16.
- Do not request `MANAGE_EXTERNAL_STORAGE`.
- Keep final media in MediaStore and temporary chunks/intermediates in app-private storage.
- Generic media detection must continue to work when adapters fail.
- Remote executable adapter code is forbidden in the MVP.
- Do not log cookies, Authorization headers, session tokens, or full signed media URLs.
- No release while any S1 or S2 issue remains.

## Milestone Gate

Do not start a later plan until:
1. All tests in the current plan pass.
2. Its exit criterion is demonstrated.
3. A reviewer accepts the change.
4. The branch is clean and committed.
