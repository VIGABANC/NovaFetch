# NovaFetch Android — Replica Architect Design Spec

**Date:** 2026-10-17  
**Status:** Approved design, awaiting final user review before implementation planning  
**Project type:** Android-only, media-first, local-only browser/downloader  
**Distribution:** Direct APK from the official project website  
**Primary priority:** Reliable media detection and downloading  
**Secondary priority:** Private browsing and offline playback  
**Clean-room rule:** Rebuild product capabilities and flows with original code, original branding, and original UI. Do not copy proprietary source code, logos, trademarks, copy, private APIs, catalogs, or protected assets from SnapTube, Brave, or any other product.

---

## 1. Product Goal

NovaFetch is an Android application that combines:

1. A privacy-conscious web browser.
2. Generic media detection.
3. Site-adapter-assisted media discovery.
4. Direct-file, HLS VOD, and DASH VOD downloading.
5. Quality and format selection.
6. Pause/resume and recovery across process death and device reboot.
7. Video/audio track merging and safe media processing.
8. A local offline media library.
9. Video and audio playback with background audio and Picture-in-Picture.

The product is **media-first**. Browsing exists primarily to reach media, discover downloadable formats, and hand them to a reliable download pipeline.

The first version is **local-only**:
- No accounts.
- No cloud database.
- No cloud sync.
- No backend required for core functionality.
- No mandatory telemetry.

---

## 2. Success Criteria

The MVP is successful only when the following end-to-end flow works reliably:

1. Launch NovaFetch.
2. Open a web page.
3. Detect downloadable media.
4. Show correct media options and available qualities.
5. Start a download.
6. Pause and resume it.
7. Recover after application process death.
8. Recover after a normal device reboot.
9. Download separate audio/video streams when required.
10. Merge/remux them without unnecessary re-encoding.
11. Verify the final media.
12. Save it to Android shared media storage.
13. Show it in NovaFetch Library.
14. Play it offline.

A visually polished UI does not compensate for failure in this chain.

---

## 3. Non-Goals and Hard Boundaries

NovaFetch will not implement:

- DRM bypass.
- Widevine circumvention.
- Paywall bypass.
- Access-control circumvention.
- Credential theft or cookie export.
- Decryption-key extraction.
- Scraping content the user is not allowed to access.
- Remote arbitrary-code adapters in the MVP.
- Cloud accounts or sync in the MVP.
- Desktop or iOS clients in the MVP.
- VPN, Tor, crypto, rewards, social networking, or a browser extension marketplace.
- Live-stream recording in the MVP.
- A built-in video editor in the MVP.

Protected media must be classified as unsupported rather than circumvented.

---

## 4. Platform and Technology Decisions

### 4.1 Core stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Browser engine:** GeckoView
- **Browser/content bridge:** Built-in WebExtensions
- **Database:** Room
- **Playback:** Media3 / ExoPlayer
- **Background playback:** MediaSession + MediaSessionService
- **Persistent deferred/recovery work:** WorkManager
- **Long user-initiated transfers on Android 14+:** User-Initiated Data Transfer jobs where appropriate
- **Shared media output:** MediaStore
- **User-selected directories:** Storage Access Framework when needed
- **Temporary/internal state:** App-specific storage
- **Architecture:** Multi-module Android project

### 4.2 Why GeckoView

NovaFetch is a full browser/downloader product, not a simple embedded webpage. GeckoView provides the browser-engine foundation, session model, content-blocking primitives, private mode, and built-in WebExtension integration needed for the design.

Android WebView is not selected as the primary browser engine.

### 4.3 Why local-only first

Local-only:
- Reduces privacy risk.
- Removes account/backend complexity.
- Makes offline usage a first-class capability.
- Prevents cloud dependencies from delaying the core downloader.
- Keeps the MVP focused on detection and transfer reliability.

---

## 5. Product Navigation

Primary bottom navigation:

1. **Home**
2. **Browser**
3. **Downloads**
4. **Library**

Settings are accessible from the app menu.

### 5.1 Home

