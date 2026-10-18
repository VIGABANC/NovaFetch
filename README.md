# NovaFetch — Android media-first browser (work in progress)

NovaFetch is an original Android media browser and local download/library project. The initial source supports core HTTP transfers (including range-based resume), persistent download state, a prototype GeckoView browser with generic HTML media detection, and an early HLS playlist parser. The app is **not yet a verified installable release**.

## Implementation status (2026-10-18)

| Area | Status |
| --- | --- |
| Download core, resume, state machine | Portable tests available |
| Local media finalization/library | Source and portable tests; Android runtime verification outstanding |
| GeckoView and media detection | Prototype source and portable tests; Android runtime verification outstanding |
| HLS playlist metadata parsing | Implemented and portable smoke test passed |
| HLS segment transfer, DASH, mux/remux | Not implemented |
| Signed Android APK | Not built/released |

See `NATIVE_IMPLEMENTATION_STATUS.md`, `PLAN02_SOURCE_STATUS.md`, and `PLAN03_PROGRESS.md` for details.

## Development prerequisites

- Android Studio with compatible Android SDK/build tools, Java and Gradle
- Kotlin/Java for local portable test scripts
- Python 3 for deterministic local media-server tests

The project currently includes `gradle/wrapper/gradle-wrapper.properties` but **does not include Gradle wrapper executables**. Install/use a compatible local Gradle distribution before invoking the Android build scripts. The Android build and instrumentation tests have not been verified in this environment.

## Local checks

```bash
bash tools/run-hls-parser-smoke.sh
bash tools/run-generic-core-smoke.sh
python3 -m unittest discover -s tools/test-media-server -p 'test*.py' -v
```

For Android SDK environments, review and execute:

```bash
bash tools/verify-android-build.sh
bash tools/verify-android-runtime.sh
```

Those scripts need a valid Android SDK and device/emulator, and may need Gradle setup first.

## Structure

- `app` — Android application graph and Compose entry points
- `browser-core`, `browser-ui` — GeckoView browsing and detection UI
- `media-detection`, `media-resolver` — generic detection, candidate resolution, HLS inspection
- `download-core`, `download-android` — transfer state, engine, scheduling/recovery
- `data`, `library` — Room storage, local media catalog and finalization
- `player` — local playback integration
- `security` — input and URL safety
- `tools` — smoke tests, deterministic fixture server and Android verification scripts

## Scope and safety

Only download content you have the right to save and sources that allow this use. No DRM/Widevine circumvention, paywall bypass, proprietary app cloning, or credential extraction is part of this project. Site adapters, where added, will be compiled and scoped rather than remotely executable.

## Codex workflow

Clone https://github.com/VIGABANC/NovaFetch.git and open that repository in Codex and implement one tested task at a time from `docs/superpowers/plans/`. Use a feature branch and a pull request for changes. Keep the Android runtime gate open until an SDK-backed build and device tests have actually passed.

## License

No open-source license has been selected. **All rights reserved** until the repository owner explicitly chooses a license. Publishing a public repository does not automatically grant permission to reuse its contents.
