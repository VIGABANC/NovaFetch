# NovaFetch — Browser + Generic Media Detection (source checkpoint)

**Date:** 2026-10-18
**Branch:** `feature/generic-media-detection`
**Status:** Source implementation and portable tests added. **NOT yet an Android-built APK.**
**Previous gate:** Plan 01 Android SDK / Gradle build + on-device tests still pending.

## New source in this checkpoint

1. **Generic DOM candidate scanner** (`browser-core/.../scan-media.js`): `<video>`, `<audio>`, `<source>`, direct media anchors, and social metadata hints; HTTP(S) URL normalization; unsafe scheme rejection; deduplication; bounded batches.
2. **Dynamic media detection** (`content.js`): debounced `MutationObserver`, only scans newly inserted/modified subtrees rather than polling the whole page, and sends bounded native messages.
3. **Built-in GeckoView WebExtension**: bundled manifest and content scripts with an explicit native message channel.
4. **Native sender validation**: verifies extension ID, actual GeckoSession, actual sender-frame URL, schema version, message type, fields, and batch size.
5. **Media resolver**: URL policy, confidence scoring, duplicate catalog, and direct-file download-plan generation. Hints from page metadata alone are not offered as direct-download URLs.
6. **Browser tab registry and GeckoView host**: normal tab creation, selection, closure, navigation callbacks, bridge-to-catalog integration.
7. **Compose browser UI**: address bar, back/forward/reload, tab controls, media counter, media selection sheet, and direct-file handoff to existing DownloadCoordinator.
8. **Deterministic browser fixture**: `http://10.0.2.2:8765/media-page.html` (on emulator), with an HTML5 MP4 and dynamic video insertion.
9. **Android instrumentation test source**: independent Gecko sessions, browser-to-direct-download enqueue; debug-only HTTP test allowance.
10. **Android verification scripts updated** to run browser/app instrumentation and start the test media server during runtime tests.

## Fresh portable verification

- `node --test tools/media-detector-tests/*.test.cjs` — **11 tests passed**.
- `./tools/run-generic-core-smoke.sh` — **MediaCatalogTest + UrlPolicyTest passed**, plus the 11 Node tests.
- `./tools/run-extension-bridge-smoke.sh` — **passed**.
- `./tools/run-browser-tabs-smoke.sh` — **passed**.
- `(cd tools/test-media-server && python3 -m unittest test_server.py -q)` — **11 tests passed**.
- `python3 -m unittest tools.test_browser_android_source_contract -q` — **4 source-contract tests passed**.
- `./tools/run-pipeline-e2e-smoke.sh` — **existing playable direct-download E2E passed**.

## Still unverified / not implemented

- Android Gradle sync/build: **not run** (no Android SDK or Gradle available in this execution environment).
- Actual GeckoView extension delivery/native messaging on a device: **not run**.
- Browser Compose rendering/session switching on an emulator: **not run**.
- Android download execution/scheduling/instrumentation: **not run**.
- HLS/DASH format parsing, split-track downloading, merge, protected-media classification: **later Plan 03**; manifest results shown as not-yet-downloadable.
- Site-specific adapters: **later Plan 04**.
- Private browsing, advanced privacy blocking, PiP/background player: **later Plan 05**.
- No ready-to-install APK is claimed.

## Follow-up in an Android SDK-equipped machine

1. Open the project directory in Android Studio.
2. Install Android SDK 36, Build Tools 36.0.0, JDK 17+, and Gradle 9.6 as required by AGP 9.4 (or generate a checked-in wrapper).
3. Run `./tools/verify-android-build.sh` and fix any real SDK/Gradle diagnostics.
4. Start an emulator or device with `adb`.
5. Run `./tools/verify-android-runtime.sh` (the script starts the test HTTP media server).
6. Confirm GeckoView can load `http://10.0.2.2:8765/media-page.html`, detects the MP4, enqueues a direct download, and plays it from Library.
7. Only after the pending device gates pass mark Plan 01 and Plan 02 complete.

**Security boundary:** no DRM bypass, access-control circumvention, or remote executable adapters; only user-permitted media should be downloaded. Browser-provided URLs are untrusted hints that go through validation.