Home contains:
- Search / URL field.
- Browser shortcut.
- Downloads shortcut.
- Video shortcut.
- Audio shortcut.
- Recent downloads.
- Continue Watching.
- Recently visited pages for normal browsing only.

### 5.2 Browser

Browser provides:
- Address/search bar.
- Back.
- Forward.
- Reload.
- Tab switcher.
- Privacy indicator.
- Media detection/download indicator.
- Normal and private tabs.

### 5.3 Downloads

Sections:
- All.
- Active.
- Completed.
- Failed.

Each active item may show:
- Progress.
- Downloaded bytes.
- Total bytes when known.
- Speed.
- ETA when estimable.
- Pause.
- Resume.
- Cancel.
- Current phase.

### 5.4 Library

Sections:
- Videos.
- Audio.
- Images.
- Playlists.
- Favorites.

The Library is a media collection, not merely a download history list.

---

## 6. Module Architecture

Recommended Gradle modules:

```text
:app

:browser-core
:browser-ui
:privacy

:media-detection
:media-resolver

:adapter-api
:adapters

:download-core
:download-android

:media-processing
:player
:library

:data
:network
:security
:common
```

### 6.1 Dependency rule

Dependencies flow inward toward interfaces and shared domain models.

Forbidden direct dependencies include:

```text
SiteAdapter -> Download UI
SiteAdapter -> Room DAO
Player -> BrowserSession
MediaDetector -> Activity
WebExtension -> Filesystem
WebExtension -> Room
```

Subsystems communicate through interfaces.

Core interfaces include:

```text
MediaDetector
MediaResolver
SiteAdapter
DownloadPlanner
DownloadCoordinator
TransferEngine
MediaProcessor
MediaRepository
PlaybackRepository
CredentialBroker
```

---

## 7. Browser Architecture

### 7.1 Sessions

Each browser tab owns an independent Gecko session.

```text
Browser
├── Normal Tab -> GeckoSession
├── Normal Tab -> GeckoSession
└── Private Tab -> isolated GeckoSession
```

Normal browsing and private browsing use separate storage/session contexts.

### 7.2 Tab persistence

Normal tab metadata may be restored after app restart.

Private tabs:
- Are not restored by default.
- Do not write browser history.
- Do not write recent-site data.
- Must clear private session state when the private session closes.

### 7.3 Permissions

All webpage permissions are mediated by a dedicated `PermissionController`.

Examples:
- Location.
- Notifications.
- Camera/microphone if later enabled.
- Autoplay behavior.
- Persistent site storage.

Default policy is least privilege.

---

## 8. Privacy Layer

NovaFetch includes a basic privacy shield using browser-engine primitives and app policy.

MVP controls:
- Tracking protection.
- Ad/resource filtering where supported.
- Third-party cookie controls.
- HTTPS preference.
- Popup blocking.
- Autoplay control.
- Private tabs.
- Per-site exceptions.
- Clear browsing data.

Per-site settings are stored separately from browser history.

Downloaded files remain real files even when the download was initiated from a private tab. NovaFetch must tell the user before saving permanent media from private browsing.

---

## 9. Media Detection Architecture

Media detection is multi-layered:

```text
Web page
   ↓
DOM Detector
Resource Detector
Manifest Detector
Site Adapter Registry
   ↓
Candidate Normalizer
   ↓
Security / Protection Classification
   ↓
Deduplicator
   ↓
Format Resolver
   ↓
Media Catalog
```

The generic engine always remains available, even if every site adapter fails.

### 9.1 Discovery methods

Supported discovery methods:

- Direct URL.
- DOM media elements.
- DOM source elements.
- Relevant page metadata.
- Dynamically added media.
- HLS manifests.
- DASH manifests.
- Resource/network observations when supported by the current browser integration.
- Site adapters.

### 9.2 Dynamic content

The DOM detector uses incremental observation of DOM changes rather than repeatedly rescanning the entire page.

### 9.3 Candidate model

Normalized media candidates contain:

