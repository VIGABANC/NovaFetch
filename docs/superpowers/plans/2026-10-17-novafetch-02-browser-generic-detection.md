# NovaFetch 02 — Browser & Generic Detection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a GeckoView browser that generically detects direct media from pages and hands validated, deduplicated options to the working downloader.

**Architecture:** GeckoView owns browsing; a built-in WebExtension reports DOM media hints through a validated bridge. Native detectors normalize candidates, apply URL/media policy, deduplicate them, and expose a Media Catalog. Site adapters are not part of this plan.

**Tech Stack:** Kotlin, Compose, GeckoView, built-in WebExtensions, Room, existing download modules, JUnit, Android instrumented tests.

**Spec:** `docs/superpowers/specs/2026-10-17-novafetch-android-design.md`

## Global Constraints

- Generic detection must work without any site adapter.
- WebExtension messages are untrusted input.
- Only validated HTTP(S) media candidates may reach the downloader.
- `blob:` URLs are not treated as directly downloadable.
- Private browsing implementation is deferred to Plan 05.
- No raw cookies are exposed to the WebExtension.

## Review Focus

- Malformed/oversized extension message → reject safely without crashing.
- Candidate in an iframe claims a privileged/local URL → reject it.
- Same media appears in DOM and metadata → show one option.
- Dynamically inserted `<video>` → detect without full-page polling.
- Page contains many tiny assets/trackers → avoid presenting them as primary media.

### Task 1: GeckoView Browser Shell

**Files:**
- Create: `browser-core/build.gradle.kts`
- Create: `browser-core/src/main/kotlin/app/novafetch/browser/BrowserSessionManager.kt`
- Create: `browser-ui/src/main/kotlin/app/novafetch/browser/BrowserScreen.kt`
- Create: `browser-ui/src/main/kotlin/app/novafetch/browser/AddressBar.kt`
- Modify: `settings.gradle.kts`
- Modify: `app/src/main/kotlin/app/novafetch/MainActivity.kt`
- Test: `browser-core/src/androidTest/kotlin/app/novafetch/browser/BrowserSessionManagerTest.kt`

**Interfaces:**
- Produces: `fun BrowserSessionManager.open(url: String): BrowserTabId`
- Produces: `fun BrowserSessionManager.observeTabs(): Flow<List<BrowserTab>>`
- Produces: `suspend fun BrowserSessionManager.close(tabId: BrowserTabId)`

- [ ] Write failing browser-session tests for open, navigate, close, and two independent normal tabs.
- [ ] Run `./gradlew :browser-core:connectedAndroidTest`; expect FAIL.
- [ ] Implement Gecko runtime/session manager and minimal Compose browser screen.
- [ ] Run `./gradlew :browser-core:connectedAndroidTest`; expect PASS.
- [ ] Commit: `git add . && git commit -m "feat: add geckoview browser shell"`

### Task 2: Built-in WebExtension and Message Validation

**Files:**
- Create: `browser-core/src/main/assets/extensions/media-detector/manifest.json`
- Create: `browser-core/src/main/assets/extensions/media-detector/content.js`
- Create: `browser-core/src/main/kotlin/app/novafetch/browser/ExtensionMessageValidator.kt`
- Create: `browser-core/src/main/kotlin/app/novafetch/browser/MediaBridge.kt`
- Test: `browser-core/src/test/kotlin/app/novafetch/browser/ExtensionMessageValidatorTest.kt`

**Interfaces:**
- Produces: `fun ExtensionMessageValidator.validate(sender: ExtensionSender, raw: String): Result<DomMediaMessage>`
- Produces: `fun MediaBridge.messages(tabId: BrowserTabId): Flow<DomMediaMessage>`

- [ ] Write failing tests for valid message, unknown extension ID, oversized payload, invalid scheme, and missing frame metadata.
- [ ] Run `./gradlew :browser-core:test`; expect FAIL.
- [ ] Implement strict JSON schema parsing and sender/session checks.
- [ ] Run `./gradlew :browser-core:test`; expect PASS.
- [ ] Commit: `git add . && git commit -m "feat: validate built-in media extension messages"`

### Task 3: DOM Detector and Dynamic Mutation Detection