- Source page.
- Frame context when relevant.
- Resource URL.
- Media kind.
- MIME type.
- Discovery method.
- Title.
- Thumbnail.
- Width.
- Height.
- Bitrate.
- Estimated size when available.
- Resolution requirement.
- Confidence score.

### 9.4 Candidate validation

Before a candidate becomes downloadable:
- Validate URL scheme.
- Validate request destination rules.
- Validate media type when possible.
- Reject malformed and unsafe schemes.
- Reject obvious trackers and non-media assets.
- Classify protected/DRM media.

### 9.5 Blob handling

`blob:` URLs are not treated as ordinary direct-download URLs.

NovaFetch must try to locate the underlying media resources, manifests, or segment sources instead.

### 9.6 HLS

MVP supports HLS VOD:
- Master playlists.
- Variant streams.
- Audio tracks.
- Downloaded segments.
- Retry and per-segment checkpointing.
- Remux/finalization.

Live-stream recording is out of scope.

### 9.7 DASH

MVP supports DASH VOD:
- MPD parsing.
- Video representations.
- Audio representations.
- Separate audio/video selection.
- Segment downloading.
- Final muxing.

### 9.8 Deduplication

Candidates from DOM, resource observation, and adapters may refer to the same media.

The resolver produces a single user-facing option using a fingerprint derived from:
- Canonical resource identity.
- Manifest identity.
- Resolution.
- Codec.
- Bitrate.
- Duration.
- Content length when known.

### 9.9 Media grouping

The catalog separates:
- Primary media.
- Secondary media.
- Background/auxiliary media.
- Audio.
- Images.

Active playback interaction may increase a candidate's priority.

---

## 10. Format Resolution

The resolver converts raw streams into understandable download options.

Example user-facing option:

```text
1080p
MP4
H.264
AAC
~520 MB
```

Internally it may be:

```text
video representation
+
audio representation
+
mux plan
```

When multiple codecs are available at the same resolution, NovaFetch may rank options by:
- Hardware decode support.
- Container compatibility.
- Estimated size.
- Need for transcoding.
- Audio compatibility.

One option can be labeled `Recommended`.

---

## 11. DRM and Protected Media Classification

Before creating a download plan, NovaFetch runs a protection inspection step.

If the media uses unsupported DRM or equivalent protected delivery, the result is:

```text
Protected media
Download unavailable
```

No module exposes APIs for:
- Extracting keys.
- Decrypting Widevine.
- Bypassing access controls.
- Circumventing protected playback.

---

## 12. Site Adapter SDK

Adapters improve site-specific media discovery but do not replace the generic engine.

### 12.1 Adapter contract

Each adapter exposes:

```text
id
version
matches(url)
inspect(context)
```

The result includes:
- Media items.
- Formats.
- Warnings.
- Optional refresh descriptor.
- Confidence score.

### 12.2 Adapter capabilities

Possible capabilities include:
- DOM metadata.
- Resource metadata.
- Manifest read.
- Limited page state.
- Scoped session request.

Adapters do not receive:
- Raw cookie database access.
- Filesystem access.
- Room DAO access.
- Direct download queue control.

### 12.3 Adapter packaging

MVP adapters are:
- Compiled into the APK.
- Versioned.
- Reviewed.
- Tested.

Remote adapter code execution is not allowed in the MVP.

### 12.4 Adapter health

Persist:
- Adapter ID/version.
- Success count.
- Failure count.
- Last success.
- Last failure.
- Consecutive failures.
- Detection time.
- Health state.

Health states:

```text
HEALTHY
DEGRADED
BROKEN
DISABLED
```

A broken adapter must not break generic detection.

### 12.5 Adapter examples

The MVP ships with a small number of thoroughly tested adapter examples rather than a large fragile catalog.

Target:
- Strong generic engine.
- Adapter SDK.
- Approximately 3–5 robust example adapters.

---

## 13. Credential Broker

Adapters and resolvers do not receive raw cookies.

Credential flow:

```text
Adapter / Resolver
    ↓
AuthorizedRequestSpec
    ↓
CredentialBroker
    ↓
HTTP client
```

Rules:
- Credentials are origin-scoped.
- Cross-origin credential forwarding is denied by default.
- Sensitive headers are not logged.
- Signed URLs are not stored indefinitely.
- Temporary session material should remain short-lived.
- Diagnostics must redact authentication information.

---

## 14. Download Plan Model

The resolver produces a typed download plan.

Plan types:

```text
DirectPlan
ChunkedPlan
HlsPlan
DashPlan
MuxPlan
AudioExtractionPlan
```

Example `MuxPlan`:

```text
Video: 1080p H.264
Audio: AAC 256 kbps
Output: MP4
Operation: mux only
Transcode: no
```

---

## 15. Download Engine

Core components:

```text
DownloadCoordinator
├── QueueManager
├── NetworkClient
├── ChunkPlanner
├── RetryPolicy
├── ResumeManager
├── ManifestDownloader
├── IntegrityVerifier
├── MediaProcessor
└── Finalizer
```

### 15.1 Queue

Priority levels:

```text
HIGH
NORMAL
LOW
```

User-started downloads are high priority.

Default maximum simultaneous downloads: **2**.

User-selectable range for MVP:
- 1
- 2
- 3
- 4

Download concurrency and chunk concurrency are distinct.

### 15.2 Pause

Pause must:
- Stop active requests.
- Persist offsets/checkpoints.
- Flush buffers.
- Preserve temporary data.
- Set job state to `PAUSED`.

### 15.3 Resume

For direct HTTP files:
- Use byte-range requests when supported.
- Validate content identity before appending.
- Use ETag, Last-Modified, Content-Length, and source fingerprints when available.

For HLS/DASH:
- Persist segment completion state.
- Redownload only missing/failed segments unless the manifest/source identity changed.

### 15.4 Adaptive chunking

Parallel chunking is optional and only enabled when useful.

Consider:
- File size.
- Range support.
- Connection quality.
- Server behavior.
- Device/network conditions.

Small files should use a single connection.

### 15.5 Retry policy

Retry temporary failures:
- Network disconnect.
- Timeout.
- 502/503/504.

Use bounded exponential backoff.

Do not endlessly retry:
- 404.
- Protected media.
- Invalid format.
- Full storage.
- Invalid permissions.

### 15.6 Expiring URLs

If a temporary media URL expires:
1. Invoke the refresh architecture.
2. Re-resolve the same media/format.
3. Compare identity.
4. Resume only when safe.
5. Restart or ask the user if identity cannot be proven.

Never append arbitrary new content to an existing partial file.

---

## 16. Persistent Download State

Room is the source of truth for download state.

### 16.1 Main tables

```text
download_jobs
download_parts
media_candidates
media_items
playlists
playlist_items
browser_history
bookmarks
browser_tabs
site_settings
adapter_state
processing_jobs
```

### 16.2 `download_jobs`

Stores:
- Job ID.
- Source URL.
- Page URL.
- Title.
- File name.
- MIME type.
- Media type.
- Quality.
- Resolution.
- Bitrate.
- Codec.
- Container.
- Total/downloaded bytes.
- Status.
- Output URI.
- Temporary path.
- Resume support.
- ETag.
- Last-Modified.
- Retry state.
- Failure state.
- Adapter metadata.
- Timestamps.

### 16.3 `download_parts`

Stores:
- Part/segment index.
- Byte range when relevant.
- Segment URL when relevant.
- Expected size.
- Downloaded size.
- Local path.
- Status.
- Retry state.
- Optional checksum.

---

## 17. Download State Machine

Primary states:

```text
CREATED
RESOLVING
QUEUED
CONNECTING
DOWNLOADING
VERIFYING
PROCESSING
FINALIZING
COMPLETED
```

Side states:

```text
PAUSED
WAITING_NETWORK
WAITING_STORAGE
RETRY_PENDING
FAILED
CANCELLED
```

State transitions are explicit and validated.

User pause and involuntary interruption are distinct.

---

## 18. Recovery

### 18.1 Activity/UI exit

Closing the UI must not erase download state.

### 18.2 Process death