**Files:**
- Create: `media-detection/src/main/kotlin/app/novafetch/detection/MediaDetector.kt`
- Create: `media-detection/src/main/kotlin/app/novafetch/detection/MediaCandidate.kt`
- Create: `media-detection/src/main/kotlin/app/novafetch/detection/DomMediaDetector.kt`
- Modify: `browser-core/src/main/assets/extensions/media-detector/content.js`
- Test: `media-detection/src/test/kotlin/app/novafetch/detection/DomMediaDetectorTest.kt`

**Interfaces:**
- Produces: `interface MediaDetector { fun candidates(tabId: BrowserTabId): Flow<MediaCandidate> }`
- `MediaCandidate` contains page URL, frame URL, resource URL, kind, MIME, discovery method, metadata, and confidence.

- [ ] Write failing tests for `<video>`, `<audio>`, `<source>`, direct anchor, metadata hint, and dynamically inserted `<video>`.
- [ ] Run `./gradlew :media-detection:test`; expect FAIL.
- [ ] Implement candidate normalization from extension messages; MutationObserver scans changed subtrees only.
- [ ] Run tests; expect PASS.
- [ ] Commit: `git add . && git commit -m "feat: detect dom media candidates"`

### Task 4: URL Policy, Candidate Validation, Confidence, and Deduplication

**Files:**
- Create: `security/src/main/kotlin/app/novafetch/security/UrlPolicy.kt`
- Create: `media-resolver/src/main/kotlin/app/novafetch/resolver/CandidateValidator.kt`
- Create: `media-resolver/src/main/kotlin/app/novafetch/resolver/MediaFingerprint.kt`
- Create: `media-resolver/src/main/kotlin/app/novafetch/resolver/MediaCatalog.kt`
- Test: `media-resolver/src/test/kotlin/app/novafetch/resolver/MediaCatalogTest.kt`
- Test: `security/src/test/kotlin/app/novafetch/security/UrlPolicyTest.kt`

**Interfaces:**
- Produces: `fun UrlPolicy.allowRemoteMedia(url: String): Boolean`
- Produces: `suspend fun CandidateValidator.validate(candidate: MediaCandidate): ValidationResult`
- Produces: `fun MediaFingerprint.of(candidate: ValidatedCandidate): String`
- Produces: `fun MediaCatalog.upsert(candidate: ValidatedCandidate): CatalogSnapshot`

- [ ] Write failing tests for http/https allow, javascript/file/content reject, blob special classification, duplicate collapse, tiny-asset de-prioritization.
- [ ] Run resolver/security tests; expect FAIL.
- [ ] Implement policies and deduplication.
- [ ] Run tests; expect PASS.
- [ ] Commit: `git add . && git commit -m "feat: validate and deduplicate media candidates"`

### Task 5: Media Sheet and Browser-to-Downloader E2E

**Files:**
- Create: `browser-ui/src/main/kotlin/app/novafetch/browser/MediaDetectionButton.kt`
- Create: `browser-ui/src/main/kotlin/app/novafetch/browser/MediaSheet.kt`
- Create: `media-resolver/src/main/kotlin/app/novafetch/resolver/DirectFormatResolver.kt`
- Test: `app/src/androidTest/kotlin/app/novafetch/BrowserDirectMediaE2eTest.kt`
- Create fixture: `tools/test-media-server/fixtures/direct-page.html`

**Interfaces:**
- Produces: `fun DirectFormatResolver.toPlan(candidate: ValidatedCandidate): DirectPlan`
- Media Sheet emits selected `DownloadPlan` to existing `DownloadCoordinator`.

- [ ] Write failing E2E test: open fixture page → detection badge becomes `1` → select candidate → enqueue download → final Library item exists.
- [ ] Run `./gradlew :app:connectedAndroidTest`; expect FAIL.
- [ ] Implement detection badge, Media Sheet, and direct-plan handoff.
- [ ] Run full browser/detection tests; expect PASS.
- [ ] Commit: `git add . && git commit -m "feat: connect browser media detection to downloads"`

## Plan Exit Criterion

Open a fixture web page in GeckoView, detect a direct media asset without site-specific logic, download it, and play it offline.