Recovery:
- Read Room.
- Validate temporary files.
- Restore unfinished eligible work.
- Resume from checkpoints.

### 18.3 Device reboot

After a normal reboot:
- Restore scheduled work.
- Scan unfinished jobs.
- Validate partial state.
- Resume eligible interrupted downloads.
- Keep user-paused jobs paused.

### 18.4 Force stop

NovaFetch does not promise background recovery while Android holds the app in a force-stopped state.

On the next explicit app launch:
- Run recovery.
- Reconcile unfinished jobs.
- Resume eligible interrupted jobs according to settings.

---

## 19. Storage

### 19.1 Temporary data

Use app-private storage for:
- Temp files.
- Chunks.
- Manifests.
- Thumbnails.
- Processing intermediates.

### 19.2 Final files

Use MediaStore for final media that should be visible outside NovaFetch.

Recommended default locations:

```text
Movies/NovaFetch/
Music/NovaFetch/
Pictures/NovaFetch/
Downloads/NovaFetch/
```

Do not request `MANAGE_EXTERNAL_STORAGE`.

### 19.3 Atomic finalization

Workflow:

```text
partial download
→ verify
→ process
→ temporary final output
→ validate
→ MediaStore insert
→ mark COMPLETED
```

A partially processed file must not appear as a successful final media item.

### 19.4 Storage validation

Before large downloads:
- Estimate required bytes.
- Include temporary merge/remux overhead.
- Block or warn when free space is insufficient.

---

## 20. Media Processing

Preferred processing order:

```text
Direct copy
→ mux
→ remux/transmux
→ transcode only when necessary
```

Re-encoding is the last option due to:
- Battery cost.
- Thermal cost.
- Time.
- Potential quality loss.

Audio-only downloads should use a direct audio stream when available instead of downloading a large video first.

---

## 21. Integrity Verification

Before `COMPLETED`, validate as applicable:
- Expected byte count.
- Container readability.
- Presence of expected video track.
- Presence of expected audio track.
- Duration sanity.
- Successful mux/remux output.

A missing required track is a failed processing result, not a completed download.

---

## 22. Offline Library

Each media item may store:
- Library ID.
- Download job ID.
- Title.
- Media type.
- Content URI.
- Thumbnail URI.
- Duration.
- Resolution.
- Codec.
- Container.
- Bitrate.
- File size.
- Source domain.
- Source page URL.
- Creation timestamp.
- Last played timestamp.
- Playback position.
- Favorite flag.

The Library works offline.

---

## 23. Player Architecture

Use:

```text
Compose Player UI
    ↓
MediaController
    ↓
MediaSessionService
    ↓
MediaSession
    ↓
ExoPlayer
```

### 23.1 Video MVP

- Play/pause.
- Seek.
- Double-tap seek.
- Fullscreen.
- Rotation/orientation support.
- Playback speed.
- Audio track selection.
- Subtitle selection when available.
- Picture-in-Picture.

### 23.2 Audio MVP

- Background playback.
- Notification controls.
- Lock-screen controls.
- Bluetooth/media-button controls.
- Queue.
- Playback speed.

### 23.3 Continue Watching

Store position when:
- More than 30 seconds were played.
- The user is not within the final 5% of the media.

---

## 24. History and Data Clearing

Browser history and download/library history are separate.

Clearing browsing data must not silently delete downloaded media.

Clear-data UI may offer:

```text
Browsing history
Cookies
Site storage
Cache
Downloads history
Downloaded files
```

`Downloaded files` is not selected by default.

---

## 25. Security Model

### 25.1 Trust boundary

Untrusted:
- Web pages.
- Iframes.
- DOM data.
- WebExtension messages.
- Media URLs.
- Manifests.
- Adapter output.

Validation boundary:
- Message schema validation.
- Sender validation.
- URL policy.
- Protection classification.
- Credential policy.
- Download planning.

Trusted core:
- DownloadCoordinator.
- Room.
- MediaProcessor.
- Finalizer.
- MediaStore integration.
- Local player.

### 25.2 WebExtension message validation

Validate:
- Expected extension ID.
- Native app/channel ID.
- Gecko session.
- Sender URL.
- Frame/top-level context.
- Message schema.
- Payload size.

### 25.3 URL policy

Reject or specially handle unsafe schemes.

Ordinary download requests are limited to supported HTTP(S) media paths.

Do not allow untrusted page data to directly produce arbitrary:
- `file://`
- internal `content://`
- privileged localhost access
- internal app-file access

### 25.4 Redirect policy

Revalidate every redirect.

Do not forward sensitive origin-bound credentials across origin changes by default.

### 25.5 Filename policy

Sanitize:
- Path traversal.
- Reserved characters.
- Control characters.
- Dangerous separators.
- Excessive length.

File extension must be based on validated media type/container, not URL text alone.

### 25.6 Safe logging

Production logging must redact:
- Cookie headers.
- Authorization headers.
- Session tokens.
- Sensitive signed query parameters.
- Private browsing URLs when possible.

---

## 26. Direct APK Distribution

Release rules:
- Sign production APKs using a protected release key.
- Keep the signing key outside the repository.
- Publish release checksums.
- Publish releases only on the official distribution channel.

MVP update behavior:
- Check versioned update metadata.
- Notify the user.
- Open the official download page.
- Let Android handle package installation.

No complex silent/self-installer system is required for the MVP.

---

## 27. Testing Strategy

### 27.1 Unit tests

Cover:
- Download state transitions.
- Filename sanitization.
- Retry policy.
- URL policy.
- Format ranking.
- Deduplication.
- Media fingerprints.
- Storage calculations.

### 27.2 Parser tests

Cover:
- HLS fixtures.
- DASH fixtures.
- Adapter fixtures.
- Malformed manifests.
- Oversized/invalid input.

### 27.3 Database tests

Cover:
- Room DAO behavior.
- Schema migrations.
- Recovery state.
- Partial downloads.
- App upgrades.

### 27.4 Network tests

Cover:
- Range support.
- No-range fallback.
- Redirects.
- Expired URLs.
- Retryable failures.
- Cross-origin redirect protection.
- Connection loss.

### 27.5 Integration tests

Cover:
- Detector -> resolver -> plan.
- Direct HTTP downloads.
- HLS VOD.
- DASH VOD.
- Separate video/audio muxing.
- Expired-source refresh.

### 27.6 Lifecycle tests

Cover:
- UI close.
- Process kill.
- Device reboot recovery.
- User pause persistence.
- Force-stop recovery on next explicit app launch.

### 27.7 UI tests

Cover critical Compose flows:
- Browser to media sheet.
- Quality selection.
- Start/pause/resume.
- Downloads screen.
- Completed item to Library.
- Library to Player.
- Private browsing indicators.

---

## 28. Deterministic Test Media Server

The test suite should use a small controlled server capable of serving:

```text
/direct.mp4
/range.mp4
/no-range.mp4
/slow.mp4
/redirect.mp4
/expired-url
/master.m3u8
/manifest.mpd
/disconnect-mid-download
/error-503
```

This avoids relying exclusively on changing third-party sites during core downloader testing.

---

## 29. Failure Injection

Required test scenarios include:

- Disconnect at 42%.
- Kill process at 70%.
- Disk becomes full.
- Server changes ETag.
- Manifest expires.
- One HLS segment fails.
- Audio succeeds while video fails.
- Repeated HTTP 503.
- Redirect changes origin.
- Adapter timeout.
- Malformed WebExtension message.
- Path traversal filename.
- Fake MIME type.
- Infinite redirect attempt.

Expected behavior is a safe, recoverable failure rather than a crash or silent corruption.

---

## 30. Bug Severity and Release Gate

Severity:

```text
S1 = security issue, data corruption, crash loop
S2 = core detection/download failure
S3 = important issue with workaround
S4 = minor/visual
```

Release requirement:

```text
S1 = 0
S2 = 0
```

---

## 31. MVP Scope Lock

### 31.1 Required for MVP

- GeckoView browser.
- Normal tabs.
- Private tabs.
- Generic media detection.
- Direct HTTP media files.
- HLS VOD.
- DASH VOD.
- Quality picker.
- Separate video/audio handling.
- Mux/remux.
- Download queue.
- Pause/resume.
- Network interruption recovery.
- Process-death recovery.
- Device reboot recovery.
- Expiring-URL refresh architecture.
- Offline Library.
- Video player.
- Audio player.
- Background audio.
- Picture-in-Picture.
- Basic privacy protection.
- Protected-media safe rejection.
- Small set of robust example site adapters.

### 31.2 Explicitly deferred

- Cloud sync.
- Accounts.
- Desktop.
- iOS.
- VPN.
- Tor.
- Casting.
- Live-stream recording.
- Remote adapters.
- Extension marketplace.
- AI media tools.
- Social features.
- Crypto/rewards.
- Large site-adapter catalog.

---

## 32. Release Candidate Matrix

Test at minimum across Android versions:

```text
Android 10
Android 11
Android 12
Android 13
Android 14
Android 15
Android 16
```

Prioritize Android 14+ validation for modern long-running transfer scheduling.

Test on devices/emulators with:
- Low memory.
- Mid-range memory.
- High memory.
- Slow network.
- Network switching.
- Limited storage.

---

## 33. Critical End-to-End Release Test

This test blocks release:

```text
Launch app
→ open supported test page
→ detect media
→ show 1080p option
→ start download
→ pause
→ resume
→ kill process
→ restore
→ interrupt network
→ restore network
→ complete video + audio
→ mux
→ verify final media
→ save through MediaStore
→ show in Library
→ play offline
```

If this chain fails in a core step, the build is not releasable.

---

## 34. Implementation Order

Implementation should proceed vertically, proving one end-to-end slice before expanding breadth.

Recommended sequence:

1. Multi-module skeleton and domain contracts.
2. Room schema and download state machine.
3. Deterministic test media server.
4. Direct HTTP download engine.
5. Pause/resume and process recovery.
6. Basic MediaStore finalization.
7. Basic Library.
8. Basic Media3 playback.
9. GeckoView browser shell.
10. Built-in WebExtension bridge.
11. Generic DOM/direct media detection.
12. Media candidate normalization and deduplication.
13. HLS VOD.
14. DASH VOD.
15. Separate audio/video mux.
16. Expiring-source refresh architecture.
17. Adapter SDK.
18. 3–5 robust example adapters.
19. Privacy controls.
20. Private tabs.
21. Background audio and PiP.
22. Security hardening.
23. Failure-injection suite.
24. Release-candidate matrix.
25. Direct-APK release pipeline.

---

## 35. Architecture Summary

```text
                     NovaFetch Android
                            │
          ┌─────────────────┴─────────────────┐
          │                                   │
       Browser                            Library
          │                                   │
      GeckoView                            Media3
          │                                   │
 Built-in WebExtension                        │
          │                                   │
  Media Detection Core                       │
          ↓                                   │
 Generic + Site Adapters                     │
          ↓                                   │
      Media Resolver                         │
          ↓                                   │
    Security / Policy                        │
          ↓                                   │
     Download Planner                        │
          ↓                                   │
   Download Coordinator                      │
          ↓                                   │
 Direct / HLS / DASH Transfer                │
          ↓                                   │
    Media Processing                         │
          ↓                                   │
 Integrity Verification                      │
          ↓                                   │
       MediaStore ────────────────────────────┘

Room + app-private filesystem = persistent source of truth for in-progress work
```

---

## 36. Final Decisions

The approved architecture is:

```text
Android only
Media-first
Local-only
Direct APK distribution
Kotlin
Jetpack Compose
GeckoView
Built-in WebExtensions
Room
Media3 / ExoPlayer
WorkManager
UIDT on Android 14+ when appropriate
MediaStore
Generic detection first
Site adapters second
Reliable downloading before visual polish
No DRM bypass
Light/minimal UI
```

This specification contains no unresolved requirements and is intended to be the single source of truth for the implementation plan.
